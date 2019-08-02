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
import javafx.application.Application.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

internal fun <T> CoroutineScope.applyOutboundFlowControl_minSynchronized(
        streamObserver: CallStreamObserver<T>,
        targetChannel: Channel<T>
){
    val isOutboundJobRunning = AtomicBoolean()
    val channelIterator = targetChannel.iterator()
    streamObserver.setOnReadyHandler {
        if(targetChannel.isClosedForReceive){
            streamObserver.completeSafely()
        }else if( synchronized(isOutboundJobRunning) {
                streamObserver.isReady &&
                !targetChannel.isClosedForReceive &&
                isOutboundJobRunning.compareAndSet(false, true) }
        ){
            launch(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
                streamObserver.completeSafely(e)
                targetChannel.close(e)
            }) {
                try{
                    while(
                            !targetChannel.isClosedForReceive &&
                            channelIterator.hasNext()
                    ){
                        synchronized(isOutboundJobRunning) {
                            if (streamObserver.isReady.not()) {
                                isOutboundJobRunning.set(false)
                                return@launch
                            }
                        }
                        val value = channelIterator.next()
                        streamObserver.onNext(value)
                    }
                    if(targetChannel.isClosedForReceive){
                        streamObserver.onCompleted()
                    }
                } finally {
                    isOutboundJobRunning.set(false)
                }
            }
        }
    }
}

internal fun <T> CoroutineScope.applyOutboundFlowControl(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){
    ChannelToStreamObserverAdapter(targetChannel, streamObserver, this)
}

private class ChannelToStreamObserverAdapter<T>(private val sourceChannel: Channel<T>,
                                                private val destStreamObserver: CallStreamObserver<T>,
                                                private val coscope: CoroutineScope) {

    private val isOutboundJobRunning = AtomicBoolean()
    private var currentCont: Continuation<Unit>? = null

    init {
        destStreamObserver.setOnReadyHandler {
            if(sourceChannel.isClosedForReceive)
                destStreamObserver.completeSafely()
            else if (destStreamObserver.isReady &&
                    !sourceChannel.isClosedForReceive &&
                    isOutboundJobRunning.compareAndSet(false, true)) {
                coscope.launch(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
                    destStreamObserver.completeSafely(e)
                    sourceChannel.close(e)
                }) {
                    run()
                }
            }
            else synchronized(this@ChannelToStreamObserverAdapter) {
                currentCont?.resume(Unit)
                currentCont = null
            }
        }
    }

    /**
     * Dispatch messages from the [sourceChannel] to the [destStreamObserver]. This coroutine runs for the entire
     * duration of the underlying call and suspends while either source is empty or destination is not ready.
     */
    private suspend fun run() {
        val channelIterator = sourceChannel.iterator()
        while (!sourceChannel.isClosedForReceive) {
            if (!destStreamObserver.isReady) {
                suspendCancellableCoroutine<Unit> {
                    synchronized(this@ChannelToStreamObserverAdapter) {
                        if (destStreamObserver.isReady)
                            it.resume(Unit)
                        else
                            currentCont = it
                    }
                }
            }
            if (!channelIterator.hasNext())
                break
            destStreamObserver.onNext(channelIterator.next())
        }
        if (sourceChannel.isClosedForReceive) {
            destStreamObserver.onCompleted()
        }
    }
}
