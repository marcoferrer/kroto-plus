package com.github.mferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlin.coroutines.experimental.Continuation

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