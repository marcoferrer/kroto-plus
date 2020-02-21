/*
 *  Copyright 2019 Kroto+ Contributors
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
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.mockk.verify
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.toList
import java.util.concurrent.Phaser
import kotlin.test.Test
import kotlin.test.assertEquals


class ServerStreamingTests :
    RpcCallTest<HelloRequest, HelloReply>(GreeterCoroutineGrpc.sayHelloServerStreamingMethod){

    private fun setupUpServerHandler(
        block: suspend (request: HelloRequest, responseChannel: SendChannel<HelloReply>) -> Unit
    ) {
        val serviceImpl = object : GreeterCoroutineGrpc.GreeterImplBase() {
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest, responseChannel: SendChannel<HelloReply>) = block(request, responseChannel)
        }

        val service = ServerInterceptors.intercept(serviceImpl, callState)
        grpcServerRule.serviceRegistry.addService(service)
        nonDirectGrpcServerRule.serviceRegistry.addService(service)
    }

    @Test
    fun `Call is successful`(){
        val rpcSpy = RpcSpy()
        setupUpServerHandler { request, responseChannel ->
            request.name.map { char ->
                responseChannel.send(HelloReply.newBuilder()
                    .setMessage("response:$char")
                    .build()
                )
            }
        }
        val responseChannel = rpcSpy.coStub.sayHelloServerStreaming(expectedRequest)
        val result = runTest { responseChannel.toList() }

        callState.blockUntilClosed()

        assert(responseChannel.isClosedForReceive){ "Response channel should be closed" }
        assertEquals(expectedRequest.name.length, result.size)
        result.forEachIndexed { index, response ->
            assertEquals("response:${expectedRequest.name[index]}", response.message)
        }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }


    @Test
    fun `Server responds with error`(){
        val rpcSpy = RpcSpy()
        val phaser = Phaser(2)
        setupUpServerHandler { request, responseChannel ->
            repeat(3){
                responseChannel.send(HelloReply.newBuilder()
                    .setMessage("response:$it")
                    .build()
                )
            }
            phaser.arriveAndAwaitAdvance()
            responseChannel.close(Status.INVALID_ARGUMENT.asRuntimeException())
        }

        val responseChannel = rpcSpy.coStub.sayHelloServerStreaming(expectedRequest)
        val result = mutableListOf<HelloReply>()
        runTest {
            repeat(3){
                result += responseChannel.receive()
            }
            phaser.arrive()
            assertFailsWithStatus(Status.INVALID_ARGUMENT) {
                responseChannel.receive()
            }
        }

        callState.blockUntilClosed()

        assert(responseChannel.isClosedForReceive){ "Response channel should be closed" }
        assertEquals(3, result.size)
        result.forEachIndexed { index, response ->
            assertEquals("response:$index", response.message)
        }
        verify(exactly = 0) { rpcSpy.call.cancel(any(), any()) }
    }

}