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
import io.grpc.*
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ClientCallClientStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloClientStreamingMethod()
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
        every { service.sayHelloClientStreaming(any()) } answers {
            val responseObserver = firstArg<StreamObserver<HelloReply>>()
            object : StreamObserver<HelloRequest>{
                var reqQty = 0
                var responseString = ""
                override fun onNext(value: HelloRequest) {
                    responseString += "Req:#${value.name}/Resp:#${reqQty++}|"
                    if(reqQty == 2){
                        responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException())
                    }
                }
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {
                    responseObserver.onNext(HelloReply.newBuilder()
                        .setMessage(responseString)
                        .build())
                    responseObserver.onCompleted()
                }
            }
        }
    }

    private fun setupServerHandlerSuccess(){
        every { service.sayHelloClientStreaming(any()) } answers {
            val responseObserver = firstArg<StreamObserver<HelloReply>>()
            object : StreamObserver<HelloRequest>{
                var reqQty = 0
                var responseString = ""
                override fun onNext(value: HelloRequest) {
                    responseString += "Req:#${value.name}/Resp:#${reqQty++}|"
                }
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {
                    responseObserver.onNext(HelloReply.newBuilder()
                        .setMessage(responseString)
                        .build())
                    responseObserver.onCompleted()
                }
            }
        }
    }

    private fun setupServerHandlerNoop(){
        every { service.sayHelloClientStreaming(any()) } answers {
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

        lateinit var requestChannel: SendChannel<HelloRequest>
        runBlocking {
            val (sendChannel, response) = stub
                .withCoroutineContext()
                .clientCallClientStreaming(methodDescriptor)
            requestChannel = sendChannel
            launch {
                repeat(3){
                    requestChannel.send(
                        HelloRequest.newBuilder()
                            .setName(it.toString())
                            .build())
                }
                requestChannel.close()
            }
            launch{
                assertEquals("Req:#0/Resp:#0|Req:#1/Resp:#1|Req:#2/Resp:#2|", response.await().message)
            }
        }

        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }

    }

    @Test
    fun `Call fails on server error`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerError()

        val (requestChannel, response) = stub
            .clientCallClientStreaming(methodDescriptor)

        var requestsSent = 0
        runBlocking {
            launch {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    repeat(2) {
                        requestsSent++
                        requestChannel.send(
                            HelloRequest.newBuilder()
                                .setName(it.toString())
                                .build()
                        )
                    }
                    assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                        requestChannel.send(
                            HelloRequest.newBuilder()
                                .setName("request")
                                .build()
                        )
                    }
                }
                assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                    response.await().message
                }
            }
        }

        assertEquals(2,requestsSent)
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
    }

    @Test
    fun `Call is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        val externalJob = Job()
        val (requestChannel, response) = stub
            .withCoroutineContext(externalJob)
            .clientCallClientStreaming(methodDescriptor)

        runBlocking {
            launch(Dispatchers.Default) {
                val job = launch {
                    launch(start = CoroutineStart.UNDISPATCHED){
                        assertFailsWithStatus(Status.CANCELLED) {
                            response.await().message
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

        // First invocation comes from the requestChannel being closed and calling `onError`
        // Second invocation comes from the scope cancellation handler
        verify(exactly = 2) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        lateinit var requestChannel: SendChannel<HelloRequest>
        assertFails<CancellationException> {
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val callChannel = stub
                        .withCoroutineContext()
                        .clientCallClientStreaming(methodDescriptor)

                    requestChannel = callChannel.requestChannel

                    val job = launch {
                        callChannel.response.await().message
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

    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled exceptionally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        lateinit var requestChannel: SendChannel<HelloRequest>
        assertFailsWith(IllegalStateException::class, "cancel") {
            runBlocking {
                val callChannel = stub
                    .withCoroutineContext()
                    .clientCallClientStreaming(methodDescriptor)

                requestChannel = callChannel.requestChannel

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
                        callChannel.response.await().message
                    }
                }
            }
        }

        verify { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }

    }

}