/*
 *  Copyright 2019 Kroto+ Contributors
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
import kotlinx.coroutines.channels.Channel

internal fun CallStreamObserver<*>.newCallReadyObserver(): CallReadyObserver =
    CallReadyObserver(this)

internal class CallReadyObserver(
    callStreamObserver: CallStreamObserver<*>
) : Runnable {

    private val notificationChannel = Channel<READY_TOKEN>(1)

    private var hasRan = false

    private val callStreamObserver: CallStreamObserver<*> = callStreamObserver
        .apply { setOnReadyHandler(this@CallReadyObserver) }

    suspend fun isReady(): Boolean {
        // Suspend until the call is ready.
        // If the call is cancelled before then, an exception
        // will be thrown.
        awaitReady()
        return true
    }

    suspend fun awaitReady() {
        // If our handler hasnt run yet we will want to
        // suspend immediately since its early enough that
        // calls to `callStreamObserver.isReady` will throw
        // and NPE
        if(!hasRan)
            notificationChannel.receive()
        // By the time the on ready handler is invoked, calls
        // to `callStreamObserver.isReady` could return false
        // Here we will continue to poll notifications until
        // the call is ready. For more details reference the
        // documentation for `callStreamObserver.setOnReadyHandler()`
        while(!callStreamObserver.isReady){
            notificationChannel.receive()
        }
    }

    fun cancel(t: Throwable? = null){
        notificationChannel.close(t)
    }

    private fun signalReady() = notificationChannel.offer(READY_TOKEN)

    @Deprecated(
        message = "This method should not be called directly",
        level = DeprecationLevel.HIDDEN)
    override fun run() {
        if(!hasRan) {
            hasRan = true
        }
        signalReady()
    }

    companion object{
        private object READY_TOKEN
    }

}