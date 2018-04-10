package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.AbstractStub
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.suspendCancellableCoroutine


suspend inline fun <T : AbstractStub<T>, reified R> T.suspendingUnaryCallObserver(
        crossinline block: T.(StreamObserver<R>)->Unit
): R =
        suspendCancellableCoroutine { cont: CancellableContinuation<R> ->
            block(SuspendingUnaryObserver(cont))
        }

inline fun <T : AbstractStub<T>, ReqT, RespT> T.bidiCallChannel(
        crossinline block: T.(StreamObserver<RespT>)->StreamObserver<ReqT>
): ClientBidiCallChannel<ReqT, RespT> {

    val responseObserverChannel = InboundStreamChannel<RespT>()
    val requestObserver = block(responseObserverChannel)

    val requestObserverChannel = actor<ReqT> {
        try{
            channel.consumeEach { requestObserver.onNext(it) }
        }catch (e:Exception){
            requestObserver.onError(e)
            return@actor
        }
        requestObserver.onCompleted()
    }

    return ClientBidiCallChannel(requestObserverChannel, responseObserverChannel)
}