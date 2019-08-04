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
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import java.lang.Math.random
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

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

// Kroto+ 0.4.0 version
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

// Minimally invasive synchronized blocks
internal fun <T> CoroutineScope.applyOutboundFlowControl_minSync(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){
    val isOutboundJobRunning = AtomicBoolean()
    val channelIterator = targetChannel.iterator()
    streamObserver.setOnReadyHandler {
        if(targetChannel.isClosedForReceive){
            streamObserver.completeSafely()
        }else if(
            synchronized(isOutboundJobRunning) { streamObserver.isReady &&
                        !targetChannel.isClosedForReceive &&
                        isOutboundJobRunning.compareAndSet(false, true) }
        ){
            launch(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
                streamObserver.completeSafely(e)
                targetChannel.close(e)
            }) {
                try{
                    while( synchronized(isOutboundJobRunning) { streamObserver.isReady.apply { if (this.not()) isOutboundJobRunning.set(false) } }&&
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

// long coroutine with suspend/resume, work in progress, hangs horribly
internal fun <T> CoroutineScope.applyOutboundFlowControl_longco(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){
    val isOutboundJobRunning = AtomicBoolean()
    val channelIterator = targetChannel.iterator()
    var cont: CancellableContinuation<Unit>? = null
    streamObserver.setOnReadyHandler {
        if(targetChannel.isClosedForReceive){
            streamObserver.completeSafely()
        }else if(
                    streamObserver.isReady &&
                    !targetChannel.isClosedForReceive
        ){
            if (isOutboundJobRunning.compareAndSet(false, true))
                launch(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
                    streamObserver.completeSafely(e)
                    targetChannel.close(e)
                }) {
                    while(!targetChannel.isClosedForReceive){
                        if (!streamObserver.isReady) {
                            suspendCancellableCoroutine<Unit> {
                                synchronized(isOutboundJobRunning) {
                                    if (streamObserver.isReady)
                                        it.resume(Unit)
                                    else
                                        cont = it
                                }
                            }
                        }
                        if (!channelIterator.hasNext())
                            break
                        val value = channelIterator.next()
                        streamObserver.onNext(value)
                    }
                    if(targetChannel.isClosedForReceive){
                        streamObserver.onCompleted()
                    }
                }
            else synchronized (isOutboundJobRunning) {
                cont?.resume(Unit)
                cont = null
            }
        }
    }
}

private typealias MessageHandler = suspend CoroutineScope.() -> Unit

internal fun <T> CoroutineScope.applyOutboundFlowControl_marcoProposal(
    streamObserver: CallStreamObserver<T>,
    targetChannel: Channel<T>
){

    val channelIterator = targetChannel.iterator()
    val messageHandlerBlock: MessageHandler = {
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
    }

    val messageHandlerActor = actor<MessageHandler>(
        start = CoroutineStart.LAZY,
        capacity = Channel.CONFLATED,
        context = Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
            streamObserver.completeSafely(e)
            targetChannel.close(e)
        }
    ) {
        consumeEach {
            if(streamObserver.isReady && !targetChannel.isClosedForReceive){
                it.invoke(this)
            }else{
                streamObserver.completeSafely()
            }
        }
    }

    streamObserver.setOnReadyHandler {
        if(targetChannel.isClosedForReceive){
            streamObserver.completeSafely()
        }else if(
            streamObserver.isReady &&
            !targetChannel.isClosedForReceive
        ){
            // Using sendBlocking here is safe since we're using a conflated channel
            messageHandlerActor.sendBlocking(messageHandlerBlock)
        }
    }
}