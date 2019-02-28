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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext


internal fun <RespT> CoroutineScope.newSendChannelFromObserver(
    observer: StreamObserver<RespT>,
    capacity: Int = 1
): SendChannel<RespT> =
    actor<RespT>(
        context = Dispatchers.Unconfined,
        capacity = capacity,
        start = CoroutineStart.LAZY
    ) {
        consumeEach { observer.onNext(it) }
    }.apply {
        invokeOnClose(observer.completionHandler)
    }

internal fun <ReqT, RespT> CoroutineScope.newManagedServerResponseChannel(
    responseObserver: ServerCallStreamObserver<RespT>,
    isMessagePreloaded: AtomicBoolean,
    requestChannel: Channel<ReqT> = Channel()
): SendChannel<RespT> {

    val responseChannel = newSendChannelFromObserver(responseObserver)

    responseObserver.enableManualFlowControl(requestChannel,isMessagePreloaded)

    return responseChannel
}

internal fun CoroutineScope.bindToClientCancellation(observer: ServerCallStreamObserver<*>){
    observer.setOnCancelHandler { this@bindToClientCancellation.cancel() }
}

internal val StreamObserver<*>.completionHandler: CompletionHandler
    get() = {
        // If the call was cancelled already
        // the stream observer will throw
        runCatching {
            if(it != null)
                onError(it.toRpcException()) else
                onCompleted()
        }
    }

internal val SendChannel<*>.completionHandler: CompletionHandler
    get() = {
        if(!isClosedForSend){
            close(it?.toRpcException())
        }
    }

internal val SendChannel<*>.abandonedRpcHandler: CompletionHandler
    get() = { completionError ->
        if(!isClosedForSend){

            val rpcException = completionError
                ?.toRpcException()
                ?.let { it as? StatusRuntimeException }
                ?.takeUnless { it.status.code == Status.UNKNOWN.code }
                ?: Status.UNKNOWN
                    .withDescription("Abandoned Rpc")
                    .asRuntimeException()

            close(rpcException)
        }
    }

internal fun Throwable.toRpcException(): Throwable =
    when (this) {
        is StatusException,
        is StatusRuntimeException -> this
        else -> Status.fromThrowable(this).asRuntimeException(
            Status.trailersFromThrowable(this)
        )
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
    runCatching { onNext(block()) }
        .onSuccess { onCompleted() }
        .onFailure { onError(it.toRpcException()) }
}

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
internal suspend inline fun <T> SendChannel<T>.handleStreamingRpc(block: suspend (SendChannel<T>)->Unit){
    runCatching { block(this) }
        .onSuccess { close() }
        .onFailure { close(it.toRpcException()) }
}

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
internal suspend inline fun <ReqT, RespT> handleBidiStreamingRpc(
    requestChannel: ReceiveChannel<ReqT>,
    responseChannel: SendChannel<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
) {
    runCatching { block(requestChannel,responseChannel) }
        .onSuccess { responseChannel.close() }
        .onFailure { responseChannel.close(it.toRpcException()) }
}