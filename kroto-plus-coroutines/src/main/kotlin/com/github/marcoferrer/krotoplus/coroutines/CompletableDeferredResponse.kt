package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred

class CompletableDeferredObserver<RespT> internal constructor(
    private val delegateObserver: StreamObserver<RespT>,
    private val delegateDeferred: CompletableDeferred<RespT> = CompletableDeferred()
) : CompletableDeferred<RespT> by delegateDeferred {

    override fun complete(value: RespT): Boolean =
        delegateDeferred.complete(value).also { isCompleted ->
            if(isCompleted){
                delegateObserver.onNext(value)
                delegateObserver.onCompleted()
            }
        }

    override fun completeExceptionally(exception: Throwable): Boolean =
        delegateDeferred.completeExceptionally(exception).also { isCompleted ->
            if(isCompleted){
                delegateObserver.onError(exception)
            }
        }
}


fun <T> CompletableDeferred<T>.toStreamObserver(): StreamObserver<T> =
    object : StreamObserver<T> {

        /**
         * Since [CompletableDeferred] is a single value coroutine primitive,
         * once [onNext] has been called we can be sure that we have completed
         * our stream.
         *
         */
        override fun onNext(value: T) {
            complete(value)
        }

        override fun onError(t: Throwable) {
            completeExceptionally(t)
        }

        /**
         * This method is intentionally left blank.
         *
         * Since this stream represents a single value, completion is marked by
         * the first invocation of [onNext]
         */
        override fun onCompleted() {
            // NOOP
        }
    }


fun <RespT> StreamObserver<RespT>.toCompletableDeferred(): CompletableDeferredObserver<RespT> =
    CompletableDeferredObserver(delegateObserver = this)

