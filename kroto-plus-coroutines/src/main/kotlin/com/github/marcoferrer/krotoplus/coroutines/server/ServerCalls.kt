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
import io.grpc.ForwardingServerCall
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


public fun <ReqT, RespT> ServiceScope.serverCallUnary(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend () -> RespT
) {
    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(responseObserver as ServerCallStreamObserver<*>)
        launch {
            try{
                responseObserver.onNext(block())
                responseObserver.onCompleted()
            }catch (e: Throwable){
                responseObserver.completeSafely(e)
            }
        }
    }
}

public fun <ReqT, RespT> ServiceScope.serverCallServerStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    val responseChannel = Channel<RespT>(capacity = 0)
    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>
    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)
        applyOutboundFlowControl(serverCallObserver,responseChannel)
        launch {
            try{
                block(responseChannel)
                responseChannel.close()
            }catch (e: Throwable){
                val rpcError = e.toRpcException()
                serverCallObserver.completeSafely(rpcError)
                responseChannel.close(rpcError)
            }
        }

        // We attach to the parent job because we want
        // to make sure all children complete including
        // any scheduled outbound producers
        coroutineContext[Job]?.invokeOnCompletion {
            serverCallObserver.completeSafely(it)
        }
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
public fun <ReqT, RespT> ServiceScope.serverCallClientStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>) -> RespT
): StreamObserver<ReqT> {

    val activeInboundJobCount = AtomicInteger()
    val inboundChannel = Channel<ReqT>(capacity = 0)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { applyInboundFlowControl(inboundChannel, activeInboundJobCount) }

    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)

        val requestChannel = ServerRequestStreamChannel(
            coroutineContext = coroutineContext,
            inboundChannel = inboundChannel,
            activeInboundJobCount = activeInboundJobCount,
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

        launch {
            try{
                responseObserver.onNext(block(requestChannel))
                responseObserver.onCompleted()
            }catch (e: Throwable){
                responseObserver.completeSafely(e)
            }
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

    val responseChannel = Channel<RespT>(capacity = 0)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
    with(newRpcScope(initialContext, methodDescriptor)) rpcScope@ {
        bindToClientCancellation(serverCallObserver)
        applyOutboundFlowControl(serverCallObserver,responseChannel)
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

        launch {
            serverCallObserver.request(1)
            try{
                block(requestChannel,responseChannel)
                responseChannel.close()
            }catch (e:Throwable){
                val rpcError = e.toRpcException()
                serverCallObserver.completeSafely(rpcError)
                responseChannel.close(rpcError)
            }
            if(!requestChannel.isClosedForReceive){
                requestChannel.cancel()
            }
        }

        // We attach to the parent job because we want
        // to make sure all children complete including
        // any scheduled outbound producers
        coroutineContext[Job]?.invokeOnCompletion {
            serverCallObserver.completeSafely(it)
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
