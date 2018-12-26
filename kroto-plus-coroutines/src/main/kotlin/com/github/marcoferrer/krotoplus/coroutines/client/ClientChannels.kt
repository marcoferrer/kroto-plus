package com.github.marcoferrer.krotoplus.coroutines.client

import com.github.marcoferrer.krotoplus.coroutines.FlowControlledObserver
import com.github.marcoferrer.krotoplus.coroutines.enableManualFlowControl
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext


data class ClientBidiCallChannel<ReqT, RespT>(
    val requestChannel: SendChannel<ReqT>,
    val responseChannel: ReceiveChannel<RespT>
) : SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel


data class ClientStreamingCallChannel<ReqT, RespT>(
    val requestChannel: SendChannel<ReqT> = Channel(),
    val response: Deferred<RespT>
) : SendChannel<ReqT> by requestChannel


class ClientResponseObserverChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    private val responseChannelDelegate: Channel<RespT> = Channel()
) : ClientResponseObserver<ReqT, RespT>,
    FlowControlledObserver,
    ReceiveChannel<RespT> by responseChannelDelegate,
    CoroutineScope {

    private val isMessagePreloaded = AtomicBoolean()

    lateinit var requestStream: ClientCallStreamObserver<ReqT>

    @ExperimentalCoroutinesApi
    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        this.requestStream = requestStream.apply {
            enableManualFlowControl(responseChannelDelegate,isMessagePreloaded)
        }
    }

    @ExperimentalCoroutinesApi
    override fun onNext(value: RespT) = nextValueWithBackPressure(
        value = value,
        channel = responseChannelDelegate,
        callStreamObserver = requestStream,
        isMessagePreloaded = isMessagePreloaded
    )

    override fun onError(t: Throwable) {
        responseChannelDelegate.close(t)
    }

    override fun onCompleted() {
        responseChannelDelegate.close()
    }
}