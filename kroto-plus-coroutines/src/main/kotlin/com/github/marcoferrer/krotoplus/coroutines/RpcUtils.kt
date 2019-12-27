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

package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.call.newProducerScope
import com.github.marcoferrer.krotoplus.coroutines.call.toRpcException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Launch a [Job] within a [ProducerScope] using the supplied channel as the Receiver.
 * This is useful for emulating the behavior of [CoroutineScope.produce] using an existing
 * channel. The supplied channel is then closed upon completion of the newly created Job.
 *
 * @param channel The channel that will be used as receiver of the [ProducerScope]
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param block the coroutine code which will be invoked in the context of the [ProducerScope].
 *
 * @return [Job] Returns a handle to the [Job] that is executing the [ProducerScope] block
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <T> CoroutineScope.launchProducerJob(
    channel: SendChannel<T>,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend ProducerScope<T>.()->Unit
): Job =
    launch(context, start) {
        newProducerScope(channel).block()
    }.apply {
        invokeOnCompletion {
            if(!channel.isClosedForSend){
                channel.close(it?.toRpcException())
            }
        }
    }

internal suspend fun SendChannel<*>.awaitCloseOrThrow(){
    suspendCancellableCoroutine<Unit> { cont ->
        invokeOnClose { error ->
            if(error == null)
                cont.resume(Unit) else
                cont.resumeWithException(error)
        }
    }
}