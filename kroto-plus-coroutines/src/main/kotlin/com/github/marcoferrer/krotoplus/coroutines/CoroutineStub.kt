package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.CallOptions
import io.grpc.stub.AbstractStub
import kotlin.coroutines.CoroutineContext

val CALL_OPTION_COROUTINE_CONTEXT: CallOptions.Key<CoroutineContext?> =
    CallOptions.Key.create<CoroutineContext>("coroutineContext")

val <T : AbstractStub<T>> T.coroutineContext: CoroutineContext?
    get() = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)

fun <T : AbstractStub<T>> T.withCoroutineContext(coroutineContext: CoroutineContext): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, coroutineContext)

suspend fun <T : AbstractStub<T>> T.withCoroutineContext(): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)