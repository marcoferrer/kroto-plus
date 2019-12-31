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

    private val notificationChannel = Channel<Any>()

    private val callStreamObserver: CallStreamObserver<*> = callStreamObserver
        .apply { setOnReadyHandler(this@CallReadyObserver) }

    suspend fun isReady(): Boolean =
        callStreamObserver.isReady || notificationChannel.receive() === READY_TOKEN

    private fun signalReady() = notificationChannel.offer(READY_TOKEN)

    @Deprecated(
        message = "This method should not be called directly",
        level = DeprecationLevel.HIDDEN)
    override fun run() {
        signalReady()
    }

    companion object{
        private object READY_TOKEN
    }

}