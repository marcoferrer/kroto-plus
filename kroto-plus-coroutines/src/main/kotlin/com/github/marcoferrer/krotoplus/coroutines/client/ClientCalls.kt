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

import com.github.marcoferrer.krotoplus.coroutines.CALL_OPTION_COROUTINE_CONTEXT
import com.github.marcoferrer.krotoplus.coroutines.SuspendingUnaryObserver
import com.github.marcoferrer.krotoplus.coroutines.call.bindScopeCancellationToCall
import com.github.marcoferrer.krotoplus.coroutines.call.newRpcScope
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.CallOptions
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.asyncBidiStreamingCall
import io.grpc.stub.ClientCalls.asyncClientStreamingCall
import io.grpc.stub.ClientCalls.asyncServerStreamingCall
import io.grpc.stub.ClientCalls.asyncUnaryCall
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.suspendCancellableCoroutine


/**
 * Executes a unary rpc call using the [io.grpc.Channel] and [io.grpc.CallOptions] attached to the
 * receiver [AbstractStub].
 *
 * This method will suspend the invoking coroutine until completion. Its execution is bound to the current
 * [CancellableContinuation] as well as the current [Job]
 *
 * The server is notified of cancellation under one of the following conditions:
 * * The current continuation is, or has become cancelled.
 * * The current job is, or has become cancelled. Either exceptionally or normally.
 *
 * A cancellation of the current scopes job will not always directly correlate to a cancelled continuation.
 * If the job of the receiver stub differs from that of the continuation, its cancellation will cause the
 * this method to throw a [io.grpc.StatusRuntimeException] with a status code of [io.grpc.Status.CANCELLED].
 *
 * @throws io.grpc.StatusRuntimeException The error returned by the server or local scope cancellation.
 *
 */
public suspend fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallUnary(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): RespT = suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->

    with(newRpcScope(cont.context + coroutineContext, method)) {
        val call = channel.newCall(method, callOptions.withCoroutineContext(coroutineContext))
        asyncUnaryCall<ReqT, RespT>(call, request, SuspendingUnaryObserver(cont))
        cont.invokeOnCancellation { call.cancel(it?.message, it) }
        bindScopeCancellationToCall(call)
    }
}

/**
 * Executes a server streaming rpc call using the [io.grpc.Channel] and [io.grpc.CallOptions] attached to the
 * receiver [AbstractStub].
 *
 * This method will return a [ReceiveChannel] to its invoker so that response messages from the target server can
 * be processed in a suspending manner.
 *
 * A new [observer][ClientResponseObserver] is created with back-pressure enabled. The observer will be
 * used by grpc to handle incoming messages and errors from the target server. New messages are submitted to the
 * resulting channel for consumption by the client.
 *
 * In the event of the server returning an [error][io.grpc.StatusRuntimeException], the resulting [ReceiveChannel] will
 * be closed with it. If the local coroutine scope is cancelled then the resulting [ReceiveChannel] will be closed with
 * a [io.grpc.StatusRuntimeException] with a status code of [io.grpc.Status.CANCELLED]
 *
 * The server is notified of cancellations once the current job is, or has become cancelled,
 * either exceptionally or normally.
 *
 */
public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallServerStreaming(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): ReceiveChannel<RespT> {

    with(newRpcScope(coroutineContext, method)) {
        val call = channel.newCall(method, callOptions.withCoroutineContext(coroutineContext))
        val responseObserverChannel = ClientResponseStreamChannel<ReqT, RespT>(coroutineContext)
        asyncServerStreamingCall<ReqT, RespT>(
            call,
            request,
            responseObserverChannel
        )
        bindScopeCancellationToCall(call)
        return responseObserverChannel
    }
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallBidiStreaming(
    method: MethodDescriptor<ReqT, RespT>
): ClientBidiCallChannel<ReqT, RespT> {

    with(newRpcScope(coroutineContext, method)) {

        val call = channel.newCall(method, callOptions.withCoroutineContext(coroutineContext))
        val callChannel = ClientBidiCallChannelImpl<ReqT,RespT>(coroutineContext)
        asyncBidiStreamingCall<ReqT, RespT>(call, callChannel)
        bindScopeCancellationToCall(call)

        return callChannel
    }
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallClientStreaming(
    method: MethodDescriptor<ReqT, RespT>
): ClientStreamingCallChannel<ReqT, RespT> {

    with(newRpcScope(coroutineContext, method)) {
        val call = channel.newCall(method, callOptions.withCoroutineContext(coroutineContext))
        val callChannel = ClientStreamingCallChannelImpl<ReqT,RespT>(coroutineContext)
        asyncClientStreamingCall<ReqT, RespT>(call, callChannel)
        bindScopeCancellationToCall(call)

        return callChannel
    }
}

