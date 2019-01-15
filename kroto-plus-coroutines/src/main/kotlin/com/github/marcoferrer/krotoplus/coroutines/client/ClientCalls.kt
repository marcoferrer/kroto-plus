package com.github.marcoferrer.krotoplus.coroutines.client

import com.github.marcoferrer.krotoplus.coroutines.*
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel


public suspend fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallUnary(
    request: ReqT,
    method: MethodDescriptor<ReqT,RespT>
): RespT = suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->

    val rpcScope = newRpcScope(cont.context + coroutineContext, method )
    val rpcContext = rpcScope.coroutineContext

    asyncUnaryCall<ReqT, RespT>(
        channel.newCall(method, callOptions.withCoroutineContext(rpcContext)),
        request,
        SuspendingUnaryObserver(cont)
    )
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallServerStreaming(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): ReceiveChannel<RespT> {

    val rpcScope = newRpcScope(coroutineContext, method)
    val rpcContext = rpcScope.coroutineContext
    val responseObserverChannel = ClientResponseObserverChannel<ReqT, RespT>(rpcContext)

    asyncServerStreamingCall<ReqT, RespT>(
        channel.newCall(method, callOptions.withCoroutineContext(rpcContext)),
        request,
        responseObserverChannel
    )

    return responseObserverChannel
}

@ObsoleteCoroutinesApi
public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallBidiStreaming(
    method: MethodDescriptor<ReqT,RespT>
): ClientBidiCallChannel<ReqT, RespT> {

    val rpcScope = newRpcScope(coroutineContext, method)
    val rpcContext = rpcScope.coroutineContext
    val responseChannel = ClientResponseObserverChannel<ReqT, RespT>(rpcContext)
    val requestObserver = asyncBidiStreamingCall<ReqT,RespT>(
        channel.newCall(method, callOptions.withCoroutineContext(rpcContext)),
        responseChannel
    )
    val requestChannel = rpcScope.newSendChannelFromObserver(requestObserver)

    return ClientBidiCallChannelImpl(requestChannel, responseChannel)
}

public fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallClientStreaming(
    method: MethodDescriptor<ReqT,RespT>
): ClientStreamingCallChannel<ReqT, RespT> {

    val rpcScope = newRpcScope(coroutineContext, method)
    val rpcContext = rpcScope.coroutineContext
    val completableResponse = CompletableDeferred<RespT>()
    val requestObserver = asyncClientStreamingCall<ReqT, RespT>(
        channel.newCall(method, callOptions.withCoroutineContext(rpcContext)),
        completableResponse.toStreamObserver()
    )
    val requestChannel = rpcScope.newSendChannelFromObserver(requestObserver)
    return ClientStreamingCallChannelImpl(
        requestChannel,
        completableResponse
    )
}

