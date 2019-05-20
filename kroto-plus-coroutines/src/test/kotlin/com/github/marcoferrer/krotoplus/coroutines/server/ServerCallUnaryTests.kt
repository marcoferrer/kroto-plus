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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse


class ServerCallUnaryTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloMethod()
    private val request = HelloRequest.newBuilder().setName("request").build()
    private val expectedResponse = HelloReply.newBuilder().setMessage("reply").build()
    private val responseObserver = spyk<StreamObserver<HelloReply>>(object: StreamObserver<HelloReply>{
        override fun onNext(value: HelloReply?) {}
        override fun onError(t: Throwable?) {}
        override fun onCompleted() {}
    })

    private fun newCall(): ClientCall<HelloRequest, HelloReply> {
        val call = grpcServerRule.channel
            .newCall(methodDescriptor, CallOptions.DEFAULT)

        ClientCalls.asyncUnaryCall<HelloRequest, HelloReply>(call, request, responseObserver)
        return call
    }

    @Test
    fun `Server responds successfully`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply {
                return expectedResponse
            }
        })

        val response = GreeterGrpc.newBlockingStub(grpcServerRule.channel).sayHello(request)
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `Server responds with error`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply {
                throw Status.INVALID_ARGUMENT.asRuntimeException()
            }
        })

        val stub = GreeterGrpc.newBlockingStub(grpcServerRule.channel)

        assertFailsWithStatus(Status.INVALID_ARGUMENT) {
            stub.sayHello(HelloRequest.newBuilder().setName("test").build())
        }
    }

    @Test
    fun `Server responds with cancellation when scope cancelled normally`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply = coroutineScope {
                launch {
                    delay(5L)
                }
                cancel()
                expectedResponse
            }
        })

        assertFailsWithStatus(Status.CANCELLED) {
            GreeterGrpc.newBlockingStub(grpcServerRule.channel).sayHello(request)
        }
    }

    @Test
    fun `Server responds with error when scope cancelled exceptionally`(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply = coroutineScope {
                launch {
                    error("unexpected cancellation")
                }
                expectedResponse
            }
        })

        assertFailsWithStatus(Status.UNKNOWN) {
            GreeterGrpc.newBlockingStub(grpcServerRule.channel).sayHello(request)
        }
    }

    @Test
    fun `Server is cancelled when client sends cancellation`() {
        lateinit var serverSpy: ServerSpy
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply {
                serverSpy = serverRpcSpy(coroutineContext)
                delay(300000L)
                return expectedResponse
            }
        })

        val call = newCall()
        call.cancel("test",null)
        assert(serverSpy.job!!.isCancelled){ "Server job must be cancelled" }
        verify(exactly = 1) {
            responseObserver.onError(matchStatus(Status.CANCELLED,"CANCELLED"))
        }
        assertEquals("Job was cancelled",serverSpy.error?.message)
    }

    @Test
    fun `Server method is at least invoked before being cancelled`(){
        val serverMethodExecuted = AtomicBoolean()
        val serverMethodCompleted = AtomicBoolean()
        val serverCtx = AtomicReference<CoroutineContext?>()

        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Default
            override suspend fun sayHello(request: HelloRequest): HelloReply {
                serverMethodExecuted.set(true)
                serverCtx.set(coroutineContext)
                delay(1)
                serverMethodCompleted.set(true)
                return expectedResponse
            }
        })

        val stub = GreeterGrpc
            .newBlockingStub(grpcServerRule.channel)
            .withInterceptors(CancellingClientInterceptor)

        assertFailsWithStatus(Status.CANCELLED,"CANCELLED: test"){
            stub.sayHello(HelloRequest.getDefaultInstance())
        }

        runBlocking {
            do { delay(50) } while(serverCtx.get() == null)
            assert(serverCtx.get()?.get(Job)!!.isCompleted){ "Server job should be completed" }
            assert(serverCtx.get()?.get(Job)!!.isCancelled){ "Server job should be cancelled" }
            assertFalse(serverMethodCompleted.get(),"Server method should not complete")
        }
    }
}