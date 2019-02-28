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

public val CALL_OPTION_COROUTINE_CONTEXT: CallOptions.Key<CoroutineContext> =
    CallOptions.Key.createWithDefault<CoroutineContext>("coroutineContext", EmptyCoroutineContext)

public val <T : AbstractStub<T>> T.coroutineContext: CoroutineContext
    get() = callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)

public fun <T : AbstractStub<T>> T.withCoroutineContext(coroutineContext: CoroutineContext): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, coroutineContext)

public suspend fun <T : AbstractStub<T>> T.withCoroutineContext(): T =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)

public fun CallOptions.withCoroutineContext(coroutineContext: CoroutineContext): CallOptions =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, coroutineContext)

public suspend fun CallOptions.withCoroutineContext(): CallOptions =
    this.withOption(CALL_OPTION_COROUTINE_CONTEXT, kotlin.coroutines.coroutineContext)