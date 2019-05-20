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
import io.mockk.coVerify
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.mapTo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals

class ServerCallBidiStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloStreamingMethod()
    private val expectedResponse = HelloReply.newBuilder().setMessage("reply").build()
    private val responseObserver = spyk<StreamObserver<HelloReply>>(object : StreamObserver<HelloReply> {
        override fun onNext(value: HelloReply?) {}
        override fun onError(t: Throwable?) {}
        override fun onCompleted() {}
    })

    private fun newCall(): Pair<ClientCall<HelloRequest, HelloReply>, StreamObserver<HelloRequest>> {
        val call = grpcServerRule.channel
            .newCall(methodDescriptor, CallOptions.DEFAULT)

        return call to ClientCalls.asyncBidiStreamingCall<HelloRequest, HelloReply>(call, responseObserver)
    }

    private fun StreamObserver<HelloRequest>.sendRequests(qty: Int) {
        repeat(qty) {
            onNext(HelloRequest.newBuilder().setName(it.toString()).build())
        }
        onCompleted()
    }

    @Test
    fun `Server responds successfully on rendezvous requests`() {
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
                requestChannel.mapTo(responseChannel) {
                    HelloReply.newBuilder().setMessage("Reply: ${it.name}").build()
                }
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verifyOrder {
            responseObserver.onNext(match { it.message == "Reply: 0" })
            responseObserver.onNext(match { it.message == "Reply: 1" })
            responseObserver.onNext(match { it.message == "Reply: 2" })
            responseObserver.onCompleted()
        }
        verify(exactly = 0) { responseObserver.onError(any()) }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server responds successfully on uneven requests to response`() {
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
                var groupCount = 0
                val requestIter = requestChannel.iterator()
                while (requestIter.hasNext()) {
                    val requestValues = listOf(
                        requestIter.next(),
                        requestIter.next(),
                        requestIter.next()
                    )
                    responseChannel.send {
                        message = "${groupCount++}:" + requestValues.joinToString { it.name }
                    }
                }
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(9)
        verifyOrder {
            responseObserver.onNext(match { it.message == "0:0, 1, 2" })
            responseObserver.onNext(match { it.message == "1:3, 4, 5" })
            responseObserver.onNext(match { it.message == "2:6, 7, 8" })
            responseObserver.onCompleted()
        }
        verify(exactly = 0) { responseObserver.onError(any()) }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    //
    @Test
    fun `Server responds with error when exception thrown`() {
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
                requestChannel.receive()
                requestChannel.receive()
                throw Status.INVALID_ARGUMENT.asRuntimeException()
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.INVALID_ARGUMENT)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server responds with error when response channel closed`() {
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
                requestChannel.receive()
                requestChannel.receive()
                responseChannel.close(Status.INVALID_ARGUMENT.asRuntimeException())
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.INVALID_ARGUMENT)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server responds with completion even when no responses are sent`() {
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 0) { responseObserver.onError(any()) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 1) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server responds with cancellation when scope cancelled normally`(){
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) = coroutineScope {
                reqChannel = requestChannel
                respChannel = responseChannel
                requestChannel.receive()
                requestChannel.receive()
                launch {
                    delay(5L)
                }
                launch {
                    delay(5)
                    responseChannel.send { message = "error" }
                }
                cancel()
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server responds with error when scope cancelled exceptionally`(){
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                coroutineScope {
                    reqChannel = requestChannel
                    respChannel = responseChannel
                    requestChannel.receive()
                    requestChannel.receive()
                    launch {
                        error("unexpected cancellation")
                    }
                    launch {
                        delay(5)
                        responseChannel.send { message = "error" }
                    }
                }
            }
        })

        val requestObserver = GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloStreaming(responseObserver)

        requestObserver.sendRequests(3)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.UNKNOWN)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server is cancelled when client sends cancellation`() {

        lateinit var serverSpy: ServerSpy
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = requestChannel
                respChannel = responseChannel
                serverSpy = serverRpcSpy(coroutineContext)
                delay(300000L)
            }
        })

        val (call, requestObserver) = newCall()
        requestObserver.sendRequests(3)
        call.cancel("test",null)
        assert(serverSpy.job?.isCancelled == true)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED")) }
        assertEquals("Job was cancelled",serverSpy.error?.message)
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
    }

    @Test
    fun `Server is cancelled when client sends error`() {

        lateinit var serverSpy: ServerSpy
        lateinit var reqChannel: ReceiveChannel<HelloRequest>
        lateinit var respChannel: SendChannel<HelloReply>
        var requestCount = 0
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                reqChannel = spyk(requestChannel)
                respChannel = responseChannel
                serverSpy = serverRpcSpy(coroutineContext)
                reqChannel.consumeEach {
                    requestCount++
                }

                delay(300000L)
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
        assert(reqChannel.isClosedForReceive) { "Request channel should be closed" }
        assert(respChannel.isClosedForSend) { "Response channel should be closed" }
        verify(exactly = 1) {
            responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED"))
        }
    }

    @Test
    fun `Server method is at least invoked before being cancelled`(){
        val respChannel = AtomicReference<SendChannel<HelloReply>?>()
        val serverCtx = AtomicReference<CoroutineContext?>()
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Default
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                respChannel.set(spyk(responseChannel))
                serverCtx.set(coroutineContext)
                // Need to receive message since
                // cancellation occurs in client
                // half close.
                requestChannel.receive()
                delay(10)
                repeat(3){
                    respChannel.get()!!.send { message = "response" }
                }
            }
        })

        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
            .withInterceptors(CancellingClientInterceptor)

        val reqObserver = stub.sayHelloStreaming(responseObserver)
        reqObserver.onNext(HelloRequest.getDefaultInstance())
        reqObserver.onCompleted()

        runBlocking {
            do { delay(100) } while(serverCtx.get() == null)
            assert(serverCtx.get()?.get(Job)!!.isCompleted){ "Server job should be completed" }
            assert(serverCtx.get()?.get(Job)!!.isCancelled){ "Server job should be cancelled" }
            assert(respChannel.get()!!.isClosedForSend){ "Abandoned response channel should be closed" }
            verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED, "CANCELLED: test")) }
            coVerify(exactly = 0) { respChannel.get()!!.send(any()) }
        }
    }
}