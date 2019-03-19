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
import kotlinx.coroutines.channels.SendChannel
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals

class ServerCallServerStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloServerStreamingMethod()
    private val request = HelloRequest.newBuilder().setName("abc").build()
    private val expectedResponse = HelloReply.newBuilder().setMessage("reply").build()
    private val responseObserver = spyk<StreamObserver<HelloReply>>(object: StreamObserver<HelloReply>{
        override fun onNext(value: HelloReply?) {}
        override fun onError(t: Throwable?) {}
        override fun onCompleted() {}
    })

    private fun newCall(): ClientCall<HelloRequest, HelloReply> {
        val call = grpcServerRule.channel
            .newCall(methodDescriptor, CallOptions.DEFAULT)

        ClientCalls.asyncServerStreamingCall<HelloRequest, HelloReply>(call, request, responseObserver)
        return call
    }

    @Test
    fun `Server responds successfully`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                for(char in request.name) {
                    responseChannel.send { message = char.toString() }
                }
            }
        })

        GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloServerStreaming(request,responseObserver)

        verifyOrder {
            responseObserver.onNext(match { it.message == "a" })
            responseObserver.onNext(match { it.message == "b" })
            responseObserver.onNext(match { it.message == "c" })
            responseObserver.onCompleted()
        }
        verify(exactly = 0) { responseObserver.onError(any()) }
    }

    @Test
    fun `Server responds with error when exception thrown`(){
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                respChannel = responseChannel
                throw Status.INVALID_ARGUMENT.asRuntimeException()
            }
        })

        GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloServerStreaming(request,responseObserver)

        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.INVALID_ARGUMENT)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(respChannel.isClosedForSend){ "Server response channel should be closed" }
    }

    @Test
    fun `Server responds with cancellation when scope cancelled normally`(){
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                respChannel = responseChannel
                coroutineScope {
                    launch {
                        delay(5L)
                    }
                    cancel()
                    repeat(3){
                        responseChannel.send(expectedResponse)
                    }
                }
            }
        })

        GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloServerStreaming(request,responseObserver)

        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(respChannel.isClosedForSend){ "Server response channel should be closed" }
    }

    @Test
    fun `Server responds with error when scope cancelled exceptionally`(){
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                respChannel = responseChannel
                coroutineScope {
                    launch {
                        error("unexpected cancellation")
                    }
                    repeat(3){
                        responseChannel.send(expectedResponse)
                    }
                }
            }
        })

        GreeterGrpc.newStub(grpcServerRule.channel)
            .sayHelloServerStreaming(request,responseObserver)

        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.UNKNOWN)) }
        verify(exactly = 0) { responseObserver.onNext(any()) }
        verify(exactly = 0) { responseObserver.onCompleted() }
        assert(respChannel.isClosedForSend){ "Server response channel should be closed" }
    }

    @Test
    fun `Server is cancelled when client sends cancellation`() {

        lateinit var serverSpy: ServerSpy
        lateinit var respChannel: SendChannel<HelloReply>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                respChannel = responseChannel
                serverSpy = serverRpcSpy(coroutineContext)
                delay(300000L)
            }
        })

        val call = newCall()
        call.cancel("test",null)
        assert(serverSpy.job?.isCancelled == true)
        verify(exactly = 1) { responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED: Job was cancelled")) }
        assertEquals("Job was cancelled",serverSpy.error?.message)
        assert(respChannel.isClosedForSend){ "Abandoned response channel should be closed"}
    }
}