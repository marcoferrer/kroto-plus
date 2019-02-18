package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel
import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannelImpl
import io.grpc.stub.AbstractStub
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext


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
fun <T : AbstractStub<T>, ReqT, RespT> T.bidiCallChannel(
    block: T.(StreamObserver<RespT>) -> StreamObserver<ReqT>
): ClientBidiCallChannel<ReqT, RespT> {

    val responseObserverChannel = InboundStreamChannel<RespT>()
    val requestObserver = block(responseObserverChannel)

    val requestObserverChannel = CoroutineScope(coroutineContext)
        .newSendChannelFromObserver(requestObserver)

    return ClientBidiCallChannelImpl(
        requestObserverChannel,
        responseObserverChannel
    )
}


