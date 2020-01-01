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
import com.github.marcoferrer.krotoplus.coroutines.client.clientCallBidiStreaming
import com.github.marcoferrer.krotoplus.coroutines.utils.CALL_TRACE_ENABLED
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithCancellation
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus2
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.send
import io.grpc.testing.GrpcServerRule
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals

class BidiStreamingTests : RpcCallTest<HelloRequest, HelloReply>(GreeterCoroutineGrpc.sayHelloStreamingMethod){

    @Test
    fun `Bidi streaming rendezvous impl completes successfully`() {

        registerService(object : GreeterCoroutineGrpc.GreeterImplBase(){

            override val initialContext: CoroutineContext
                get() = Dispatchers.Default

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
        runTest {

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

            val results = responseChannel.toList()
            assertEquals(9, results.size)

            val expected = "name 0|name 0|name 0" +
                    "|name 1|name 1|name 1" +
                    "|name 2|name 2|name 2"
            assertEquals(
                expected,
                results.joinToString(separator = "|") { it.message }
            )
        }
    }

    @Test
    fun `Client cancellation cancels server rpc scope`() {
        registerService(object : GreeterCoroutineGrpc.GreeterImplBase(){

            override val initialContext: CoroutineContext
                get() = Dispatchers.Default

            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                suspendCancellableCoroutine<Unit> {  }
            }
        })
        lateinit var reqChanSpy: SendChannel<HelloRequest>
        runTest(5000L) {
            val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
                .withInterceptors(callState)
                .withCoroutineContext()

            val (requestChannel, responseChannel) = stub.sayHelloStreaming()

            reqChanSpy = spyk(requestChannel)
            val reqJob = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                assertFailsWithStatus2(Status.CANCELLED) {
                    repeat(6) {
                        reqChanSpy.send { name = "name $it" }
                    }
                }
            }

            responseChannel.cancel()
            reqJob.join()
        }

        coVerify(exactly = 2) { reqChanSpy.send(any()) }
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
        runTest {
            val req = HelloRequest.newBuilder()
                .setName("test").build()

            launch {
                repeat(numMessages) {
                    requestChannel.send(req)
                }
                requestChannel.close()
            }

            launch {
                repeat(numMessages) {
                    responseChannel.receive()
                    receivedCount.incrementAndGet()
                }
            }

            callState.awaitClose()
        }

        assert(requestChannel.isClosedForSend) { "Request channel should be closed for send" }
        assert(responseChannel.isClosedForReceive) { "Response channel should be closed for receive" }
        assertEquals(numMessages, receivedCount.get(), "Must response count must equal request count")
    }
}