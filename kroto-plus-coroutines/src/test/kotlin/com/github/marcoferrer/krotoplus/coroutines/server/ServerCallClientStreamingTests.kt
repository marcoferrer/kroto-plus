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

package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.utils.COROUTINE_TEST_TIMEOUT
import com.github.marcoferrer.krotoplus.coroutines.utils.CancellingClientInterceptor
import com.github.marcoferrer.krotoplus.coroutines.utils.ServerSpy
import com.github.marcoferrer.krotoplus.coroutines.utils.matchStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.serverRpcSpy
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ServerCallClientStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    public val timeout = CoroutinesTimeout.seconds(COROUTINE_TEST_TIMEOUT)

    private val methodDescriptor = GreeterGrpc.getSayHelloClientStreamingMethod()
    private val expectedResponse = HelloReply.newBuilder().setMessage("reply").build()
    private val responseObserver = spyk<StreamObserver<HelloReply>>(object: StreamObserver<HelloReply>{
        override fun onNext(value: HelloReply?) {}
        override fun onError(t: Throwable?) {}
        override fun onCompleted() {}
    })

    private fun newCall(): Pair<ClientCall<HelloRequest, HelloReply>, StreamObserver<HelloRequest>>{
        val call = grpcServerRule.channel
            .newCall(methodDescriptor, CallOptions.DEFAULT)

        return call to ClientCalls.asyncClientStreamingCall<HelloRequest, HelloReply>(call, responseObserver)
    }

    private fun StreamObserver<HelloRequest>.sendRequests(qty: Int){
        repeat(qty){
            onNext(HelloRequest.newBuilder().setName(it.toString()).build())
        }
        onCompleted()
    }

    @Test
    fun `Server responds successfully`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                return HelloReply.newBuilder()
                    .setMessage(requestChannel.toList().joinToString{ it.name })
                    .build()
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloClientStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onNext(match { it.message == "0, 1, 2" }) }
        verify(exactly = 1) { responseObserver.onCompleted() }
        verify(exactly = 0) { responseObserver.onError(any()) }
    }

    @Test
    fun `Server responds with error`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                requestChannel.receive()
                requestChannel.receive()
                throw Status.INVALID_ARGUMENT.asRuntimeException()
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloClientStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.INVALID_ARGUMENT)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
    }

    @Test
    fun `Server responds with cancellation when scope cancelled normally`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(
                requestChannel: ReceiveChannel<HelloRequest>
            ): HelloReply = coroutineScope {
                requestChannel.receive()
                requestChannel.receive()
                launch {
                    delay(5L)
                }
                cancel()
                expectedResponse
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel).sayHelloClientStreaming(responseObserver)
        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
    }

    @Test
    fun `Server responds with error when scope cancelled exceptionally`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(
                requestChannel: ReceiveChannel<HelloRequest>
            ): HelloReply = coroutineScope {
                requestChannel.receive()
                requestChannel.receive()
                launch {
                    error("unexpected cancellation")
                }
                expectedResponse
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel).sayHelloClientStreaming(responseObserver)
        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.UNKNOWN)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
    }

    @Test
    fun `Server request channel is closed if not consumed`(){
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(
                requestChannel: ReceiveChannel<HelloRequest>
            ): HelloReply {
                reqChannel = requestChannel
                delay(5L)
                return expectedResponse
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloClientStreaming(responseObserver)

        requestObserver.sendRequests(3)

        // We sleep to ensure the server had time to send all responses
        Thread.sleep(10L)
        verify(exactly = 0) { responseObserver.onError(any()) }
        verify(exactly = 1) { responseObserver.onNext(expectedResponse) }
        verify(exactly = 1) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive){ "Abandoned request channel should be closed"}
    }

    @Test
    fun `Server is cancelled when client sends cancellation`() {

        lateinit var serverSpy: ServerSpy
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                reqChannel = requestChannel
                serverSpy = serverRpcSpy(coroutineContext)
                delay(300000L)
                return expectedResponse
            }
        })

        val (call, requestObserver) = newCall()
        requestObserver.sendRequests(3)
        call.cancel("test",null)
        assert(serverSpy.job?.isCancelled == true)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED")) }
        assertEquals("Job was cancelled",serverSpy.error?.message)
        assert(reqChannel.isClosedForReceive){ "Abandoned request channel should be closed"}
    }

    @Test
    fun `Server is cancelled when client sends error`() {

        lateinit var serverSpy: ServerSpy
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        var requestCount = 0
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                reqChannel = spyk(requestChannel)
                serverSpy = serverRpcSpy(coroutineContext)
                reqChannel.consumeEach {
                    requestCount++
                }

                delay(300000L)
                return expectedResponse
            }
        })

        val (_, requestObserver) = newCall()
        requestObserver.apply {
            onNext(HelloRequest.getDefaultInstance())
            onNext(HelloRequest.getDefaultInstance())
            onError(Status.DATA_LOSS.asRuntimeException())
        }

        assert(serverSpy.job?.isCancelled == true){ "Server job should be cancelled" }
        assertEquals(2,requestCount, "Server should receive two requests")
        assertEquals("Job was cancelled",serverSpy.error?.message)
        assert(reqChannel.isClosedForReceive){ "Abandoned request channel should be closed"}
        verify(exactly = 1) {
            responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED"))
        }
    }

    @Test
    fun `Server method is at least invoked before being cancelled`(){
        val serverMethodCompleted = AtomicBoolean()
        val deferredCtx = CompletableDeferred<CoroutineContext>()
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Default
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                deferredCtx.complete(coroutineContext)

                // Need to receive message since
                // cancellation occurs in client
                // half close.
                requestChannel.receive()
                delay(10000)
                yield()
                serverMethodCompleted.set(true)
                return expectedResponse
            }
        })

        runBlocking {
            val stub = GreeterGrpc
                .newStub(grpcServerRule.channel)
                .withInterceptors(CancellingClientInterceptor)

            // Start the call
            val reqObserver = stub.sayHelloClientStreaming(responseObserver)

            // Wait for the server method to be invoked
            val serverCtx = deferredCtx.await()

            // At this point the server method is suspended. We can send the first message.
            reqObserver.onNext(HelloRequest.getDefaultInstance())

            // Once we call `onCompleted` the server scope will be canceled
            // because of the CancellingClientInterceptor
            reqObserver.onCompleted()

            // We wait for the server scope to complete before proceeding with assertions
            serverCtx[Job]!!.join()

            verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED, "CANCELLED: test")) }

            assert(serverCtx[Job]!!.isCompleted) { "Server job should be completed" }
            assert(serverCtx[Job]!!.isCancelled) { "Server job should be cancelled" }
            assertFalse(serverMethodCompleted.get(), "Server method should not complete")
        }
    }
}