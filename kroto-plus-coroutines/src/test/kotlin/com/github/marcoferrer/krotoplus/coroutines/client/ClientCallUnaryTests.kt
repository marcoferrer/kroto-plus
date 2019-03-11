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


import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.*
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

    private val methodDescriptor = GreeterGrpc.getSayHelloMethod()
    private val service = spyk(object : GreeterGrpc.GreeterImplBase(){})
    private val request = HelloRequest.newBuilder().setName("request").build()
    private val response = HelloReply.newBuilder().setMessage("reply").build()

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
        every { service.sayHello(request,any()) } just Runs
    }

    @BeforeTest
    fun setupService(){
        grpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Call succeeds on server response`(){
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        every { service.sayHello(request,any()) } answers {
            with(secondArg<StreamObserver<HelloReply>>()){
                onNext(response)
                onCompleted()
            }
        }

        val result = runBlocking {
            stub.clientCallUnary(request, methodDescriptor)
        }

        assertEquals(response, result)
    }

    @Test
    fun `Call fails on server error`(){
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
        val expectedError = Status.INVALID_ARGUMENT.asRuntimeException()

        every { service.sayHello(request,any()) } answers {
            secondArg<StreamObserver<HelloReply>>()
                .onError(expectedError)
        }

        assertFailsWithStatus(Status.INVALID_ARGUMENT){
            runBlocking {
                stub.clientCallUnary(request, methodDescriptor)
            }
        }
    }

    @Test
    fun `Call is canceled when scope is canceled normally`(){
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        runBlocking {
            launch(Dispatchers.Default) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    stub.clientCallUnary(request, methodDescriptor)
                }
                cancel()
            }
        }

        verify { rpcSpy.call.cancel(any(),any()) }
    }

    @Test
    fun `Call is canceled when scope is canceled exceptionally`(){
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        assertFailsWith(IllegalStateException::class,"cancel") {
            runBlocking {
                launch(Dispatchers.Default) {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        stub.clientCallUnary(request, methodDescriptor)
                    }
                    launch {
                        error("cancel")
                    }
                }
            }
        }

        verify { rpcSpy.call.cancel("Parent job is Cancelling",any()) }
    }

    @Test
    fun `Call is canceled when stub context job differs`(){
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        val job = Job()
        runBlocking {
            launch(Dispatchers.Default) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    stub.withCoroutineContext(job)
                        .clientCallUnary(request, methodDescriptor)
                }
                cancel()
            }
        }

        verify { rpcSpy.call.cancel("Job was cancelled",any<CancellationException>()) }
    }

    @Test
    fun `Call is canceled when stub context job is cancelled`(){
        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        setupServerHandlerNoop()

        val job = Job().apply { cancel() }
        assertFailsWithStatus(Status.CANCELLED, "CANCELLED: Job was cancelled") {
            runBlocking {
                launch {
                    stub.withCoroutineContext(job)
                        .clientCallUnary(request, methodDescriptor)
                }
            }
        }

        verify { rpcSpy.call.cancel("Job was cancelled",any()) }
    }

}