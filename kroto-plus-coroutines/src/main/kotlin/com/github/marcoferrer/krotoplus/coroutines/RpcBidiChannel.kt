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

import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel


@Deprecated(
    message = "Deprecated in favor of back-pressure supporting APIs.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("ClientBidiCallChannel<ReqT,RespT>")
)
typealias RpcBidiChannel<ReqT, RespT> = ClientBidiCallChannel<ReqT, RespT>

@Deprecated(
    message = "Deprecated in favor of back-pressure supporting APIs.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel<ReqT,RespT>")
)
typealias ClientBidiCallChannel<ReqT, RespT> = com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel<ReqT, RespT>

@Deprecated(
    message = "Deprecated in favor of back-pressure supporting APIs.",
    level = DeprecationLevel.WARNING
)
data class ServerBidiCallChannel<ReqT, RespT>(
    val requestChannel: ReceiveChannel<ReqT>,
    val responseChannel: SendChannel<RespT>
) : SendChannel<RespT> by responseChannel,
    ReceiveChannel<ReqT> by requestChannel

