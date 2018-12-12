package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel
import io.grpc.stub.AbstractStub
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*


suspend inline fun <T : AbstractStub<T>, reified R> T.suspendingUnaryCallObserver(
    crossinline block: T.(StreamObserver<R>) -> Unit
): R = suspendCancellableCoroutine { cont: CancellableContinuation<R> ->
    block(SuspendingUnaryObserver(cont))
}

/**
 * Marked as [ObsoleteCoroutinesApi] due to usage of [CoroutineScope.actor]
 * Marked as [ExperimentalCoroutinesApi] due to usage of [Dispatchers.Unconfined]
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@ExperimentalKrotoPlusCoroutinesApi
inline fun <T : AbstractStub<T>, ReqT, RespT> T.bidiCallChannel(
    crossinline block: T.(StreamObserver<RespT>) -> StreamObserver<ReqT>
): ClientBidiCallChannel<ReqT, RespT> {

    val responseObserverChannel = InboundStreamChannel<RespT>()
    val requestObserver = block(responseObserverChannel)

    val context = coroutineContext
        ?.let { it + Dispatchers.Unconfined } ?: Dispatchers.Unconfined

    val requestObserverChannel = CoroutineScope(context)
        .newSendChannelFromObserver(requestObserver)

    return ClientBidiCallChannel(
        requestObserverChannel,
        responseObserverChannel
    )
}


