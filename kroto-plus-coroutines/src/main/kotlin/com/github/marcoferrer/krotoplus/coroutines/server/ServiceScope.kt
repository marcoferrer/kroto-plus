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

package com.github.marcoferrer.krotoplus.coroutines.server

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext


/**
 * Defines a scope for gRPC service implementations. The generated abstract base class for every gRPC coroutine service
 * implements this interface. This interface allows service implementations to define the initial [CoroutineContext]
 * that will be used to populate a new [CoroutineScope] for each incoming rpc method invocation.
 *
 * Usage of this interface from within service implementations may look like this:
 *
 * ```
 * // GreeterCoroutineGrpc.GreeterImplBase implements the [ServiceScope] interface
 *
 * class GreeterServiceImpl : GreeterCoroutineGrpc.GreeterImplBase() {
 *
 *     // Attach the thread local logging context (which was initially setup up in a server interceptor)
 *     // for every new rpc invocation.
 *     val initialContext: CoroutineContext
 *         get() = Dispatchers.Default + MDCContext()
 *
 *     suspend fun sayHello(request: HelloRequest): HelloReply {
 *         ...
 *     }
 * }
 * ```
 *
 * Details showing usage during server rpc invocation at:
 * - Unary RPCs: [ServiceScope.serverCallUnary]
 * - Client Streaming RPCs: [ServiceScope.serverCallClientStreaming]
 * - Server Streaming RPCs: [ServiceScope.serverCallServerStreaming]
 * - Bidirectional RPCs: [ServiceScope.serverCallBidiStreaming]
 *
 */
interface ServiceScope {

    /**
     * The context that will be used to create a new [CoroutineScope] for
     * every incoming rpc request.
     */
    val initialContext: CoroutineContext

}