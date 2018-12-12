package com.github.marcoferrer.krotoplus.coroutines.client

import com.github.marcoferrer.krotoplus.coroutines.SuspendingUnaryObserver
import com.github.marcoferrer.krotoplus.coroutines.newSendChannelFromObserver
import com.github.marcoferrer.krotoplus.coroutines.toStreamObserver
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel


public suspend fun <ReqT, RespT, T : AbstractStub<T>> T.clientCallUnary(
    request: ReqT,
    method: MethodDescriptor<ReqT,RespT>
): RespT = suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->
    asyncUnaryCall<ReqT, RespT>(
        channel.newCall(method, callOptions),
        request,
        SuspendingUnaryObserver(cont)
    )
}

public fun <ReqT, RespT, T> T.clientCallServerStreaming(
    request: ReqT,
    method: MethodDescriptor<ReqT, RespT>
): ReceiveChannel<RespT>
        where T : CoroutineScope, T : AbstractStub<T> {

    val responseObserverChannel = ClientResponseObserverChannel<ReqT, RespT>(coroutineContext)

    asyncServerStreamingCall<ReqT, RespT>(
        channel.newCall(method, callOptions),
        request,
        responseObserverChannel
    )

    return responseObserverChannel
}

@ObsoleteCoroutinesApi
public fun <ReqT, RespT, T> T.clientCallBidiStreaming(
    method: MethodDescriptor<ReqT,RespT>
): ClientBidiCallChannel<ReqT, RespT>
        where T : CoroutineScope, T : AbstractStub<T> {

    val responseChannel = ClientResponseObserverChannel<ReqT, RespT>(coroutineContext)
    val requestObserver = asyncBidiStreamingCall<ReqT,RespT>(
        channel.newCall(method, callOptions), responseChannel
    )
    val requestChannel = newSendChannelFromObserver(requestObserver)

    return ClientBidiCallChannel(requestChannel, responseChannel)
}

@ObsoleteCoroutinesApi
public fun <ReqT, RespT, T> T.clientCallClientStreaming(
    method: MethodDescriptor<ReqT,RespT>
): ClientStreamingCallChannel<ReqT, RespT>
        where T : CoroutineScope, T : AbstractStub<T> {

    val completableResponse = CompletableDeferred<RespT>()
    val requestObserver = asyncClientStreamingCall<ReqT, RespT>(
        channel.newCall(method, callOptions), completableResponse.toStreamObserver()
    )
    val requestChannel = newSendChannelFromObserver(requestObserver)
    return ClientStreamingCallChannel(
        requestChannel,
        completableResponse
    )
}

