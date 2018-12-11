package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred

class CompletableDeferredResponse<RespT>(
    private val observer: StreamObserver<RespT>,
    private val delegate: CompletableDeferred<RespT> = CompletableDeferred()
) : CompletableDeferred<RespT> by delegate,
    StreamObserver<RespT> {

    override fun onNext(value: RespT) {
        complete(value)
    }

    override fun onError(t: Throwable) {
        completeExceptionally(t)
    }

    override fun onCompleted() {
        // NOOP
    }

    override fun complete(value: RespT): Boolean =
        delegate.complete(value).also { isCompleted ->
            if(isCompleted) observer.apply {
                onNext(value)
                onCompleted()
            }
        }

    override fun completeExceptionally(exception: Throwable): Boolean =
        delegate.completeExceptionally(exception).also { isCompleted ->
            if(isCompleted){
                observer.onError(exception)
            }
        }
}

fun <RespT> StreamObserver<RespT>.toCompletableDeferred(): CompletableDeferredResponse<RespT> =
    CompletableDeferredResponse(observer = this)

inline fun <RespT> CompletableDeferredResponse<RespT>.handleRequest(block: (CompletableDeferred<RespT>)->Unit){
    try{
        block(this)
    }catch (t: Throwable){
        // This is a fallback in the event
        // there was an exception during the
        // invocation of a service method.
        completeExceptionally(t)
    }
}

