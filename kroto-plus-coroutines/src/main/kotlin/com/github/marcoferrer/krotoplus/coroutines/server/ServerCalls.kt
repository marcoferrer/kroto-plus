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

import com.github.marcoferrer.krotoplus.coroutines.call.*
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.atomic.AtomicBoolean


public fun <ReqT, RespT> CoroutineScope.serverCallUnary(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend () -> RespT
) {
    with(newRpcScope(coroutineContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(responseObserver as ServerCallStreamObserver<*>)
        launch {
            responseObserver.handleUnaryRpc { block() }
        }
    }
}

public fun <ReqT, RespT> CoroutineScope.serverCallServerStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>

    with(newRpcScope(coroutineContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val responseChannel = newSendChannelFromObserver(responseObserver)

        launch {
            responseChannel.handleStreamingRpc { block(it) }
        }.invokeOnCompletion(responseChannel.abandonedRpcHandler)
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <ReqT, RespT> CoroutineScope.serverCallClientStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>) -> RespT
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { enableManualFlowControl(requestChannelDelegate, isMessagePreloaded) }

    with(newRpcScope(coroutineContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val requestChannel = ServerRequestStreamChannel(
            coroutineContext = coroutineContext,
            delegateChannel = requestChannelDelegate,
            callStreamObserver = serverCallObserver,
            isMessagePreloaded = isMessagePreloaded,
            onErrorHandler = {
                this@rpcScope.cancel()
                responseObserver.onError(it.toRpcException())
            }
        )

        launch {
            responseObserver.handleUnaryRpc { block(requestChannel) }
        }

        return requestChannel
    }
}


@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <ReqT, RespT> CoroutineScope.serverCallBidiStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)

    with(newRpcScope(coroutineContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val responseChannel = newManagedServerResponseChannel(
            responseObserver = serverCallObserver,
            requestChannel = requestChannelDelegate,
            isMessagePreloaded = isMessagePreloaded
        )
        val requestChannel = ServerRequestStreamChannel(
            coroutineContext = coroutineContext,
            delegateChannel = requestChannelDelegate,
            callStreamObserver = serverCallObserver,
            isMessagePreloaded = isMessagePreloaded,
            onErrorHandler = {
                // In the event of a request error, we
                // need to close the responseChannel before
                // cancelling the rpcScope.
                responseChannel.close(it)
                this@rpcScope.cancel()
            }
        )

        launch {
            handleBidiStreamingRpc(requestChannel, responseChannel){ req, resp -> block(req,resp) }
        }.invokeOnCompletion(responseChannel.abandonedRpcHandler)

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
