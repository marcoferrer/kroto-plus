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

package com.github.marcoferrer.krotoplus.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.*

@Deprecated("Deprecated in favor of ThreadLocalElement", ReplaceWith("GrpcContextElement"))
class GrpcContextContinuationInterceptor(
    val grpcContext: io.grpc.Context = io.grpc.Context.current(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        dispatcher.interceptContinuation(Wrapper(continuation))

    inner class Wrapper<T>(private val continuation: Continuation<T>) : Continuation<T> {

        override val context: CoroutineContext get() = continuation.context

        private inline fun wrap(block: () -> Unit) {

            val previous = grpcContext.attach()

            try {
                block()
            } finally {
                grpcContext.detach(previous)
            }
        }

        override fun resumeWith(value: Result<T>): Unit = wrap { continuation.resumeWith(value) }

    }
}

@Deprecated("Deprecated in favor of ThreadLocalElement", ReplaceWith("asContextElement"))
fun io.grpc.Context.asContinuationInterceptor(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) = GrpcContextContinuationInterceptor(
    grpcContext = this,
    dispatcher = dispatcher
)

