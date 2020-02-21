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


import com.github.marcoferrer.krotoplus.coroutines.RpcCallTest
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFails
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithCancellation
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.matchStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.matchThrowable
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ClientCallBidiStreamingTests :
    RpcCallTest<HelloRequest, HelloReply>(GreeterGrpc.getSayHelloStreamingMethod()) {

    val expectedCancelMessage = "Cancelled by client with StreamObserver.onError()"

    private fun setupServerHandler(
        block: (StreamObserver<HelloReply>) -> StreamObserver<HelloRequest>
    ){
        registerService(object : GreeterGrpc.GreeterImplBase(){
            override fun sayHelloStreaming(responseObserver: StreamObserver<HelloReply>)
                    : StreamObserver<HelloRequest> = block(responseObserver)
        })
    }

    private fun setupServerHandlerError(){
        setupServerHandler { responseObserver ->
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
        setupServerHandler { responseObserver ->
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
        setupServerHandler { responseObserver ->
            object : StreamObserver<HelloRequest>{
                override fun onNext(value: HelloRequest) {}
                override fun onError(t: Throwable?) {}
                override fun onCompleted() {}
            }
        }
    }

    @Test
    fun `Call succeeds on server response`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerSuccess()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        val result = runTest {
            launch {
                repeat(3){
                    requestChannel.send(
                        HelloRequest.newBuilder()
                            .setName(it.toString())
                            .build())
                }
                requestChannel.close()
            }

            responseChannel.consumeAsFlow().map { it.message }.toList()
        }

        callState.blockUntilClosed()

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

        runTest {
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
                repeat(3) {
                    assertEquals("Req:#$it/Resp:#$it", responseChannel.receive().message)
                }
                assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                    responseChannel.receive().message
                }
            }
        }

        callState.blockUntilClosed()

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


        runTest {
            launch {
                val job = launch(start = CoroutineStart.ATOMIC) {
                    launch(start = CoroutineStart.UNDISPATCHED){
                        assertFailsWithCancellation {
                            responseChannel.receive()
                        }
                    }
                    assertFailsWithCancellation {
                        repeat(3) {
                            requestChannel.send(
                                HelloRequest.newBuilder()
                                    .setName(it.toString())
                                    .build()
                            )
                            delay(10)
                        }
                    }
                }
                launch {
                    job.start()
                    externalJob.cancel()
                }
            }
        }

        callState.blockUntilCancellation()

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
            runTest {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val callChannel = stub
                        .withCoroutineContext()
                        .clientCallBidiStreaming(methodDescriptor)

                    requestChannel = callChannel.requestChannel
                    responseChannel = callChannel.responseChannel

                    val job = launch {
                        callChannel.responseChannel.receive().message
                    }
                    assertFailsWithCancellation {
                        repeat(3) {
                            requestChannel.send(
                                HelloRequest.newBuilder()
                                    .setName(it.toString())
                                    .build()
                            )
                            delay(5)
                        }
                    }
                    assertFailsWithCancellation {
                        job.join()
                    }
                }
                cancel()
            }
        }

        runTest {
            assertFailsWithCancellation {
                responseChannel.receive()
            }
        }

        callState.blockUntilCancellation()

        verify(exactly = 1) { rpcSpy.call.cancel(any(), any()) }
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
            runBlocking(Dispatchers.Default) {
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

        callState.blockUntilCancellation()

        verify(exactly = 1) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call is cancelled when request channel closed with error concurrently`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        val expectedException = IllegalStateException("test")

        setupServerHandlerSuccess()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        val result = mutableListOf<String>()
        runTest {
            launch {
                repeat(3) {
                    requestChannel.send(
                        HelloRequest.newBuilder()
                            .setName(it.toString())
                            .build()
                    )
                }
                requestChannel.close(expectedException)
            }

            assertFailsWithStatus(Status.CANCELLED, "CANCELLED: $expectedCancelMessage") {
                responseChannel.consumeAsFlow()
                    .map { it.message }
                    .collect { result.add(it) }
            }
        }

        callState.blockUntilCancellation()

        assert(result.isNotEmpty())
        result.forEachIndexed { index, message ->
            assertEquals("Req:#$index/Resp:#$index",message)
        }
        verify(exactly = 1) { rpcSpy.call.cancel(expectedCancelMessage, matchThrowable(expectedException)) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call is cancelled when request channel closed with error sequentially`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        val expectedException = IllegalStateException("test")

        setupServerHandlerSuccess()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        val result = mutableListOf<String>()
        runTest {
            requestChannel.send(
                HelloRequest.newBuilder()
                    .setName(0.toString())
                    .build()
            )
            result.add(responseChannel.receive().message)
            requestChannel.close(expectedException)

            assertFailsWithStatus(Status.CANCELLED, "CANCELLED: $expectedCancelMessage") {
                responseChannel.consumeAsFlow()
                    .collect { result.add(it.message) }
            }
        }

        callState.client.cancelled.assertBlocking{ "Client must be cancelled" }

        assertEquals(1, result.size)
        result.forEachIndexed { index, message ->
            assertEquals("Req:#$index/Resp:#$index",message)
        }
        verify(exactly = 1) { rpcSpy.call.cancel(expectedCancelMessage, matchThrowable(expectedException)) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

    @Test
    fun `Call is cancelled when response channel is prematurely canceled`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerSuccess()
        val (requestChannel, responseChannel) = stub
            .clientCallBidiStreaming(methodDescriptor)

        runTest {
            launch {
                assertFailsWithStatus(Status.CANCELLED) {
                    repeat(10) {
                        requestChannel.send(
                            HelloRequest.newBuilder()
                                .setName(it.toString())
                                .build()
                        )
                        delay(10)
                    }
                }
                requestChannel.close()
            }

            repeat(3){
                responseChannel.receive()
            }
            responseChannel.cancel()
        }

        callState.blockUntilCancellation()

        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL,matchStatus(Status.CANCELLED)) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
    }

}