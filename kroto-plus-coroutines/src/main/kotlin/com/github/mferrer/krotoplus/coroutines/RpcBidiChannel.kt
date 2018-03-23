package com.github.mferrer.krotoplus.coroutines

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

class RpcBidiChannel<ReqT,RespT>(
        val requestChannel: SendChannel<ReqT>,
        val responseChannel: ReceiveChannel<RespT>
) : SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel