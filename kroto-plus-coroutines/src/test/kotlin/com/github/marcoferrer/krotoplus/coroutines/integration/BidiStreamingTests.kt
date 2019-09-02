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

import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
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
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals

class BidiStreamingTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    var nonDirectGrpcServerRule = GrpcServerRule()

    @Test
    fun `Bidi streaming rendezvous impl completes successfully`() {
        nonDirectGrpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){

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
        runBlocking {

            val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
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
        nonDirectGrpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){

            override val initialContext: CoroutineContext
                get() = Dispatchers.Default

            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                delay(10000L)
            }
        })
        runBlocking(Dispatchers.Default) {
            withTimeout(5000L) {
                val stub = GreeterCoroutineGrpc.newStub(nonDirectGrpcServerRule.channel)
                    .withCoroutineContext()

                val (requestChannel, responseChannel) = stub.sayHelloStreaming()

                val reqChanSpy = spyk(requestChannel)
                val reqJob = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    assertFailsWithStatus(Status.CANCELLED) {
                        repeat(6) {
                            reqChanSpy.send { name = "name $it" }
                        }
                    }
                }

                responseChannel.cancel()
                reqJob.join()

                coVerify(exactly = 2) { reqChanSpy.send(any()) }
                assert(reqChanSpy.isClosedForSend) { "Request channel should be closed after response channel is closed" }
            }
        }
    }
}