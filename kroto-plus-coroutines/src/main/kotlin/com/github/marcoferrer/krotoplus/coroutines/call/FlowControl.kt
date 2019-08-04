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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
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

private typealias MessageHandler = suspend ActorScope<*>.() -> Unit

internal fun <T> CoroutineScope.applyOutboundFlowControl(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){

    val channelIterator = targetChannel.iterator()
    val messageHandlerBlock: MessageHandler = {
        while(
            streamObserver.isReady &&
            channelIterator.hasNext()
        ){
            val value = channelIterator.next()
            streamObserver.onNext(value)
        }
        if(targetChannel.isClosedForReceive){
            channel.close()
        }
    }

    val messageHandlerActor = actor<MessageHandler>(
        capacity = 1,
        context = Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
            streamObserver.completeSafely(e)
            targetChannel.close(e)
        }
    ) {
        consumeEach { it.invoke(this) }
        if(targetChannel.isClosedForReceive){
            streamObserver.onCompleted()
        }
    }

    streamObserver.setOnReadyHandler {
        try {
            if(targetChannel.isClosedForReceive){
                messageHandlerActor.close()
            }else{
                messageHandlerActor.offer(messageHandlerBlock)
            }
        }catch (e: Throwable){
            // If offer throws an exception then it is
            // either already closed or there was a failure
            // which has already cleaned up call resources
        }
    }
}