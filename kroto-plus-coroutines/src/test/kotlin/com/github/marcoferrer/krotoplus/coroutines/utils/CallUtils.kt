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

package com.github.marcoferrer.krotoplus.coroutines.utils

import com.github.marcoferrer.krotoplus.coroutines.CALL_OPTION_COROUTINE_CONTEXT
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object CancellingClientInterceptor : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val _call = next.newCall(method,callOptions)
        return object : SimpleForwardingClientCall<ReqT,RespT>(_call){
            override fun halfClose() {
                super.halfClose()
                // Cancel call after we've verified
                // the first message was sent
                cancel("test",null)
            }
        }

    }
}


class ClientState(

    val intercepted: CompletableDeferred<Unit> = CompletableDeferred(),
    val started: CompletableDeferred<Unit> = CompletableDeferred(),
    val closed: CompletableDeferred<Unit> = CompletableDeferred(),
    val cancelled: CompletableDeferred<Unit> = CompletableDeferred()
){

    override fun toString(): String {
        return "\tClientState(\n" +
                "\t\tintercepted=${intercepted.stateToString()}, \n" +
                "\t\tstarted=${started.stateToString()},\n" +
                "\t\tclosed=${closed.stateToString()},\n" +
                "\t\tcancelled=${cancelled.stateToString()}\n" +
                "\t)"
    }
}

class ServerState(
    val intercepted: CompletableDeferred<Unit> = CompletableDeferred(),
    val wasReady: CompletableDeferred<Unit> = CompletableDeferred(),
    val halfClosed: CompletableDeferred<Unit> = CompletableDeferred(),
    val closed: CompletableDeferred<Unit> = CompletableDeferred(),
    val cancelled: CompletableDeferred<Unit> = CompletableDeferred(),
    val completed: CompletableDeferred<Unit> = CompletableDeferred()
) {
    override fun toString(): String {
        return "\tServerState(\n" +
                "\t\tintercepted=${intercepted.stateToString()},\n" +
                "\t\twasReady=${wasReady.stateToString()},\n" +
                "\t\thalfClosed=${halfClosed.stateToString()},\n" +
                "\t\tclosed=${closed.stateToString()},\n" +
                "\t\tcancelled=${cancelled.stateToString()},\n" +
                "\t\tcompleted=${completed.stateToString()}\n" +
                "\t)"
    }
}

class RpcStateInterceptor(
    val client: ClientState = ClientState(),
    val server: ServerState = ServerState()
) : ClientInterceptor by ClientStateInterceptor(client),
    ServerInterceptor by ServerStateInterceptor(server) {

    override fun toString(): String {
        return "RpcStateInterceptor(\n" +
                "$client,\n" +
                "$server\n" +
                ")"
    }
}

fun CompletableDeferred<Unit>.stateToString(): String =
    "\tisCompleted:$isCompleted,\tisActive:$isActive,\tisCancelled:$isCancelled"

class ClientStateInterceptor(val state: ClientState) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions, next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method,callOptions)) {

            init {
                state.intercepted.complete()
            }

            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener){

                    override fun onClose(status: Status?, trailers: Metadata?) {
                        super.onClose(status, trailers)
                        state.closed.complete()
                    }

                }, headers)
                state.started.complete(Unit)
            }

            override fun cancel(message: String?, cause: Throwable?) {
                super.cancel(message, cause)
                state.cancelled.complete()
            }

        }
    }
}

class ServerStateInterceptor(val state: ServerState) : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>, headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val interceptedCall = object : SimpleForwardingServerCall<ReqT, RespT>(call){
            override fun close(status: Status?, trailers: Metadata?) {
                super.close(status, trailers)
                state.closed.complete()
            }
        }

        return object: SimpleForwardingServerCallListener<ReqT>(next.startCall(interceptedCall, headers)){
            init {
                state.intercepted.complete()
            }

            override fun onReady() {
                super.onReady()
                state.wasReady.complete()
            }

            override fun onHalfClose() {
                super.onHalfClose()
                state.halfClosed.complete()
            }

            override fun onComplete() {
                super.onComplete()
                state.completed.complete()
            }

            override fun onCancel() {
                super.onCancel()
                state.cancelled.complete()
            }
        }
    }
}

private fun CompletableDeferred<Unit>.complete() = complete(Unit)

fun CompletableDeferred<Unit>.awaitBlocking(timeout: Long = 20_000) =
    runBlocking {
        withTimeout(timeout){ await() }
    }

fun newCancellingInterceptor(useNormalCancellation: Boolean) = object : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val job = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)[Job]!!
        if (useNormalCancellation)
            job.cancel() else
            job.cancel(CancellationException("interceptor-cancel"))
        return next.newCall(method, callOptions)
    }
}

