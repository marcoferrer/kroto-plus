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

package com.github.marcoferrer.krotoplus.coroutines.integration

import com.github.marcoferrer.krotoplus.coroutines.RpcCallTest
import com.github.marcoferrer.krotoplus.coroutines.client.clientCallClientStreaming
import com.github.marcoferrer.krotoplus.coroutines.utils.assertExEquals
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFails
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus2
import com.github.marcoferrer.krotoplus.coroutines.utils.invoke
import com.github.marcoferrer.krotoplus.coroutines.utils.matchThrowable
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse


class ClientStreamingBackPressureTests :
    RpcCallTest<HelloRequest, HelloReply>(GreeterCoroutineGrpc.sayHelloClientStreamingMethod) {

    private fun setupUpServerHandler(
        block: suspend (requestChannel: ReceiveChannel<HelloRequest>) -> HelloReply
    ) {
        val serviceImpl = object : GreeterCoroutineGrpc.GreeterImplBase() {
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply =
                block(requestChannel)
        }

        val service = ServerInterceptors.intercept(serviceImpl, callState)
        grpcServerRule.serviceRegistry.addService(service)
        nonDirectGrpcServerRule.serviceRegistry.addService(service)
    }

    // TODO(marco)
    @Test
    fun `Server receive suspends until client invokes send`(){

    }

    @Test
    fun `Client send suspends until server invokes receive`() {
        val deferredServerChannel = CompletableDeferred<ReceiveChannel<HelloRequest>>()

        setupUpServerHandler { requestChannel ->
            deferredServerChannel.complete(spyk(requestChannel))
            delay(Long.MAX_VALUE)
            HelloReply.getDefaultInstance()
        }

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        val requestCount = AtomicInteger()

        assertFails<CancellationException> {
            runTest {

                val (clientRequestChannel, _) = stub
                    .withCoroutineContext(coroutineContext + Dispatchers.Default)
                    .clientCallClientStreaming(methodDescriptor)

                launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    repeat(10) {
                        clientRequestChannel.send(
                            HelloRequest.newBuilder()
                                .setName(it.toString())
                                .build()
                        )
                        requestCount.incrementAndGet()
                    }
                }

                val serverRequestChannel = deferredServerChannel.await()
                repeat(3){
                    delay(10L)
                    assertEquals(it + 1, requestCount.get())
                    serverRequestChannel.receive()
                }

                cancel()
            }
        }

        verify(exactly = 4) { rpcSpy.call.sendMessage(any()) }
    }

    @Test
    fun `Call completed successfully`() {
        val deferredServerChannel = CompletableDeferred<ReceiveChannel<HelloRequest>>()
        val serverJob = CompletableDeferred<Job>()
        setupUpServerHandler { requestChannel ->
            val job = coroutineContext[Job]!!
            job.invokeOnCompletion { serverJob.complete(job) }
            deferredServerChannel.complete(spyk(requestChannel))
            val reqValues = requestChannel.consumeAsFlow().map { it.name }.toList()
            HelloReply.newBuilder()
                .setMessage(reqValues.joinToString())
                .build()
        }

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub

        val (requestChannel, response) = stub
            .clientCallClientStreaming(methodDescriptor)

        val responseValue = runBlocking(Dispatchers.Default) {
            repeat(3) {
                requestChannel.send(
                    HelloRequest.newBuilder()
                        .setName(it.toString())
                        .build()
                )
            }
            requestChannel.close()
            response.await()
        }

        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assertEquals("0, 1, 2", responseValue.message)
        assertFalse(runBlocking {serverJob.await() }.isCancelled,"Server job should not be cancelled")
    }


    @Test
    fun `Call is cancelled when client closes request channel`() {
        val deferredServerChannel = CompletableDeferred<ReceiveChannel<HelloRequest>>()
        val serverJob = CompletableDeferred<Job>()
        setupUpServerHandler { requestChannel ->
            val job = coroutineContext[Job]!!
            job.invokeOnCompletion { serverJob.complete(job) }
            deferredServerChannel.complete(spyk(requestChannel))
            requestChannel.receive()
            delay(Long.MAX_VALUE)
            HelloReply.getDefaultInstance()
        }

        val rpcSpy = RpcSpy(nonDirectGrpcServerRule.channel)
        val stub = rpcSpy.stub
        val expectedCancelMessage = "Cancelled by client with StreamObserver.onError()"
        val expectedException = IllegalStateException("test")

        val (requestChannel, response) = stub
            .clientCallClientStreaming(methodDescriptor)

        runTest {
            requestChannel.send(
                HelloRequest.newBuilder()
                    .setName(0.toString())
                    .build()
            )
            requestChannel.close(expectedException)

            assertFailsWithStatus2(Status.CANCELLED){
                response.await()
            }
        }

        callState {
            client.closed.assertBlocking { "Client must be closed" }
        }

        verify(exactly = 1) { rpcSpy.call.cancel(expectedCancelMessage, matchThrowable(expectedException)) }
        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assertExEquals(expectedException, response.getCompletionExceptionOrNull()?.cause)
        assert(response.isCancelled) { "Response should not be cancelled" }
        assert(runBlocking {serverJob.await() }.isCancelled){ "Server job should be cancelled" }
    }

}
