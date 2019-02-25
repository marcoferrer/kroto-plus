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

