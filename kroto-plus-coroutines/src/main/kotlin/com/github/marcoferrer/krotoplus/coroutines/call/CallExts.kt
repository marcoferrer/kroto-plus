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

import com.github.marcoferrer.krotoplus.coroutines.asContextElement
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import io.grpc.ClientCall
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext


internal fun <RespT> CoroutineScope.newSendChannelFromObserver(
    observer: StreamObserver<RespT>,
    capacity: Int = 1
): SendChannel<RespT> =
    actor<RespT>(
        context = observer.exceptionHandler + Dispatchers.Unconfined,
        capacity = capacity,
        start = CoroutineStart.LAZY
    ) {
        try {
            consumeEach { observer.onNext(it) }
            channel.close()
        }catch (e:Throwable){
            channel.close(e)
        }
    }.apply{
        invokeOnClose(observer.completionHandler)
    }


internal fun <ReqT, RespT> CoroutineScope.newManagedServerResponseChannel(
    responseObserver: ServerCallStreamObserver<RespT>,
    isMessagePreloaded: AtomicBoolean,
    requestChannel: Channel<ReqT> = Channel(capacity = 1)
): SendChannel<RespT> {

    val responseChannel = newSendChannelFromObserver(responseObserver)

    responseObserver.enableManualFlowControl(requestChannel,isMessagePreloaded)

    return responseChannel
}

internal fun CoroutineScope.bindToClientCancellation(observer: ServerCallStreamObserver<*>){
    observer.setOnCancelHandler {
        this@bindToClientCancellation.cancel()
    }
}

internal fun CoroutineScope.bindScopeCancellationToCall(call: ClientCall<*, *>){

    val job = coroutineContext[Job]
        ?: error("Unable to bind cancellation to call because scope does not have a job: $this")

    job.apply {
        invokeOnCompletion {
            if(isCancelled){
                call.cancel(it?.message,it?.cause ?: it)
            }
        }
    }
}

internal fun StreamObserver<*>.completeSafely(error: Throwable? = null){
    // If the call was cancelled already
    // the stream observer will throw
    kotlin.runCatching {
        if (error != null)
            onError(error.toRpcException()) else
            onCompleted()
    }
}

internal val StreamObserver<*>.exceptionHandler: CoroutineExceptionHandler
    get() = CoroutineExceptionHandler { _, e ->
        completeSafely(e)
    }

internal val StreamObserver<*>.completionHandler: CompletionHandler
    get() = { completeSafely(it) }

internal fun Throwable.toRpcException(): Throwable =
    when (this) {
        is StatusException,
        is StatusRuntimeException -> this
        else -> {
            val error = Status.fromThrowable(this)
                .asRuntimeException(Status.trailersFromThrowable(this))

            if(error.status.code == Status.Code.UNKNOWN && this is CancellationException)
                Status.CANCELLED
                    .withDescription(this.message)
                    .asRuntimeException() else error
        }
    }

internal fun MethodDescriptor<*, *>.getCoroutineName(): CoroutineName =
    CoroutineName(fullMethodName)

internal fun newRpcScope(
    coroutineContext: CoroutineContext,
    methodDescriptor: MethodDescriptor<*, *>,
    grpcContext: io.grpc.Context = io.grpc.Context.current()
): CoroutineScope = CoroutineScope(
    coroutineContext +
            grpcContext.asContextElement() +
            methodDescriptor.getCoroutineName()
)

@ExperimentalCoroutinesApi
internal fun <T> CoroutineScope.newProducerScope(channel: SendChannel<T>): ProducerScope<T> =
    object : ProducerScope<T>,
        CoroutineScope by this,
        SendChannel<T> by channel {

        override val channel: SendChannel<T>
            get() = channel
    }

internal inline fun <T> StreamObserver<T>.handleUnaryRpc(block: ()->T){
    try{
        onNext(block())
        onCompleted()
    }catch (e: Throwable){
        completeSafely(e)
    }
}

internal inline fun <T> SendChannel<T>.handleStreamingRpc(block: (SendChannel<T>)->Unit){
    try{
        block(this)
        close()
    }catch (e: Throwable){
        close(e.toRpcException())
    }
}

internal inline fun <ReqT, RespT> handleBidiStreamingRpc(
    requestChannel: ReceiveChannel<ReqT>,
    responseChannel: SendChannel<RespT>,
    block: (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
) {
    try{
        block(requestChannel,responseChannel)
        responseChannel.close()
    }catch (e:Throwable){
        responseChannel.close(e.toRpcException())
    }
}