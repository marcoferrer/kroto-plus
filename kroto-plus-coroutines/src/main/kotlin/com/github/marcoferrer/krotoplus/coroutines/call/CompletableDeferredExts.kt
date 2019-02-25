package com.github.marcoferrer.krotoplus.coroutines.call

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred

internal fun <T> CompletableDeferred<T>.toStreamObserver(): StreamObserver<T> =
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

