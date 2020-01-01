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

import com.github.marcoferrer.krotoplus.coroutines.CALL_OPTION_COROUTINE_CONTEXT
import com.github.marcoferrer.krotoplus.coroutines.asContextElement
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext


internal fun CoroutineScope.bindToClientCancellation(observer: ServerCallStreamObserver<*>) {
    observer.setOnCancelHandler {
        this@bindToClientCancellation.cancel()
    }
}

internal fun CoroutineScope.bindScopeCancellationToCall(call: ClientCall<*, *>) {

    val job = coroutineContext[Job]
        ?: error("Unable to bind cancellation to call because scope does not have a job: $this")

    job.invokeOnCompletion { error ->
        if (job.isCancelled) {
            call.cancel(error?.message, error)
        }
    }
}

internal fun StreamObserver<*>.completeSafely(error: Throwable? = null, convertError: Boolean = true) {
    // If the call was cancelled already
    // the stream observer will throw
    kotlin.runCatching {
        if (error != null) {
            if (convertError)
                onError(error.toRpcException()) else
                onError(error)
        } else {
            onCompleted()
        }
    }
}

internal fun Throwable.toRpcException(): Throwable =
    when (this) {
        is StatusException,
        is StatusRuntimeException -> this
        else -> {
            val statusFromThrowable = Status.fromThrowable(this)
            val status = if (
                statusFromThrowable.code == Status.UNKNOWN.code &&
                this is CancellationException
            ) {
                Status.CANCELLED
            } else {
                statusFromThrowable
            }

            status.asRuntimeException(Status.trailersFromThrowable(this))
        }
    }

internal fun MethodDescriptor<*, *>.getCoroutineName(): CoroutineName =
    CoroutineName(fullMethodName)

internal fun newRpcScope(
    callOptions: CallOptions,
    methodDescriptor: MethodDescriptor<*, *>,
    grpcContext: io.grpc.Context = io.grpc.Context.current()
): CoroutineScope = newRpcScope(
    callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT),
    methodDescriptor,
    grpcContext
)

internal fun newRpcScope(
    coroutineContext: CoroutineContext,
    methodDescriptor: MethodDescriptor<*, *>,
    grpcContext: io.grpc.Context = io.grpc.Context.current()
): CoroutineScope{
    val context = newRpcContext(coroutineContext, methodDescriptor, grpcContext)
    return CoroutineScope(context)
}

internal fun newRpcContext(
    coroutineContext: CoroutineContext,
    methodDescriptor: MethodDescriptor<*, *>,
    grpcContext: io.grpc.Context = io.grpc.Context.current()
): CoroutineContext =
    coroutineContext + grpcContext.asContextElement() + methodDescriptor.getCoroutineName()

internal fun <T> CoroutineScope.newProducerScope(channel: SendChannel<T>): ProducerScope<T> =
    object : ProducerScope<T>,
        CoroutineScope by this,
        SendChannel<T> by channel {

        override val channel: SendChannel<T>
            get() = channel
    }