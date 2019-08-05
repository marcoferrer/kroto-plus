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
import com.github.marcoferrer.krotoplus.coroutines.utils.COROUTINE_TEST_TIMEOUT
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ClientCallServerStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    public val timeout = CoroutinesTimeout.seconds(COROUTINE_TEST_TIMEOUT)

    private val methodDescriptor = GreeterGrpc.getSayHelloServerStreamingMethod()
    private val service = spyk(object : GreeterGrpc.GreeterImplBase() {})
    private val expectedRequest = HelloRequest.newBuilder().setName("success").build()

    private val cancellingInterceptor = object : ClientInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {
            val call = next.newCall(method, callOptions)
            callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)[Job]?.cancel()
            return call
        }
    }

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

    private fun setupServerHandlerNoop(){
        every { service.sayHelloServerStreaming(expectedRequest, any()) } just Runs
    }

    @BeforeTest
    fun setupService() {
        grpcServerRule.serviceRegistry.addService(service)
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
        }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after successful call" }
        verify(exactly = 0) { rpcSpy.call.cancel(any(),any()) }
    }

    @Test
    fun `Call fails on server error`() {
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

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

                onError(Status.INVALID_ARGUMENT.asRuntimeException())
            }
        }

        val responseChannel = stub
            .clientCallServerStreaming(expectedRequest, methodDescriptor)

        runBlocking {
            repeat(2) {
                assertEquals("Request#$it:${expectedRequest.name}", responseChannel.receive().message)
            }

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
        assertFailsWithStatus(Status.CANCELLED, "CANCELLED: Job was cancelled") {
            runBlocking {
                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        responseChannel = stub
                            .withCoroutineContext(externalJob)
                            .clientCallServerStreaming(expectedRequest, methodDescriptor)

                        responseChannel.receive()
                    }
                    launch {
                        externalJob.cancel()
                    }
                }

            }
        }

        verify { rpcSpy.call.cancel("Job was cancelled", any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }


    @Test
    fun `Call is canceled when interceptor cancels scope normally`() {

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub.withInterceptors(cancellingInterceptor)

        setupServerHandlerNoop()

        lateinit var responseChannel: ReceiveChannel<HelloReply>
        assertFailsWithStatus(Status.CANCELLED, "CANCELLED: Job was cancelled") {
            runBlocking {
                launch {
                    responseChannel = stub.clientCallServerStreaming(expectedRequest, methodDescriptor)

                    responseChannel.receive()
                }
            }
        }

        verify { rpcSpy.call.cancel("Job was cancelled", any<CancellationException>()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

    @Test
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
                cancel()
            }
        }

        verify { rpcSpy.call.cancel(any(), any()) }
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
                    launch {
                        error("cancel")
                    }
                }
            }
        }

        verify { rpcSpy.call.cancel("Parent job is Cancelling", any()) }
        assert(responseChannel.isClosedForReceive) { "Response channel is closed after cancellation" }
    }

}