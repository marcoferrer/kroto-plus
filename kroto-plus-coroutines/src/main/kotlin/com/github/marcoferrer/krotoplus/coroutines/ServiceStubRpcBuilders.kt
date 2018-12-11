package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.CallOptions
import kotlinx.coroutines.channels.actor
import io.grpc.stub.AbstractStub
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext


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

    return ClientBidiCallChannel(requestObserverChannel, responseObserverChannel)
}

@ExperimentalKrotoPlusCoroutinesApi
val CALL_OPTION_COROUTINE_CONTEXT: CallOptions.Key<CoroutineContext?> =
    CallOptions.Key.create<CoroutineContext>("coroutineContext")

@ExperimentalKrotoPlusCoroutinesApi
val <T : AbstractStub<T>> T.coroutineContext: CoroutineContext?
    get() = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)

@ExperimentalKrotoPlusCoroutinesApi
fun <T : AbstractStub<T>> T.withCoroutineContext(coroutineContext: CoroutineContext): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, coroutineContext)

@ExperimentalKrotoPlusCoroutinesApi
suspend fun <T : AbstractStub<T>> T.withCoroutineContext(): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)

