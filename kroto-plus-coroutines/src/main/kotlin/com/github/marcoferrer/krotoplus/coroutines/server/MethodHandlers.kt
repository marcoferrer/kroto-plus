/*
 *  Copyright 2019 Kroto+ Contributors
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

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Adaptor to a unary method.
 */
interface UnaryMethod<ReqT, RespT> {
    suspend operator fun invoke(request: ReqT): RespT
}

/**
 * Adaptor to a server streaming method.
 */
interface ServerStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(request: ReqT, responseChannel: SendChannel<RespT>)
}

/**
 * Adaptor to a client streaming method.
 */
interface ClientStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(requestChannel: ReceiveChannel<ReqT>): RespT
}

/**
 * Adaptor to a bidirectional streaming method.
 */
interface BidiStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(requestChannel: ReceiveChannel<ReqT>, responseChannel: SendChannel<RespT>)
}