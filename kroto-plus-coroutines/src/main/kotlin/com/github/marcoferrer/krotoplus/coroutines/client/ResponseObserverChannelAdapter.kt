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

package com.github.marcoferrer.krotoplus.coroutines.client

import io.grpc.Status
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import java.util.concurrent.atomic.AtomicBoolean

internal class ResponseObserverChannelAdapter<ReqT, RespT>: ClientResponseObserver<ReqT, RespT> {

    private val isAborted = AtomicBoolean()
    private val isCompleted = AtomicBoolean()

    lateinit var scope: ProducerScope<RespT>

    lateinit var callStreamObserver: ClientCallStreamObserver<ReqT>
        private set

    val isActive: Boolean
        get() = !(isAborted.get() || isCompleted.get())

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        require(::scope.isInitialized){ "Producer scope was not initialized" }
        callStreamObserver = requestStream.apply { disableAutoInboundFlowControl() }
    }

    fun beforeCallCancellation(message: String?, cause: Throwable?){
        if(!isAborted.getAndSet(true)) {
            val cancellationStatus = Status.CANCELLED
                .withDescription(message)
                .withCause(cause)
                .asRuntimeException()

            scope.close(CancellationException(message, cancellationStatus))
        }
    }

    override fun onNext(value: RespT) {
        scope.offer(value)
    }

    override fun onError(t: Throwable) {
        isAborted.set(true)
        scope.close(t)
        scope.cancel(CancellationException(t.message,t))
    }

    override fun onCompleted() {
        isCompleted.set(true)
        scope.close()
    }
}

