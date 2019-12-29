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

package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.utils.RpcStateInterceptor
import io.grpc.MethodDescriptor
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.testing.GrpcServerRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.fail

abstract class RpcCallTest(
    val methodDescriptor: MethodDescriptor<HelloRequest, HelloReply>
) {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    var nonDirectGrpcServerRule = GrpcServerRule()

    // @[Rule JvmField]
    // public val timeout = CoroutinesTimeout.seconds(COROUTINE_TEST_TIMEOUT)

    var callState = RpcStateInterceptor()

    val expectedRequest = HelloRequest.newBuilder().setName("success").build()

    @BeforeTest
    fun setupCall() {
        callState = RpcStateInterceptor()
    }

    suspend fun RpcStateInterceptor.awaitCancellation(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT
    ){
        client.cancelled.assert(timeout){ "Client should be cancelled" }
        server.cancelled.assert(timeout){ "Server should be cancelled" }
    }

    fun RpcStateInterceptor.blockUntilCancellation(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT
    ) = runBlocking {
        client.cancelled.assert(timeout){ "Client should be cancelled" }
        server.cancelled.assert(timeout){ "Server should be cancelled" }
    }

    suspend fun withTimeoutOrDumpState(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        message: String,
        block: suspend () -> Unit
    ) = try {
        withTimeout(timeout) { block() }
    } catch (e: TimeoutCancellationException) {
        fail("""
            |$message 
            |Timeout after ${timeout}ms
            |$callState
        """.trimMargin())
    }

    suspend fun CompletableDeferred<Unit>.assert(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        message: () -> String
    ) = withTimeoutOrDumpState(timeout, message()){
        await()
    }

    fun CompletableDeferred<Unit>.assertBlocking(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        message: () -> String
    ) = runBlocking { assert(timeout, message) }


    inline fun runTest (
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) = runBlocking(Dispatchers.Default) {
        withTimeoutOrDumpState(timeout, "Rpc did not complete in time"){
            block()
        }
    }


    companion object{
        const val DEFAULT_STATE_ASSERT_TIMEOUT = 10_000L
    }
}
