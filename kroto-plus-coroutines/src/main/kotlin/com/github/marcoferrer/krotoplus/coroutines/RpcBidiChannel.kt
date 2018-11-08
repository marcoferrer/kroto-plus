package com.github.marcoferrer.krotoplus.coroutines

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel


@Deprecated(
    "RpcBidiChannel has been renamed to ClientBidiCallChannel",
    ReplaceWith("ClientBidiCallChannel<ReqT,RespT>")
)
typealias RpcBidiChannel<ReqT, RespT> = ClientBidiCallChannel<ReqT, RespT>

data class ClientBidiCallChannel<ReqT, RespT>(
    val requestChannel: SendChannel<ReqT>,
    val responseChannel: ReceiveChannel<RespT>
) : SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel

data class ServerBidiCallChannel<ReqT, RespT>(
    val requestChannel: ReceiveChannel<ReqT>,
    val responseChannel: SendChannel<RespT>
) : SendChannel<RespT> by responseChannel,
    ReceiveChannel<ReqT> by requestChannel