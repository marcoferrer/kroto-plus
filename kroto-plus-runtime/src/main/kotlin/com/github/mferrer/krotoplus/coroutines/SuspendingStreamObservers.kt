package com.github.mferrer.krotoplus.coroutines

import io.grpc.CallOptions
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlin.coroutines.experimental.Continuation

class RpcBidiChannel<ReqT,RespT>(
        val requestChannel: SendChannel<ReqT>,
        val responseChannel: ReceiveChannel<RespT>
) : SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel


class InboundStreamChannel<T>(
        val channel: Channel<T> = Channel(Channel.UNLIMITED)
) : StreamObserver<T>, ReceiveChannel<T> by channel {

    override fun onNext(value: T) {
        channel.offer(value)
    }

    override fun onError(t: Throwable?) {
        channel.close(t)
    }

    override fun onCompleted() {
        channel.close()
    }
}

class SuspendingUnaryObserver<RespT>(
        @Volatile @JvmField var cont: Continuation<RespT>?
) : StreamObserver<RespT> {

    override fun onNext(value: RespT) { cont?.resume(value) }
    override fun onError(t: Throwable) {
        cont?.resumeWithException(t)
        cont = null
    }
    override fun onCompleted() { cont = null }
}

suspend fun <ReqT,RespT> suspendingAsyncUnaryCall(
        methodDescriptor: MethodDescriptor<ReqT, RespT>,
        channel: io.grpc.Channel,
        callOptions: CallOptions,
        request: ReqT
): RespT =
        suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->
            asyncUnaryCall(channel.newCall(methodDescriptor, callOptions), request, SuspendingUnaryObserver(cont))
        }


suspend inline fun <T : AbstractStub<T>, reified R> T.suspendingUnaryCallObserver(crossinline block: T.(StreamObserver<R>)->Unit): R =
        suspendCancellableCoroutine { cont: CancellableContinuation<R> ->
            block(SuspendingUnaryObserver(cont))
        }

inline fun <T : AbstractStub<T>, ReqT, RespT> T.bidiCallChannel(
        crossinline block: T.(StreamObserver<RespT>)->StreamObserver<ReqT>
): RpcBidiChannel<ReqT,RespT> {

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

    return RpcBidiChannel(requestObserverChannel, responseObserverChannel)
}