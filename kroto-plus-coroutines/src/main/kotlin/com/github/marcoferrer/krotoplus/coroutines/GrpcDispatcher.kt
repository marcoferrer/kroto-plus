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