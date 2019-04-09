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

import com.github.marcoferrer.krotoplus.coroutines.utils.assertFails
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.testing.GrpcServerRule
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals


class ClientCallClientStreamingBackPressureTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    private val methodDescriptor = GreeterGrpc.getSayHelloClientStreamingMethod()

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


    @Test
    fun `Client sends next message on server request channel receive`() {
        lateinit var serverRequestChannel: ReceiveChannel<HelloRequest>
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                serverRequestChannel = requestChannel
                delay(Long.MAX_VALUE)
                return HelloReply.getDefaultInstance()
            }
        })

        val rpcSpy = RpcSpy()
        val stub = rpcSpy.stub
        val requestCount = AtomicInteger()

        assertFails<CancellationException> {
            runBlocking {

                val (requestChannel, response) = stub
                    .withCoroutineContext(coroutineContext + Dispatchers.Default)
                    .clientCallClientStreaming(methodDescriptor)

                launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    repeat(10) {
                        requestChannel.send(
                            HelloRequest.newBuilder()
                                .setName(it.toString())
                                .build()
                        )
                        requestCount.incrementAndGet()
                    }
                }

                repeat(3){
                    delay(10L)
                    assertEquals(it + 1, requestCount.get())
                    serverRequestChannel.receive()
                }

                cancel()
            }
        }
    }

}