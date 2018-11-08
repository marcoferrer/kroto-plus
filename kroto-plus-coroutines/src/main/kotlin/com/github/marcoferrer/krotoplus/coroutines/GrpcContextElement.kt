package com.github.marcoferrer.krotoplus.coroutines

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

public class GrpcContextElement(
    /**
     * The value of [io.grpc.Context] grpc context.
     */
    public val context: io.grpc.Context = io.grpc.Context.current()
) : ThreadContextElement<io.grpc.Context>, AbstractCoroutineContextElement(Key) {
    /**
     * Key of [GrpcContextElement] in [CoroutineContext].
     */
    companion object Key : CoroutineContext.Key<GrpcContextElement>

    override fun updateThreadContext(context: CoroutineContext): io.grpc.Context =
        this@GrpcContextElement.context.attach()

    override fun restoreThreadContext(context: CoroutineContext, oldState: io.grpc.Context) {
        this@GrpcContextElement.context.detach(oldState)
    }

}

fun io.grpc.Context.asContextElement() = GrpcContextElement(this)