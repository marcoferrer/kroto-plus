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


import com.github.marcoferrer.krotoplus.coroutines.CALL_OPTION_COROUTINE_CONTEXT
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
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
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Phaser
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail


class ClientCallServerStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    var nonDirectGrpcServerRule = GrpcServerRule()

    // @[Rule JvmField]
    // public val timeout = CoroutinesTimeout.seconds(COROUTINE_TEST_TIMEOUT)

    private val methodDescriptor = GreeterGrpc.getSayHelloServerStreamingMethod()
    private val service = spyk(object : GreeterGrpc.GreeterImplBase() {})
    private val expectedRequest = HelloRequest.newBuilder().setName("success").build()

    private fun newCancellingInterceptor(useNormalCancellation: Boolean) = object : ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            val job = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)[Job]!!
            if(useNormalCancellation)
                job.cancel() else
                job.cancel(CancellationException("interceptor-cancel"))
            return next.newCall(method,callOptions)
        }
    }

    private val excessiveInboundMessageInterceptor = object : ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> =
            object:SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method,callOptions)) {
                override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
                    super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener){
                        override fun onMessage(message: RespT) {
                            repeat(3){ super.onMessage(message) }
                        }
                    }, headers)
                }
            }
    }

    inner class RpcSpy(val channel: Channel = grpcServerRule.channel) : ClientInterceptor {

        val call: ClientCall<HelloRequest,HelloReply>
            get() = runBlocking { _call.await() }

        private val _call = CompletableDeferred<ClientCall<HelloRequest,HelloReply>>()

        val initialized = CompletableDeferred<Unit>()

        val stub: GreeterGrpc.GreeterStub = GreeterGrpc
            .newStub(channel).withInterceptors(this)

        @Suppress("UNCHECKED_CAST")
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            val spy = spyk(next.newCall(method,callOptions))
            _call.complete(spy as ClientCall<HelloRequest, HelloReply>)
            initialized.complete(Unit)
            return spy
        }
    }

    private fun setupServerHandlerNoop(){
        every { service.sayHelloServerStreaming(expectedRequest, any()) } just Runs
    }

    @BeforeTest
    fun setupService() {
        grpcServerRule.serviceRegistry.addService(service)
        nonDirectGrpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Call succeeds on server response`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        every { service.sayHelloServerStreaming(expectedRequest, any()) } answers {
            val actualRequest = firstArg<HelloRequest>()
            with(secondArg<StreamObserver<HelloReply>>()) {
                repeat(3) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${actualRequest.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)
        runBlocking {
            repeat(3) {
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
            }
            delay(100)
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after successful call" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(),any()) }
    }

////    @Test
//    fun flowwowowo(){
//        val f = channelFlow<String>{
//            launch {
//                repeat(10){
//                    send("A: $it")
//                }
//            }
//            launch {
//                repeat(10){
//                    send("B: $it")
//                }
//            }
//        }
//
//            .onEach {
//                println("hellow: $it")
//            }
//            .buffer(kotlinx.coroutines.channels.Channel.UNLIMITED)
//
//        runBlocking(Dispatchers.Default) {
//            f.collect {
//                println("receive: $it")
//                delay(1000)
//            }
//        }
//
//    }

    @Test
    fun `Call is cancelled when response channel is prematurely canceled`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        every { service.sayHelloServerStreaming(expectedRequest, any()) } answers {
            val actualRequest = firstArg<HelloRequest>()
            with(secondArg<StreamObserver<HelloReply>>()) {
                repeat(10) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${actualRequest.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)
        val results = mutableListOf<String>()
        runBlocking {
            repeat(3) {
                results.add(responseChannel.receive().message)
            }
            responseChannel.cancel()

            rpcSpy.initialized.await()
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after successful call" }
        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
    }

    @Test
    fun `Call fails on server error`() {
        val rpcSpy = RpcSpy(nonDirectGrpcServerRule.channel)
        val stub = rpcSpy.stub

        val phaser = Phaser(2)
        every { service.sayHelloServerStreaming(expectedRequest, any()) } answers {
            val actualRequest = firstArg<HelloRequest>()
            with(secondArg<StreamObserver<HelloReply>>()) {
                repeat(2) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${actualRequest.name}")
                            .build()
                    )
                }
                phaser.arriveAndAwaitAdvance()
                onError(Status.INVALID_ARGUMENT.asRuntimeException())
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        runBlocking {
            repeat(2) {
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
            }
            phaser.arrive()

            assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                responseChannel.receive()
            }
        }

        assert(responseChannel.isClosedForReceive) { "Response channel is closed after server error" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(),any()) }
    }

    @Test
    fun `Call is canceled when scope is canceled normally`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerNoop()

        val externalJob = Job()
        lateinit var responseChannel: ReceiveChannel<HelloReply>
        lateinit var parentJob: Job
        runBlocking {
            launch(Dispatchers.Default) {
                parentJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    responseChannel = stub
                        .withCoroutineContext(externalJob)
                        .clientCallServerStreaming(expectedRequest, methodDescriptor)
                    responseChannel.receive()
                    fail("Should not reach here")
                }
                launch {
                    externalJob.cancel()
                }
            }
        }

        assert(parentJob.isCancelled){ "External job cancellation should propagate from receive channel" }
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
        runBlocking {
            launch {
                responseChannel = stub.clientCallServerStreaming(expectedRequest, methodDescriptor)

                responseChannel.receive()
            }
        }

        assertFailsWith(CancellationException::class){
            runBlocking { responseChannel.receive() }
        }


        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
    fun `Call is canceled when interceptor cancels scope exceptionally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub.withInterceptors(newCancellingInterceptor(useNormalCancellation = false))

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        runBlocking {
            launch {
                responseChannel = stub.clientCallServerStreaming(expectedRequest, methodDescriptor)

                responseChannel.receive()
            }
        }

        assertFailsWith(CancellationException::class){
            runBlocking { responseChannel.receive() }
        }


        verify(exactly = 1) { rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test //THIS TEST IS CORRECT
    fun `Call withCoroutineContext is canceled when scope is canceled normally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        runBlocking {
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

        verify(exactly = 1){ rpcSpy.call.cancel(MESSAGE_CLIENT_CANCELLED_CALL, any<CancellationException>()) }
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

        every { service.sayHelloServerStreaming(expectedRequest, any()) } answers {
            val actualRequest = firstArg<HelloRequest>()
            with(secondArg<StreamObserver<HelloReply>>()) {
                repeat(20) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${actualRequest.name}")
                            .build()
                    )
                }
                onCompleted()
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        val result = runBlocking(Dispatchers.Default) {
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
        verify(exactly = 0) { rpcSpy.call.cancel(any(),any()) }
    }

    @Test
    fun `Excessive messages are buffered without requesting new ones`() {

        val rpcSpy = RpcSpy(ClientInterceptors.intercept(nonDirectGrpcServerRule.channel, excessiveInboundMessageInterceptor))
        val stub = rpcSpy.stub


        every { service.sayHelloServerStreaming(expectedRequest, any()) } answers {
            val actualRequest = firstArg<HelloRequest>()
            with(secondArg<StreamObserver<HelloReply>>()) {
                repeat(20) {
                    onNext(
                        HelloReply.newBuilder()
                            .setMessage("Request#$it:${actualRequest.name}")
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
        verify(exactly = 0) { rpcSpy.call.cancel(any(),any()) }
    }

}