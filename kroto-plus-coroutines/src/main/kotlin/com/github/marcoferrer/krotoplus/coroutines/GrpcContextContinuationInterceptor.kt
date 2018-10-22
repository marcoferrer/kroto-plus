package com.github.marcoferrer.krotoplus.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.*

@Deprecated("Deprecated in favor of ThreadLocalElement",ReplaceWith("GrpcContextElement"))
class GrpcContextContinuationInterceptor(
        val grpcContext: io.grpc.Context = io.grpc.Context.current(),
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            dispatcher.interceptContinuation(Wrapper(continuation))

    inner class Wrapper<T>(private val continuation: Continuation<T>): Continuation<T> {

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

@Deprecated("Deprecated in favor of ThreadLocalElement",ReplaceWith("asContextElement"))
fun io.grpc.Context.asContinuationInterceptor(
        dispatcher: CoroutineDispatcher = Dispatchers.Default
) = GrpcContextContinuationInterceptor(
        grpcContext = this,
        dispatcher = dispatcher
)

