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
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal interface FlowControlledInboundStreamObserver<T> : StreamObserver<T>, CoroutineScope {

    val inboundChannel: Channel<T>

    val isInboundCompleted: AtomicBoolean

    val transientInboundMessageCount: AtomicInteger

    val callStreamObserver: CallStreamObserver<*>

    val isChannelReadyForClose: Boolean
        get() = isInboundCompleted.get() && transientInboundMessageCount.get() == 0

    fun onNextWithBackPressure(value: T) {
        transientInboundMessageCount.incrementAndGet()
        when {
            !inboundChannel.isClosedForSend && inboundChannel.offer(value) -> {
                transientInboundMessageCount.decrementAndGet()
                requestNextOrClose()
            }
            !inboundChannel.isClosedForSend -> {
                launch(context = Dispatchers.Unconfined) {
                    try {
                        inboundChannel.send(value)
                    } catch (e: Throwable) {
                        inboundChannel.close(e)
                    }
                }.invokeOnCompletion {
                    transientInboundMessageCount.decrementAndGet()
                    if (!inboundChannel.isClosedForReceive) {
                        requestNextOrClose()
                    }
                }
            }
            else -> {
                // We need to drop messages that were received
                // after the inbound channel was  prematurely
                // closed. Usually done to signal a cancellation
                transientInboundMessageCount.decrementAndGet()
            }
        }
    }

    fun requestNextOrClose() {
        if (isChannelReadyForClose)
            inboundChannel.close() else
            callStreamObserver.request(1)
    }

    override fun onCompleted() {
        isInboundCompleted.set(true)
        if (isChannelReadyForClose) {
            inboundChannel.close()
        }
    }
}