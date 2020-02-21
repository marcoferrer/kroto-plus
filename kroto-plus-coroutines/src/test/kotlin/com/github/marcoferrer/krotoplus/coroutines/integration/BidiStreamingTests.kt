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
import com.github.marcoferrer.krotoplus.coroutines.server.MESSAGE_SERVER_CANCELLED_CALL
import com.github.marcoferrer.krotoplus.coroutines.utils.CALL_TRACE_ENABLED
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithCancellation
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.suspendForever
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.send
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BidiStreamingTests : RpcCallTest<HelloRequest, HelloReply>(GreeterCoroutineGrpc.sayHelloStreamingMethod){

    @Test
    fun `Bidi streaming rendezvous impl completes successfully`() {

        val serverJob = Job()
        registerService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext
                get() = serverJob + Dispatchers.Default

            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                requestChannel.consumeEach { request ->
                    repeat(3) {
                        responseChannel.send { message = request.name }
                    }
                }
                responseChannel.close()
            }
        })
        val results = runTest {
            val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
                .withInterceptors(callState)
                .withCoroutineContext()

            val (requestChannel, responseChannel) = stub.sayHelloStreaming()

            launch(Dispatchers.Default) {
                repeat(3) {
                    requestChannel.send { name = "name $it" }
                }
                requestChannel.close()
            }

            responseChannel.toList()
        }

        assertEquals(9, results.size)
        assertEquals(
            "name 0|name 0|name 0" +
            "|name 1|name 1|name 1" +
            "|name 2|name 2|name 2",
            results.joinToString(separator = "|") { it.message }
        )
        assertFalse(serverJob.isCancelled, "Server job must not be cancelled")
    }

    @Test
    fun `Client cancellation cancels server rpc scope`() {
        val serverJob = Job()
        val hasExecuted = CompletableDeferred<Unit>()
        registerService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext
                get() = serverJob + Dispatchers.Default

            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                hasExecuted.complete(Unit)
                suspendForever("Server")
            }
        })
        lateinit var reqChanSpy: SendChannel<HelloRequest>
        runTest(20_000) {
            val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
                .withInterceptors(callState)
                .withCoroutineContext()

            val (requestChannel, responseChannel) = stub.sayHelloStreaming()

            reqChanSpy = spyk(requestChannel)
            launch(Dispatchers.Default) {
                callState.server.wasReady.await()
                assertFailsWithStatus(Status.CANCELLED) {
                    repeat(6) {
                        reqChanSpy.send { name = "name $it" }
                    }
                }
            }

            callState.server.wasReady.await()
            responseChannel.cancel()
        }

        callState.server.closed.assertBlocking { "Server must be closed" }

        runTest { hasExecuted.await() }

        assertFailsWithCancellation(message = "CANCELLED: $MESSAGE_SERVER_CANCELLED_CALL") {
            runTest { serverJob.ensureActive() }
        }
        assert(serverJob.isCompleted){ "Server job must be completed" }
        assert(serverJob.isCancelled){ "Server job must be cancelled" }
        coVerify(atMost = 2) { reqChanSpy.send(any()) }
        assert(reqChanSpy.isClosedForSend) { "Request channel should be closed after response channel is closed" }
    }

    @Test
    fun `High volume call succeeds`() {
        CALL_TRACE_ENABLED = false
        registerService(object : GreeterCoroutineGrpc.GreeterImplBase() {
            override val initialContext: CoroutineContext = Dispatchers.Default
            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                requestChannel.consumeAsFlow().collectIndexed { index, value ->
                    responseChannel.send(HelloReply.newBuilder().setMessage(value.name).build())
                }
                responseChannel.close()
            }
        })
        val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
            .withInterceptors(callState)

        val (requestChannel, responseChannel) = stub.sayHelloStreaming()

        val numMessages = 100000
        val receivedCount = AtomicInteger()
        runTest(timeout = 60_000 * 2) {
            val req = HelloRequest.newBuilder()
                .setName("test").build()

            launch {
                repeat(numMessages) {
//                    if(it % 10_000 == 0) println("Sent: $it")
                    requestChannel.send(req)
                }
                requestChannel.close()
            }

            launch {
                repeat(numMessages) {
//                    if(it % 10_000 == 0) println("Received: $it")
                    responseChannel.receive()
                    receivedCount.incrementAndGet()
                }
            }

            callState.awaitClose(timeout = 60_000)
        }

        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
        assertEquals(numMessages, receivedCount.get(), "Must response count must equal request count")
    }
}