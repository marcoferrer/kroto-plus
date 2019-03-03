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

import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatusCode
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.*
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientCallUnaryTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val service = spyk(object : GreeterGrpc.GreeterImplBase(){})
    private val request = HelloRequest.newBuilder().setName("request").build()
    private val response = HelloReply.newBuilder().setMessage("reply").build()

    @BeforeTest
    fun setupService(){
        grpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Unary call success`(){
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        every { service.sayHello(request,any()) } answers {
            with(secondArg<StreamObserver<HelloReply>>()){
                onNext(response)
                onCompleted()
            }
        }

        val result = runBlocking {
            stub.clientCallUnary(request,GreeterGrpc.getSayHelloMethod())
        }

        assertEquals(response, result)
    }

    @Test
    fun `Unary call fails`(){
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
        val expectedError = Status.INVALID_ARGUMENT.asRuntimeException()

        every { service.sayHello(request,any()) } answers {
            secondArg<StreamObserver<HelloReply>>()
                .onError(expectedError)
        }

        assertFailsWithStatusCode(Status.Code.INVALID_ARGUMENT){
            runBlocking {
                stub.clientCallUnary(request,GreeterGrpc.getSayHelloMethod())
            }
        }
    }

    @Test
    fun `Unary call is canceled when scope is canceled normally`(){
        lateinit var callSpy: ClientCall<*,*>
        val channelSpy = spyk(grpcServerRule.channel)
        val stub = GreeterGrpc.newStub(channelSpy)

        every { channelSpy.newCall(GreeterGrpc.getSayHelloMethod(),any()) } answers {
            callSpy = spyk(grpcServerRule.channel.newCall(GreeterGrpc.getSayHelloMethod(),secondArg<CallOptions>()))

            @Suppress("UNCHECKED_CAST")
            callSpy as ClientCall<HelloRequest, HelloReply>
        }
        every { service.sayHello(request,any()) } just Runs

        runBlocking {
            launch(Dispatchers.Default) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    stub.clientCallUnary(request,GreeterGrpc.getSayHelloMethod())
                }
                cancel()
            }
        }

        verify { callSpy.cancel(any(),any()) }
    }

    @Test
    fun `Unary call is canceled when scope is canceled exceptionally`(){
        lateinit var callSpy: ClientCall<*,*>
        val channelSpy = spyk(grpcServerRule.channel)
        val stub = GreeterGrpc.newStub(channelSpy)

        every { channelSpy.newCall(GreeterGrpc.getSayHelloMethod(),any()) } answers {
            callSpy = spyk(grpcServerRule.channel.newCall(GreeterGrpc.getSayHelloMethod(),secondArg<CallOptions>()))

            @Suppress("UNCHECKED_CAST")
            callSpy as ClientCall<HelloRequest, HelloReply>
        }
        every { service.sayHello(request,any()) } just Runs

        assertFailsWith(IllegalStateException::class,"cancel") {
            runBlocking {
                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        stub.clientCallUnary(request, GreeterGrpc.getSayHelloMethod())
                    }
                    launch {
                        error("cancel")
                    }
                }
            }
        }

        verify { callSpy.cancel("Parent job is Cancelling",any()) }
    }

    @Test
    fun `Unary call is still canceled when stub context job differs`(){
        lateinit var callSpy: ClientCall<*,*>
        val channelSpy = spyk(grpcServerRule.channel)
        val stub = GreeterGrpc.newStub(channelSpy)

        every { channelSpy.newCall(GreeterGrpc.getSayHelloMethod(),any()) } answers {
            callSpy = spyk(grpcServerRule.channel.newCall(GreeterGrpc.getSayHelloMethod(),secondArg<CallOptions>()))

            @Suppress("UNCHECKED_CAST")
            callSpy as ClientCall<HelloRequest, HelloReply>
        }
        every { service.sayHello(request,any()) } just Runs

        val job = Job()
        runBlocking {
            launch(Dispatchers.Default) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    stub.withCoroutineContext(job)
                        .clientCallUnary(request, GreeterGrpc.getSayHelloMethod())
                }
                cancel()
            }
        }

        verify { callSpy.cancel("Job was cancelled",any<CancellationException>()) }
    }

    @Test
    fun `Unary call is canceled when stub context job is cancelled`(){
        lateinit var callSpy: ClientCall<*,*>
        val channelSpy = spyk(grpcServerRule.channel)
        val stub = GreeterGrpc.newStub(channelSpy)

        every { channelSpy.newCall(GreeterGrpc.getSayHelloMethod(),any()) } answers {
            callSpy = spyk(grpcServerRule.channel.newCall(GreeterGrpc.getSayHelloMethod(),secondArg<CallOptions>()))

            @Suppress("UNCHECKED_CAST")
            callSpy as ClientCall<HelloRequest, HelloReply>
        }
        every { service.sayHello(request,any()) } just Runs

        val job = Job().apply { cancel() }
        assertFailsWithStatusCode(Status.Code.CANCELLED, "CANCELLED: Job was cancelled") {
            runBlocking {
                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        stub.withCoroutineContext(job)
                            .clientCallUnary(request, GreeterGrpc.getSayHelloMethod())
                    }
                }
            }
        }

        verify { callSpy.cancel("Job was cancelled",any()) }
    }

}