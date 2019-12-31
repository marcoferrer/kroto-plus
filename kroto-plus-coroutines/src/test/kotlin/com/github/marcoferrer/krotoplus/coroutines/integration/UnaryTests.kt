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
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import kotlin.test.assertEquals


class UnaryTests : RpcCallTest<HelloRequest, HelloReply>(GreeterCoroutineGrpc.sayHelloMethod) {

    private val request = HelloRequest.newBuilder().setName("request").build()
    private val response = HelloReply.newBuilder().setMessage("reply").build()

    private fun setupServerHandlerNoop(){
        setupServerHandler {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            CompletableDeferred<Nothing>().await()
        }
    }

    private fun setupServerHandler(block: suspend (request: HelloRequest) -> HelloReply){
        grpcServerRule.serviceRegistry.addService(object: GreeterCoroutineGrpc.GreeterImplBase(){
            override suspend fun sayHello(request: HelloRequest): HelloReply = block(request)
        })
    }

    @Test
    fun `Call succeeds on server response`(){
        val rpcSpy = RpcSpy()
        setupServerHandler { request ->
            HelloReply.newBuilder().setMessage("${request.name}:reply").build()
        }

        val result = runTest {
            rpcSpy.coStub.sayHello(request)
        }

        assertEquals("request:reply", result.message)
    }

}