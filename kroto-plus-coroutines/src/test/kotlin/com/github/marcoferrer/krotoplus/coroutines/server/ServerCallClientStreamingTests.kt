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

import com.github.marcoferrer.krotoplus.coroutines.utils.ServerSpy
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
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
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toList
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals

class ServerCallClientStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

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
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED: Job was cancelled")) }
        assertEquals("Job was cancelled",serverSpy.error?.message)
        assert(reqChannel.isClosedForReceive){ "Abandoned request channel should be closed"}
    }
}