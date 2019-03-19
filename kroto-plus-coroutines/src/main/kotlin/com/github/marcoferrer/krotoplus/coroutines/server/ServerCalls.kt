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


public fun <ReqT, RespT> ServiceScope.serverCallUnary(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend () -> RespT
) {
    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(responseObserver as ServerCallStreamObserver<*>)
        launch {
            responseObserver.handleUnaryRpc { block() }
        }
    }
}

public fun <ReqT, RespT> ServiceScope.serverCallServerStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val responseChannel = newSendChannelFromObserver(responseObserver, capacity = 0)

        launch {
            responseChannel.handleStreamingRpc { block(it) }
        }
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <ReqT, RespT> ServiceScope.serverCallClientStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>) -> RespT
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { enableManualFlowControl(requestChannelDelegate, isMessagePreloaded) }

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val requestChannel = ServerRequestStreamChannel(
            coroutineContext = coroutineContext,
            delegateChannel = requestChannelDelegate,
            callStreamObserver = serverCallObserver,
            isMessagePreloaded = isMessagePreloaded,
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

        launch {
            responseObserver.handleUnaryRpc { block(requestChannel) }
            // If the request channel was abandoned but we completed successfully
            // close it and clear its contents.
            if(!requestChannel.isClosedForReceive){
                requestChannel.cancel()
            }
        }

        return requestChannel
    }
}


@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <ReqT, RespT> ServiceScope.serverCallBidiStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
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
                // Call cancellation already cancels the coroutine scope
                // and closes the response stream. So we dont need to
                // do anything in this case.
                if(!serverCallObserver.isCancelled) {
                    // In the event of a request error, we
                    // need to close the responseChannel before
                    // cancelling the rpcScope.
                    responseChannel.close(it)
                    this@rpcScope.cancel()
                }
            }
        )

        launch {
            handleBidiStreamingRpc(requestChannel, responseChannel){ req, resp -> block(req,resp) }
            // If the request channel was abandoned but we completed successfully
            // close it and clear its contents.
            if(!requestChannel.isClosedForReceive){
                requestChannel.cancel()
            }
        }

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
