/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

