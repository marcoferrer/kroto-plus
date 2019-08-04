/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.marcoferrer.krotoplus.coroutines.client


import com.github.marcoferrer.krotoplus.coroutines.utils.assertFails
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.toList
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ClientCallBidiStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloStreamingMethod()
    private val service = spyk(object : GreeterGrpc.GreeterImplBase() {})

    inner class RpcSpy{
        val stub: GreeterGrpc.GreeterStub
        lateinit var call: ClientCall<HelloRequest,HelloReply>

        init {
            val channelSpy = spyk(grpcServerRule.channel)
            stub = GreeterGrpc.newStub(channelSpy)

            every { channelSpy.newCall(methodDescriptor, any()) } answers {
                spyk(grpcServerRule.channel.newCall(methodDescriptor, secondArg<CallOptions>())).also {
                    this@RpcSpy.call = it
                }
            }
        }
    }

    private fun setupServerHandlerError(){
        every { service.sayHelloStreaming(any()) } answers {
            val responseObserver = firstArg<StreamObserver<HelloReply>>()
            object : StreamObserver<HelloRequest>{
                var reqQty = 0
                override fun onNext(value: HelloRequest) {
                    if(reqQty >= 3){
                        responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException())
                    }else{
                        responseObserver.onNext(HelloReply.newBuilder()
                            .setMessage("Req:#${value.name}/Resp:#${reqQty++}")
                            .build())
                    }
                }
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {
                    responseObserver.onCompleted()
                }
            }
        }
    }

    private fun setupServerHandlerSuccess(){
        every { service.sayHelloStreaming(any()) } answers {
            val responseObserver = firstArg<StreamObserver<HelloReply>>()
            object : StreamObserver<HelloRequest>{
                var reqQty = 0
                override fun onNext(value: HelloRequest) {
                    responseObserver.onNext(HelloReply.newBuilder()
                        .setMessage("Req:#${value.name}/Resp:#${reqQty++}")
                        .build())
                }
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {
                    responseObserver.onCompleted()
                }
            }
        }
    }

    private fun setupServerHandlerNoop(){
        every { service.sayHelloStreaming(any()) } answers {
            object : StreamObserver<HelloRequest>{
                override fun onNext(value: HelloRequest) {}
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {}
            }
        }
    }

    @BeforeTest
    fun setupService() {
        grpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Call succeeds on server response`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerSuccess()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        val result = runBlocking(Dispatchers.Default) {
            launch {
                repeat(3){
                    requestChannel.send(
                        HelloRequest.newBuilder()
                            .setName(it.toString())
                            .build())
                }
                requestChannel.close()
            }

            responseChannel.map { it.message }.toList()
        }

        assertEquals(3,result.size)
        result.forEachIndexed { index, message ->
            assertEquals("Req:#$index/Resp:#$index",message)
        }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call fails on server error`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerError()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        runBlocking {
            launch {
                with(requestChannel){
                    repeat(4) {
                        send(HelloRequest.newBuilder()
                            .setName(it.toString())
                            .build())
                    }
                    assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                        send(HelloRequest.newBuilder()
                            .setName("fails")
                            .build()
                        )
                    }
                }
            }
            launch {
                repeat(2) {
                    assertEquals("Req:#$it/Resp:#$it", responseChannel.receive().message)
                }
                assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                    responseChannel.receive().message
                }
            }
        }

        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        val externalJob = Job()
        val (requestChannel, responseChannel) = stub
            .withCoroutineContext(externalJob)
            .clientCallBidiStreaming(methodDescriptor)


        runBlocking {
            launch(Dispatchers.Default) {
                val job = launch {
                    launch(start = CoroutineStart.UNDISPATCHED){
                        assertFailsWithStatus(Status.CANCELLED) {
                            responseChannel.receive()
                        }
                    }
                    assertFailsWithStatus(Status.CANCELLED) {
                        repeat(3) {
                            requestChannel.send(
                                HelloRequest.newBuilder()
                                    .setName(it.toString())
                                    .build()
                            )
                        }
                    }
                }
                launch {
                    job.start()
                    externalJob.cancel()
                }
            }
        }

        verify(exactly = 1) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        lateinit var requestChannel: SendChannel<HelloRequest>
        lateinit var responseChannel: ReceiveChannel<HelloReply>
        assertFails<CancellationException> {
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val callChannel = stub
                        .withCoroutineContext()
                        .clientCallBidiStreaming(methodDescriptor)

                    requestChannel = callChannel.requestChannel
                    responseChannel = callChannel.responseChannel

                    val job = launch {
                        callChannel.responseChannel.receive().message
                    }
                    assertFailsWithStatus(Status.CANCELLED) {
                        repeat(3) {
                            requestChannel.send(
                                HelloRequest.newBuilder()
                                    .setName(it.toString())
                                    .build()
                            )
                            delay(5)
                        }
                    }
                    assertFailsWithStatus(Status.CANCELLED) {
                        job.join()
                    }
                }
                cancel()
            }
        }

        verify { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled exceptionally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        lateinit var requestChannel: SendChannel<HelloRequest>
        lateinit var responseChannel: ReceiveChannel<HelloReply>
        assertFailsWith(IllegalStateException::class, "cancel") {
            runBlocking {
                val callChannel = stub
                    .withCoroutineContext()
                    .clientCallBidiStreaming(methodDescriptor)

                requestChannel = callChannel.requestChannel
                responseChannel = callChannel.responseChannel

                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        assertFailsWithStatus(Status.CANCELLED) {
                            repeat(3) {
                                requestChannel.send(
                                    HelloRequest.newBuilder()
                                        .setName(it.toString())
                                        .build()
                                )
                                delay(10L)
                            }
                        }
                    }
                    launch {
                        error("cancel")
                    }
                    assertFailsWithStatus(Status.CANCELLED) {
                        callChannel.responseChannel.receive().message
                    }
                }
            }
        }

        verify { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @[Rule JvmField]
    var grpcServerRule2 = GrpcServerRule()
    
    @Test
    fun `High throughput call succeeds`() {
        grpcServerRule2.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Default
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                for (request in requestChannel) {
                    responseChannel.send(HelloReply.newBuilder().setMessage(request.name).build())
                }
            }
        })
        val stub = GreeterCoroutineGrpc.newStub(grpcServerRule2.channel)

        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        runBlocking(Dispatchers.Default) {
            val numMessages = 100000
            val req = HelloRequest.newBuilder()
                .setName("test").build()
            val job1 = launch {
                repeat(numMessages){
                    if (it % 1000 == 0)
                        println("1 sending $it")
                    requestChannel.send(req)
                }
            }

            val job2 = launch {
                repeat(numMessages) {
                    if (it % 1000 == 0)
                        println("2 receiving $it")
                    responseChannel.receive()
                }
            }

            println("waiting")
            job1.join()
            job2.join()
            requestChannel.close()
            Thread.sleep(100)
        }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }
}