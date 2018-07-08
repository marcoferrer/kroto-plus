package com.github.marcoferrer.krotoplus.coroutines

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext


class GrpcContextContinuationInterceptor(
        val grpcContext: io.grpc.Context = io.grpc.Context.current(),
        private val dispatcher: CoroutineDispatcher = DefaultDispatcher
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

        override fun resume(value: T): Unit = wrap { continuation.resume(value) }

        override fun resumeWithException(exception: Throwable): Unit = wrap {
            continuation.resumeWithException(exception)
        }
    }
}

fun io.grpc.Context.asContinuationInterceptor(
        dispatcher: CoroutineDispatcher = DefaultDispatcher
) = GrpcContextContinuationInterceptor(
        grpcContext = this,
        dispatcher = dispatcher
)

