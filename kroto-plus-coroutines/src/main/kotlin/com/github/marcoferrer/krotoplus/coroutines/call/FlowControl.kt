package com.github.marcoferrer.krotoplus.coroutines.call

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
    ) {
        try {
            when {
                !channel.isClosedForSend && channel.offer(value) -> callStreamObserver.request(1)

                !channel.isClosedForSend -> {
                    // We are setting isMessagePreloaded to true to prevent the
                    // onReadyHandler from requesting a new message while we have
                    // a message preloaded.
                    isMessagePreloaded.set(true)

                    // Using [CoroutineStart.UNDISPATCHED] ensure that
                    // values are sent in the proper order (FIFO).
                    // This also prevents a race between [StreamObserver.onNext] and
                    // [StreamObserver.onComplete] by making sure all preloaded messages
                    // have been submitted before invoking [Channel.close]
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            channel.send(value)
                            callStreamObserver.request(1)

                            // Allow the onReadyHandler to begin requesting messages again.
                            isMessagePreloaded.set(false)
                        }catch (e: Throwable){
                            channel.close(e)
                        }
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
) {
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
