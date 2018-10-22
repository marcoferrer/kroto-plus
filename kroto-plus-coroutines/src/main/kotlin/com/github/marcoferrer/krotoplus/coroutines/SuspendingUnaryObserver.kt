package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SuspendingUnaryObserver<RespT>(
        @Volatile @JvmField private var cont: Continuation<RespT>?
) : StreamObserver<RespT> {

    override fun onNext(value: RespT) {
        cont?.resume(value)
        cont = null
    }
    override fun onError(t: Throwable) {
        cont?.resumeWithException(t)
        cont = null
    }
    override fun onCompleted() { cont = null }
}