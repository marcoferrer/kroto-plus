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

import io.grpc.Channel
import io.grpc.stub.AbstractStub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newCoroutineContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Represents the metadata related to a specific grpc stub type. This interface is used to provide a generic form of
 * instantiating grpc stubs. This interface is used mainly in implementations of companion objects in the generated
 * coroutine stub clients.
 *
 * ```
 * // GreeterCoroutineGrpc.GreeterCoroutineStub.Companion implements the [StubDefinition] interface
 *
 * println(GreeterCoroutineGrpc.GreeterCoroutineStub.serviceName)
 *
 * val stub = GreeterCoroutineGrpc.GreeterCoroutineStub.newStub(channel)
 *
 * ```
 */
public interface StubDefinition<T : AbstractStub<T>> {

    /**
     * The canonical name of the service this stub represents
     */
    public val serviceName: String

    /**
     * Create a new stub of type [T] that is bound to the supplied [channel]
     */
    public fun newStub(channel: Channel): T

    /**
     * Create a new stub of type [T] that is bound to the supplied [channel] and implicit coroutineContext
     * as a call option.
     */
    public suspend fun newStubWithContext(channel: Channel): T

}

/**
 * Creates a new grpc stub, inheriting the context of the receiving [CoroutineScope]. Additional context elements can
 * be specified with the [context] argument.
 *
 * This builder is meant to provide a mechanism for creating a new stub instance while explicitly defining what scope
 * the executed rpcs wil run in. This method makes it clear that the resulting stub will use the receiving scope to
 * create any child coroutines if necessary.
 *
 * One case of child jobs being created using this scope as a parent is during manual flow control management in
 * streaming variations of rpcs.
 *
 * ```
 *
 * launch {
 *     val stub = newGrpcStub(GreeterCoroutineStub, channel)
 *     ....
 * }
 *
 * ```
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param stubDefinition the definition of the stub to create. Usually implemented in the companion object of the
 * generated client stubs.
 *
 * @param channel the channel that this stub will use to do communications
 *
 */
fun <T : AbstractStub<T>> CoroutineScope.newGrpcStub(
    context: CoroutineContext = EmptyCoroutineContext,
    stubDefinition: StubDefinition<T>,
    channel: Channel
): T {
    val newContext = newCoroutineContext(context)

    return stubDefinition
        .newStub(channel)
        .withCoroutineContext(newContext)
}

