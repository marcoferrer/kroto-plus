package com.github.marcoferrer.krotoplus.coroutines.client

import com.github.marcoferrer.krotoplus.coroutines.*
import com.github.marcoferrer.krotoplus.coroutines.call.newRpcScope
import com.github.marcoferrer.krotoplus.coroutines.call.newSendChannelFromObserver
import com.github.marcoferrer.krotoplus.coroutines.call.toStreamObserver
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel


public suspend fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallUnary(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): RespT = suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->

    with(newRpcScope(cont.context + coroutineContext, method)) {
        asyncUnaryCall<ReqT, RespT>(
            channel.newCall(method, callOptions.withCoroutineContext(coroutineContext)),
            request,
            SuspendingUnaryObserver(cont)
        )
    }
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallServerStreaming(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): ReceiveChannel<RespT> {

    with(newRpcScope(coroutineContext, method)) rpcScope@{
        val responseObserverChannel = ClientResponseObserverChannel<ReqT, RespT>(coroutineContext)

        asyncServerStreamingCall<ReqT, RespT>(
            channel.newCall(method, callOptions.withCoroutineContext(coroutineContext)),
            request,
            responseObserverChannel
        )
        return responseObserverChannel
    }
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallBidiStreaming(
    method: MethodDescriptor<ReqT, RespT>
): ClientBidiCallChannel<ReqT, RespT> {

    with(newRpcScope(coroutineContext, method)){
        val responseChannel = ClientResponseObserverChannel<ReqT, RespT>(coroutineContext)
        val requestObserver = asyncBidiStreamingCall<ReqT, RespT>(
            channel.newCall(method, callOptions.withCoroutineContext(coroutineContext)),
            responseChannel
        )
        val requestChannel = newSendChannelFromObserver(requestObserver)

        return ClientBidiCallChannelImpl(requestChannel, responseChannel)
    }
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallClientStreaming(
    method: MethodDescriptor<ReqT, RespT>
): ClientStreamingCallChannel<ReqT, RespT> {

    with(newRpcScope(coroutineContext, method)) rpcScope@{
        val completableResponse = CompletableDeferred<RespT>()
        val requestObserver = asyncClientStreamingCall<ReqT, RespT>(
            channel.newCall(method, callOptions.withCoroutineContext(coroutineContext)),
            completableResponse.toStreamObserver()
        )
        val requestChannel = newSendChannelFromObserver(requestObserver)
        return ClientStreamingCallChannelImpl(
            requestChannel,
            completableResponse
        )
    }
}

