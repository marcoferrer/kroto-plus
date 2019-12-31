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

import com.github.marcoferrer.krotoplus.coroutines.utils.ClientCallSpyInterceptor
import com.github.marcoferrer.krotoplus.coroutines.utils.RpcStateInterceptor
import io.grpc.BindableService
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.MethodDescriptor
import io.grpc.ServerInterceptors
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
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

// Create StreamRecorder

abstract class RpcCallTest<ReqT, RespT>(
    val methodDescriptor: MethodDescriptor<ReqT, RespT>
) {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @[Rule JvmField]
    var nonDirectGrpcServerRule = GrpcServerRule()

    // @[Rule JvmField]
    // public val timeout = CoroutinesTimeout.seconds(COROUTINE_TEST_TIMEOUT)

    var callState = RpcStateInterceptor()

    val expectedRequest = HelloRequest.newBuilder().setName("success").build()!!

    @BeforeTest
    fun setupCall() {
        callState = RpcStateInterceptor()
    }

    fun registerService(service: BindableService){
        val interceptedService = ServerInterceptors.intercept(service, callState)
        nonDirectGrpcServerRule.serviceRegistry.addService(interceptedService)
        grpcServerRule.serviceRegistry.addService(interceptedService)
    }

    inner class RpcSpy(channel: Channel) {

        constructor(useDirectExecutor: Boolean = true) : this(
            if(useDirectExecutor) grpcServerRule.channel else nonDirectGrpcServerRule.channel
        )

        private val _call = CompletableDeferred<ClientCall<*, *>>()

        val stub = GreeterGrpc.newStub(channel)
            .withInterceptors(ClientCallSpyInterceptor(_call), callState)!!

        val coStub = GreeterCoroutineGrpc.newStub(channel)
            .withInterceptors(ClientCallSpyInterceptor(_call), callState)!!

        val call: ClientCall<ReqT, RespT> by lazy {
            @Suppress("UNCHECKED_CAST")
            runBlocking { _call.await() as ClientCall<ReqT, RespT> }
        }

    }

    suspend fun RpcStateInterceptor.awaitCancellation(timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT){
        client.cancelled.assert(timeout){ "Client call should be cancelled" }
        server.cancelled.assert(timeout){ "Server call should be cancelled" }
    }

    suspend fun RpcStateInterceptor.awaitClose(timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT){
        client.closed.assert(timeout){ "Client call should be closed" }
        server.closed.assert(timeout){ "Server call should be closed" }
    }

    fun RpcStateInterceptor.blockUntilCancellation(timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT) =
        runBlocking { awaitCancellation(timeout) }

    fun RpcStateInterceptor.blockUntilClosed(timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT) =
        runBlocking { awaitClose(timeout) }

    suspend fun <T> withTimeoutOrDumpState(
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        message: String,
        block: suspend () -> T
    ) : T = try {
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


    inline fun <T> runTest (
        timeout: Long = DEFAULT_STATE_ASSERT_TIMEOUT,
        crossinline block: suspend CoroutineScope.() -> T
    ): T = runBlocking(Dispatchers.Default) {
        withTimeoutOrDumpState(timeout, "Rpc did not complete in time"){
            block()
        }
    }


    companion object{
        const val DEFAULT_STATE_ASSERT_TIMEOUT = 10_000L
    }
}
