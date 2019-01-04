package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

@ExperimentalKrotoPlusCoroutinesApi
public val Dispatchers.Grpc: CoroutineDispatcher
    get() = GrpcDispatcherLoader.dispatcher

@ExperimentalKrotoPlusCoroutinesApi
public fun <T : ServerBuilder<T>> T.coroutineDispatcher(executor: Executor): T {
    GrpcDispatcherLoader.initialize(executor.asCoroutineDispatcher())
    return this
}

@ExperimentalKrotoPlusCoroutinesApi
public fun <T : ServerBuilder<T>> T.coroutineDispatcher(dispatcher: CoroutineDispatcher): T {
    GrpcDispatcherLoader.initialize(dispatcher)
    return this
}

@ExperimentalKrotoPlusCoroutinesApi
internal object GrpcDispatcherLoader {

    internal var dispatcher: CoroutineDispatcher = MissingGrpcCoroutineDispatcher
        private set

    public val isInitialized: Boolean
        get() = dispatcher != MissingGrpcCoroutineDispatcher

    public fun initialize(dispatcher: CoroutineDispatcher){
        require(!isInitialized){
            "Grpc dispatcher has already been initialized with an instance of ${dispatcher::class}"
        }
        this.dispatcher = dispatcher
    }
}

private object MissingGrpcCoroutineDispatcher : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) = missing()

    private fun missing() {
        throw IllegalStateException("Grpc coroutine dispatcher has not been initialized.")
    }

    override fun toString(): String = "GrpcCoroutineDispatcher[missing]"
}