package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.CallStreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

internal interface FlowControlledObserver {

    @ExperimentalCoroutinesApi
    fun <T, T2> CoroutineScope.nextValueWithBackPressure(
        value: T,
        channel: Channel<T>,
        callStreamObserver: CallStreamObserver<T2>,
        isMessagePreloaded: AtomicBoolean
    ){
        try {
            when {
                !channel.isClosedForSend && channel.offer(value) -> callStreamObserver.request(1)

                !channel.isClosedForSend -> {
                    // We are setting isMessagePreloaded to true to prevent the
                    // onReadyHandler from requesting a new message while we have
                    // a message preloaded.
                    isMessagePreloaded.set(true)
                    launch {
                        channel.send(value)
                        callStreamObserver.request(1)

                        // Allow the onReadyHandler to begin requesting messages again.
                        isMessagePreloaded.set(false)
                    }
                }
            }
        } catch (e: Throwable) {
            channel.close(e)
        }
    }
}

@ExperimentalCoroutinesApi
internal fun <T, T2> CallStreamObserver<T>.enableManualFlowControl(
    targetChannel: Channel<T2>,
    isMessagePreloaded: AtomicBoolean
){
    disableAutoInboundFlowControl()
    setOnReadyHandler {
        if (
            isReady &&
            !targetChannel.isFull &&
            !targetChannel.isClosedForSend &&
            isMessagePreloaded.compareAndSet(false, true)
        ) {
            request(1)
        }
    }
}
