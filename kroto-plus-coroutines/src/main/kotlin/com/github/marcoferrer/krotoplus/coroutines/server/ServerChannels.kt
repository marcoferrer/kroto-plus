package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.call.FlowControlledObserver
import io.grpc.stub.CallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal class ServerRequestStreamChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    private val delegateChannel: Channel<ReqT>,
    private val isMessagePreloaded: AtomicBoolean,
    private val callStreamObserver: CallStreamObserver<RespT>,
    private val onErrorHandler: ((Throwable) -> Unit)? = null
) : ReceiveChannel<ReqT> by delegateChannel,
    FlowControlledObserver,
    StreamObserver<ReqT>,
    CoroutineScope {

    @ExperimentalCoroutinesApi
    override fun onNext(value: ReqT) = nextValueWithBackPressure(
        value,
        delegateChannel,
        callStreamObserver,
        isMessagePreloaded
    )

    override fun onError(t: Throwable) {
        delegateChannel.close(t)
        onErrorHandler?.invoke(t)
    }

    override fun onCompleted() {
        delegateChannel.close()
    }
}