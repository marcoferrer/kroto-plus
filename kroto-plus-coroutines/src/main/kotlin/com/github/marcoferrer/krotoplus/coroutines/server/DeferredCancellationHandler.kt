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

package com.github.marcoferrer.krotoplus.coroutines.server

import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used for supporting atomic invocations of server method handlers in cases where they are encapsulated
 * within a flow.
 *
 * TODO(marco): Update usage of atomics to kotlinx.atomicfu once stable
 */
internal class DeferredCancellationHandler (val scope: CoroutineScope) : Runnable {

    private val wasCancelled = AtomicBoolean()
    private val handlerStarted = AtomicBoolean()

    override fun run() {
        if(handlerStarted.get()){
            cancel()
        }
        wasCancelled.set(true)
    }

    fun onMethodHandlerStart(){
        handlerStarted.set(true)
        if(wasCancelled.get()){
            cancel()
        }
    }

    private fun cancel(){
        scope.cancel(newCancellationException())
    }

    private fun newCancellationException(): CancellationException {
        val status = Status.CANCELLED
            .withDescription(MESSAGE_SERVER_CANCELLED_CALL)
            .asRuntimeException()

        return CancellationException(status.message, status)
    }
}