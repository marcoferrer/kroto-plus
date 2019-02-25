package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.call.completionHandler
import com.github.marcoferrer.krotoplus.coroutines.call.newProducerScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
    block: suspend ProducerScope<T>.()->Unit
): Job =
    launch(context) { newProducerScope(channel).block() }
        .apply { invokeOnCompletion(channel.completionHandler) }