package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel


@Deprecated(
    "RpcBidiChannel has been renamed to ClientBidiCallChannel",
    ReplaceWith("ClientBidiCallChannel<ReqT,RespT>")
)
typealias RpcBidiChannel<ReqT, RespT> = ClientBidiCallChannel<ReqT, RespT>

@Deprecated(
    "ClientBidiCallChannel has been moved",
    ReplaceWith("com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel<ReqT,RespT>")
)
typealias ClientBidiCallChannel<ReqT, RespT> = com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel<ReqT, RespT>

@Deprecated("Deprecated in favor of using kroto generated base impl.")
data class ServerBidiCallChannel<ReqT, RespT>(
    val requestChannel: ReceiveChannel<ReqT>,
    val responseChannel: SendChannel<RespT>
) : SendChannel<RespT> by responseChannel,
    ReceiveChannel<ReqT> by requestChannel

