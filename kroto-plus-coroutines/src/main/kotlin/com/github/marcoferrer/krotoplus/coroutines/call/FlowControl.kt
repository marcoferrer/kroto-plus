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

package com.github.marcoferrer.krotoplus.coroutines.call

import io.grpc.stub.CallStreamObserver
import io.grpc.stub.ClientCallStreamObserver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


internal fun <T> CallStreamObserver<*>.applyInboundFlowControl(
    targetChannel: Channel<T>,
    transientInboundMessageCount: AtomicInteger
) {
    disableAutoInboundFlowControl()
    setOnReadyHandler {
        if (
            isReady &&
            !targetChannel.isClosedForReceive &&
            transientInboundMessageCount.get() == 0
        ) {
            request(1)
        }
    }
}

internal typealias MessageHandler = suspend ActorScope<*>.() -> Unit

internal fun <T> CoroutineScope.applyOutboundFlowControl(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
): SendChannel<MessageHandler> {

    val isCompleted = AtomicBoolean()
    val channelIterator = targetChannel.iterator()
    val messageHandlerBlock: MessageHandler = handler@{
        try {
            while (
                streamObserver.isReady &&
                channelIterator.hasNext()
            ) {
                streamObserver.onNext(channelIterator.next())
            }
        } catch (e: Throwable) {
            // If the outbound channel is closed while we are suspended
            // on `hasNext()`, then the close exception will be throw
            // and need to be propagated to the call stream
            if (targetChannel.isClosedForSend) {
                // We cant convert our error before passing it to a 'client' stream observer
                // because we will loose the cause when 'onError' cancels the underlying call.
                // As for 'server' stream observers, we still need to convert the error before
                // returning it to the client. Unfortunately checking the observer type is
                // the only way we can do this in the current implementation.
                streamObserver.completeSafely(e, convertError = streamObserver !is ClientCallStreamObserver)
                isCompleted.set(true)
            } else {
                throw e
            }
        }
        if (targetChannel.isClosedForReceive &&
            !coroutineContext[Job]!!.isCancelled &&
            isCompleted.compareAndSet(false, true)
        ) {
            streamObserver.onCompleted()
            channel.close()
        }
    }

    val messageHandlerActor = actor<MessageHandler>(
        capacity = Channel.BUFFERED,
        context = Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
            streamObserver.completeSafely(e)
            targetChannel.close(e)
        }
    ) {

        for (handler in channel) {
            if (isCompleted.get()) break
            handler(this)
        }
        if (!isCompleted.get()) {
            streamObserver.completeSafely()
        }
    }

    targetChannel.invokeOnClose {
        messageHandlerActor.close()
    }

    streamObserver.setOnReadyHandler {
        try {
            if (!messageHandlerActor.isClosedForSend) {
                messageHandlerActor.offer(messageHandlerBlock)
            }
        } catch (e: Throwable) {
            // If offer throws an exception then it is
            // either already closed or there was a failure
            // which has already cleaned up call resources
        }
    }

    return messageHandlerActor
}
