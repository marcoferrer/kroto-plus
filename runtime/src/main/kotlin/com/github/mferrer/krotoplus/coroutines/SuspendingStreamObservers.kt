package com.github.mferrer.krotoplus.coroutines

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlin.coroutines.experimental.Continuation

class SuspendingUnaryObserver<RespT>(
        @Volatile @JvmField var cont: Continuation<RespT>?
) : StreamObserver<RespT> {

    override fun onNext(value: RespT) { cont?.resume(value) }
    override fun onError(t: Throwable) { cont?.resumeWithException(t) }

    // clear the reference to continuation from the observer's callback
    override fun onCompleted() { cont = null }
}

suspend fun <ReqT,RespT> suspendingAsyncUnaryCall(
        methodDescriptor: MethodDescriptor<ReqT, RespT>,
        channel: Channel,
        callOptions: CallOptions,
        request: ReqT
): RespT =
        suspendCancellableCoroutine { cont: CancellableContinuation<RespT> ->
            asyncUnaryCall(channel.newCall(methodDescriptor, callOptions), request, SuspendingUnaryObserver(cont))
        }