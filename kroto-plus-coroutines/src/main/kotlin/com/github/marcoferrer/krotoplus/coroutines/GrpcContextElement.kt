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