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

package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.call.applyInboundFlowControl
import com.github.marcoferrer.krotoplus.coroutines.call.applyOutboundFlowControl
import com.github.marcoferrer.krotoplus.coroutines.call.attachOutboundChannelCompletionHandler
import com.github.marcoferrer.krotoplus.coroutines.call.bindToClientCancellation
import com.github.marcoferrer.krotoplus.coroutines.call.completeSafely
import com.github.marcoferrer.krotoplus.coroutines.call.newRpcScope
import com.github.marcoferrer.krotoplus.coroutines.call.toRpcException
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

private const val DEPRECATION_MESSAGE =
    "Delegate based server implementations are deprecated. " +
    "This is resolved by re-generating source stubs with Kroto+ v0.7.0 and up"

@Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
public fun <ReqT, RespT> ServiceScope.serverCallUnary(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend () -> RespT
) {
    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(responseObserver as ServerCallStreamObserver<*>)
        launch(start = CoroutineStart.ATOMIC) {
            try{
                responseObserver.onNext(block())
                responseObserver.onCompleted()
            }catch (e: Throwable){
                responseObserver.completeSafely(e)
            }
        }
    }
}

@Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
public fun <ReqT, RespT> ServiceScope.serverCallServerStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    val responseChannel = Channel<RespT>()
    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>
    with(newRpcScope(initialContext, methodDescriptor)) {
        bindToClientCancellation(serverCallObserver)
        val outboundMessageHandler = applyOutboundFlowControl(serverCallObserver,responseChannel)

        attachOutboundChannelCompletionHandler(
            serverCallObserver, responseChannel,
            onSuccess = { outboundMessageHandler.close() }
        )

        launch(start = CoroutineStart.ATOMIC) {
            try{
                block(responseChannel)
                responseChannel.close()
            }catch (e: Throwable){
                val rpcError = e.toRpcException()
                serverCallObserver.completeSafely(rpcError)
                responseChannel.close(rpcError)
            }finally {
                outboundMessageHandler.close()
            }
        }

        bindScopeCompletionToObserver(serverCallObserver)
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
@Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
public fun <ReqT, RespT> ServiceScope.serverCallClientStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>) -> RespT
): StreamObserver<ReqT> {

    val activeInboundJobCount = AtomicInteger()
    val inboundChannel = Channel<ReqT>()
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { applyInboundFlowControl(inboundChannel, activeInboundJobCount) }

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val requestChannel = ServerRequestStreamChannel(
            coroutineContext = coroutineContext,
            inboundChannel = inboundChannel,
            transientInboundMessageCount = activeInboundJobCount,
            callStreamObserver = serverCallObserver,
            onErrorHandler = {
                // Call cancellation already cancels the coroutine scope
                // and closes the response stream. So we dont need to
                // do anything in this case.
                if(!serverCallObserver.isCancelled) {
                    this@rpcScope.cancel()
                    responseObserver.completeSafely(it)
                }
            }
        )

        launch(start = CoroutineStart.ATOMIC) {
            try {
                responseObserver.onNext(block(requestChannel))
                responseObserver.onCompleted()
            } catch (e: Throwable) {
                responseObserver.completeSafely(e)
            } finally {
                if (!requestChannel.isClosedForReceive) {
                    requestChannel.cancel()
                }
            }
        }

        return requestChannel
    }
}


@UseExperimental(ExperimentalCoroutinesApi::class)
@Deprecated(message = DEPRECATION_MESSAGE, level = DeprecationLevel.WARNING)
public fun <ReqT, RespT> ServiceScope.serverCallBidiStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
): StreamObserver<ReqT> {

    val responseChannel = Channel<RespT>()
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { disableAutoInboundFlowControl() }

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)
        val outboundMessageHandler = applyOutboundFlowControl(serverCallObserver,responseChannel)
        val requestChannel = ServerRequestStreamChannel<ReqT>(
            coroutineContext = coroutineContext,
            callStreamObserver = serverCallObserver,
            onErrorHandler = {
                // Call cancellation already cancels the coroutine scope
                // and closes the response stream. So we dont need to
                // do anything in this case.
                if(!serverCallObserver.isCancelled) {
                    // In the event of a request error, we
                    // need to close the responseChannel before
                    // cancelling the rpcScope.
                    responseObserver.completeSafely(it)
                    responseChannel.close(it)
                    this@rpcScope.cancel()
                }
            }
        )

        attachOutboundChannelCompletionHandler(
            serverCallObserver, responseChannel,
            onSuccess = { outboundMessageHandler.close() }
        )

        launch(start = CoroutineStart.ATOMIC) {
            serverCallObserver.request(1)
            try {
                block(requestChannel, responseChannel)
                responseChannel.close()
            } catch (e: Throwable) {
                val rpcError = e.toRpcException()
                serverCallObserver.completeSafely(rpcError)
                responseChannel.close(rpcError)
            } finally {
                if (!requestChannel.isClosedForReceive) {
                    requestChannel.cancel()
                }
                outboundMessageHandler.close()
            }
        }

        bindScopeCompletionToObserver(serverCallObserver)
        return requestChannel
    }
}

public fun <T> serverCallUnimplementedUnary(methodDescriptor: MethodDescriptor<*, *>): T =
    throw methodDescriptor.getUnimplementedException()

public fun serverCallUnimplementedStream(methodDescriptor: MethodDescriptor<*, *>, responseChannel: SendChannel<*>) {
    responseChannel.close(methodDescriptor.getUnimplementedException())
}

private fun MethodDescriptor<*, *>.getUnimplementedException(): StatusRuntimeException =
    Status.UNIMPLEMENTED
        .withDescription("Method $fullMethodName is unimplemented")
        .asRuntimeException()

/**
 * Binds the completion of the coroutine context job to the outbound stream observer.
 *
 * This is used in server call handlers with outbound streams to ensure completion of any scheduled outbound producers
 * before invoking `onComplete` and closing the call stream.
 *
 */
internal fun CoroutineScope.bindScopeCompletionToObserver(streamObserver: StreamObserver<*>) {

    coroutineContext[Job]?.invokeOnCompletion {
        streamObserver.completeSafely(it)
    }
}


/**
 * Adaptor to a unary method.
 */
interface UnaryMethod<ReqT, RespT> {
    suspend operator fun invoke(request: ReqT): RespT
}

/**
 * Adaptor to a server streaming method.
 */
interface ServerStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(request: ReqT, responseChannel: SendChannel<RespT>)
}

/**
 * Adaptor to a client streaming method.
 */
interface ClientStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(requestChannel: ReceiveChannel<ReqT>): RespT
}

/**
 * Adaptor to a bidirectional streaming method.
 */
interface BidiStreamingMethod<ReqT, RespT> {
    suspend operator fun invoke(requestChannel: ReceiveChannel<ReqT>, responseChannel: SendChannel<RespT>)
}