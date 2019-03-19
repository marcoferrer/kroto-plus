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

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * [io.grpc.Context] context element for [CoroutineContext].
 *
 * Example:
 *
 * ```
 * // Create a gRPC context key for putting a value into io.grpc.Context
 * val KEY_FOR_DATA = io.grpc.Context.key<String>("data")
 *
 * val grpcContext = Context.current().withValue(KEY_FOR_DATA, "my_data")
 *
 * launch(grpcContext.asContextElement()) {
 *     // Retrieve the value for KEY_FOR_DATA from the current io.grpc.Context and print it
 *     println(KEY_FOR_DATA.get())
 * }
 * ```
 *
 * Note, that you cannot update the current grpc context from inside of the coroutine using
 * [io.grpc.Context.attach]. These updates will be lost on the next suspension.
 * In order to modify the current context you must create a new child context without using
 * 'attach'. Then use `withContext(childContext.asContextElement()) { ... }` to wrap the execution
 * of a specified block of code with the new context.
 *
 * @param context the value of [io.grpc.Context] that will be attached / detached to the coroutine threads.
 * Default value is the context of the current thread. Which is retrieved via [io.grpc.Context.current]
 */
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

/**
 * Instantiates a new instance of [GrpcContextElement] using the receiver
 * as the value of [GrpcContextElement.context].
 *
 * @receiver The context that will be attach / detached to coroutine threads by the [GrpcContextElement]
 *
 * @return The coroutine context element which will handle updating the
 * value of the current grpc context during coroutine execution.
 */
public fun io.grpc.Context.asContextElement(): GrpcContextElement =
    GrpcContextElement(this)