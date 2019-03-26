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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


internal fun <T> CallStreamObserver<*>.applyInboundFlowControl(
    targetChannel: Channel<T>,
    activeInboundJobCount: AtomicInteger
) {
    disableAutoInboundFlowControl()
    setOnReadyHandler {
        if (
            isReady &&
            !targetChannel.isClosedForReceive &&
            activeInboundJobCount.get() == 0
        ) {
            request(1)
        }
    }
}

internal fun <T> CoroutineScope.applyOutboundFlowControl(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){
    val isOutboundJobRunning = AtomicBoolean()
    val channelIterator = targetChannel.iterator()
    streamObserver.setOnReadyHandler {
        if(targetChannel.isClosedForReceive){
            streamObserver.completeSafely()
        }else if(
            streamObserver.isReady &&
            !targetChannel.isClosedForReceive &&
            isOutboundJobRunning.compareAndSet(false, true)
        ){
            launch(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
                streamObserver.completeSafely(e)
                targetChannel.close(e)
            }) {
                try{
                    while(
                        streamObserver.isReady &&
                        !targetChannel.isClosedForReceive &&
                        channelIterator.hasNext()
                    ){
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