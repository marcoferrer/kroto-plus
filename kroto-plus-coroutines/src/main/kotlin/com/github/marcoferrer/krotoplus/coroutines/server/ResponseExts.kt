package com.github.marcoferrer.krotoplus.coroutines.server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*


suspend fun <T> CompletableDeferred<T>.respondWith(block: suspend CoroutineScope.() -> T) {
    coroutineScope {
        try {
            complete(block())
        } catch (e: Throwable) {
            completeExceptionally(e)
        }
    }
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
suspend fun <T> SendChannel<T>.respondWith(block: suspend ProducerScope<T>.() -> Unit) {
    coroutineScope {
        val destinationChannel = this@respondWith
        produce<T> {
            try{
                block()
            }catch (e:Throwable){
                destinationChannel.close(e)
            }
            destinationChannel.close()
        }.also {
            it.toChannel(destinationChannel)
        }
    }
}
