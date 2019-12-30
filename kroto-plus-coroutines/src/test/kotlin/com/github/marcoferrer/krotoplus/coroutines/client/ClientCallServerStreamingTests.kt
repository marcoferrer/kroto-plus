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
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.invoke
import com.github.marcoferrer.krotoplus.coroutines.utils.newCancellingInterceptor
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.Phaser
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail


class ClientCallServerStreamingTests : RpcCallTest(GreeterGrpc.getSayHelloServerStreamingMethod()) {

    private val excessiveInboundMessageInterceptor = object : ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> =
            object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
                    super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener) {
                        override fun onMessage(message: RespT) {
                            repeat(3) { super.onMessage(message) }
                        }
                    }, headers)
                }
            }
    }

    inner class RpcSpy(val channel: Channel = grpcServerRule.channel) : ClientInterceptor {

        private val _call = CompletableDeferred<ClientCall<HelloRequest, HelloReply>>()
        val call: ClientCall<HelloRequest, HelloReply>
            get() = runBlocking { _call.await() }

        val stub: GreeterGrpc.GreeterStub = GreeterGrpc
            .newStub(channel).withInterceptors(this, callState)

        @Suppress("UNCHECKED_CAST")
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            val spy = spyk(next.newCall(method, callOptions))
            _call.complete(spy as ClientCall<HelloRequest, HelloReply>)
            return spy
        }
    }

    private fun setupServerHandlerNoop() = setupUpServerHandler { _, _ -> }

    private fun setupUpServerHandler(
        block: (request: HelloRequest, responseObserver: StreamObserver<HelloReply>) -> Unit
    ) {
        val serviceImpl = object : GreeterGrpc.GreeterImplBase() {
            override fun sayHelloServerStreaming(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) =
                block(request, responseObserver)
        }

        val service = ServerInterceptors.intercept(serviceImpl, callState)
        nonDirectGrpcServerRule.serviceRegistry.addService(service)
        grpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Call succeeds on server response`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupUpServerHandler { request, responseObserver ->
            with(responseObserver) {
                repeat(3) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${request.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        runTest {
            repeat(3) {
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
            }
            delay(300)
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after successful call" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }

    @Test
    fun `Call is cancelled when response channel is prematurely canceled`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupUpServerHandler { request, responseObserver ->
            with(responseObserver) {
                repeat(10) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${request.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)
        val results = mutableListOf<String>()
        runTest {
            repeat(3) {
                results.add(responseChannel.receive().message)
            }
            responseChannel.cancel()
        }

        callState {
            client.cancelled.assertBlocking{ "Client should be cancelled" }
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after successful call" }
        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
    }

    @Test
    fun `Call fails on server error`() {
        val rpcSpy = RpcSpy(nonDirectGrpcServerRule.channel)
        val stub = rpcSpy.stub

        val phaser = Phaser(2)
        setupUpServerHandler { request, responseObserver ->
            with(responseObserver) {
                repeat(2) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${request.name}")
                            .build()
                    )
                }
                phaser.arriveAndAwaitAdvance()
                onError(Status.INVALID_ARGUMENT.asRuntimeException())
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        runBlocking(Dispatchers.Default) {
            repeat(2) {
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
            }
            phaser.arrive()

            assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                responseChannel.receive()
            }
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after server error" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }

    @Test
    fun `Call is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerNoop()

        val externalJob = Job()
        lateinit var responseChannel: ReceiveChannel<HelloReply>
        lateinit var parentJob: Job

        runTest {
            launch(Dispatchers.Default) {
                parentJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    responseChannel = stub
                        .withCoroutineContext(externalJob)
                        .clientCallServerStreaming(expectedRequest, methodDescriptor)
                    responseChannel.receive()
                    fail("Should not reach here")
                }
                launch {
                    callState.client.started.assert { "Client should be started" }
                    externalJob.cancel()
                }
            }
        }

        callState {
            client {
                closed.assertBlocking { "Client should be closed" }
            }
            server {
                cancelled.assertBlocking { "Server should be cancelled" }
            }
        }
        assert(parentJob.isCancelled) { "External job cancellation should propagate from receive channel" }
        assertFailsWith(CancellationException::class) {
            runBlocking {
                responseChannel.receive()
            }
        }

        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }


    @Test
    fun `Call is canceled when interceptor cancels scope normally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub.withInterceptors(newCancellingInterceptor(useNormalCancellation = true))

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        runTest {
            launch {
                responseChannel = stub.clientCallServerStreaming(expectedRequest, methodDescriptor)

                responseChannel.receive()
            }
        }

        assertFailsWith(CancellationException::class) {
            runBlocking { responseChannel.receive() }
        }

        callState.client.cancelled.assertBlocking { "Client should be cancelled" }

        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
    fun `Call is canceled when interceptor cancels scope exceptionally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub.withInterceptors(newCancellingInterceptor(useNormalCancellation = false))

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        runTest {
            launch {
                responseChannel = stub.clientCallServerStreaming(expectedRequest, methodDescriptor)

                responseChannel.receive()
            }
        }

        assertFailsWith(CancellationException::class) {
            runBlocking { responseChannel.receive() }
        }

        callState.client.cancelled.assertBlocking { "Client should be cancelled" }
        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled normally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        runTest {
            launch(Dispatchers.Default) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    responseChannel = stub
                        .withCoroutineContext()
                        .clientCallServerStreaming(expectedRequest, methodDescriptor)

                    responseChannel.receive()
                }
                delay(100)
                cancel()
            }
        }

        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
    fun `Call withCoroutineContext is canceled when scope is canceled exceptionally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        assertFailsWith(IllegalStateException::class, "cancel") {
            runBlocking {
                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        responseChannel = stub
                            .withCoroutineContext()
                            .clientCallServerStreaming(expectedRequest, methodDescriptor)

                        responseChannel.receive()
                    }
                    delay(100)
                    launch {
                        error("cancel")
                    }
                }
            }
        }

        assertFailsWith(CancellationException::class) {
            runBlocking {
                responseChannel.receive()
            }
        }

        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
    fun `Call only requests messages after one is consumed`() {
        val rpcSpy = RpcSpy(nonDirectGrpcServerRule.channel)
        val stub = rpcSpy.stub

        setupUpServerHandler { request, responseObserver ->
            with(responseObserver) {
                repeat(20) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${request.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        val result = runTest {
            delay(100)
            repeat(3) {
                verify(exactly = it + 2) { rpcSpy.call.request(1) }
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
                delay(10)
            }

            // Consume remaining messages
            responseChannel.toList()
        }

        assertEquals(17, result.size)
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after server error" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }

    @Test
    fun `Excessive messages are buffered without requesting new ones`() {

        val rpcSpy = RpcSpy(
            ClientInterceptors.intercept(nonDirectGrpcServerRule.channel, excessiveInboundMessageInterceptor)
        )
        val stub = rpcSpy.stub


        setupUpServerHandler { request, responseObserver ->
            with(responseObserver) {
                repeat(20) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${request.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        val consumedMessages = mutableListOf<String>()
        val result = runBlocking(Dispatchers.Default) {
            delay(300)
            repeat(4) {
                verify(exactly = it + 2) { rpcSpy.call.request(1) }
                consumedMessages += responseChannel.receive().message
                delay(10)
            }

            // Consume remaining messages
            responseChannel.toList()
        }

        assertEquals("Request#0:${expectedRequest.name}", consumedMessages[0])
        assertEquals("Request#0:${expectedRequest.name}", consumedMessages[1])
        assertEquals("Request#0:${expectedRequest.name}", consumedMessages[2])
        assertEquals("Request#1:${expectedRequest.name}", consumedMessages[3])
        assertEquals(56, result.size)
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after server error" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }

}