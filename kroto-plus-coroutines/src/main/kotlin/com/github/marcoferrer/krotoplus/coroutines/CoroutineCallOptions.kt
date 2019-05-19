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

import io.grpc.CallOptions
import io.grpc.stub.AbstractStub
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The call option key used for retrieving and storing the [CoroutineContext] used during rpc invocation.
 *
 * Defaults to [EmptyCoroutineContext]
 */
public val CALL_OPTION_COROUTINE_CONTEXT: CallOptions.Key<CoroutineContext> =
    CallOptions.Key.createWithDefault<CoroutineContext>("coroutineContext", EmptyCoroutineContext)

/**
 * Get the coroutineContext the receiving stub is using for cooperative cancellation.
 */
@Deprecated("Use extension property context instead", ReplaceWith("context"))
public val <T : AbstractStub<T>> T.coroutineContext: CoroutineContext
    get() = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)


public val <T : AbstractStub<T>> T.context: CoroutineContext
    get() = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)

/**
 * Returns a new stub with the value of [coroutineContext] attached as a [CallOptions].
 * Any rpcs invoked on the resulting stub will use this context to participate in cooperative cancellation.
 */
public fun <T : AbstractStub<T>> T.withCoroutineContext(context: CoroutineContext): T =
    withOption(CALL_OPTION_COROUTINE_CONTEXT, context)

public fun <T : AbstractStub<T>> T.plusCoroutineContext(context: CoroutineContext): T =
    withOption(CALL_OPTION_COROUTINE_CONTEXT, this.context + context)

/**
 * Returns a new stub with the 'coroutineContext' from the current suspension attached as a [CallOptions].
 * Any rpcs invoked on the resulting stub will use this context to participate in cooperative cancellation.
 */
public suspend fun <T : AbstractStub<T>> T.withCoroutineContext(): T =
    withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)

public suspend fun <T : AbstractStub<T>> T.plusCoroutineContext(): T =
    withOption(CALL_OPTION_COROUTINE_CONTEXT, context + kotlin.coroutines.coroutineContext)

internal fun CallOptions.withCoroutineContext(coroutineContext: CoroutineContext): CallOptions =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, coroutineContext)

internal suspend fun CallOptions.withCoroutineContext(): CallOptions =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)