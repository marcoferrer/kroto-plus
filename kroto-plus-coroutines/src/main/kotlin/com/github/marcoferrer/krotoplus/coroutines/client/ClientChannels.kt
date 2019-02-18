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

/**
 *
 */
interface ClientBidiCallChannel<ReqT, RespT> : SendChannel<ReqT>, ReceiveChannel<RespT>{

    public val requestChannel: SendChannel<ReqT>

    public val responseChannel: ReceiveChannel<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): ReceiveChannel<RespT> = responseChannel
}

internal class ClientBidiCallChannelImpl<ReqT, RespT>(
    public override val requestChannel: SendChannel<ReqT>,
    public override val responseChannel: ReceiveChannel<RespT>
) : ClientBidiCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel

/**
 *
 */
interface ClientStreamingCallChannel<ReqT, RespT> : SendChannel<ReqT> {

    public val requestChannel: SendChannel<ReqT>

    public val response: Deferred<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): Deferred<RespT> = response
}

internal class ClientStreamingCallChannelImpl<ReqT, RespT>(
    public override val requestChannel: SendChannel<ReqT>,
    public override val response: Deferred<RespT>
) : ClientStreamingCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by requestChannel


/**
 *
 */
internal class ClientResponseObserverChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    private val responseChannelDelegate: Channel<RespT> = Channel(capacity = 1)
) : ClientResponseObserver<ReqT, RespT>,
    FlowControlledObserver,
    ReceiveChannel<RespT> by responseChannelDelegate,
    CoroutineScope {

    private val isMessagePreloaded = AtomicBoolean()

    private lateinit var requestStream: ClientCallStreamObserver<ReqT>

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