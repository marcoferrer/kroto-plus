package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.*
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
    newRpcScope(coroutineContext, methodDescriptor)
        .launch { responseObserver.onNext(block()) }
        .invokeOnCompletion(responseObserver.completionHandler)
}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
public fun <ReqT, RespT> CoroutineScope.serverCallServerStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    val rpcScope = newRpcScope(coroutineContext, methodDescriptor)

    val responseChannel = newSendChannelFromObserver(responseObserver)

    rpcScope.launch { block(responseChannel) }
        .invokeOnCompletion {
            responseChannel.close(it?.toRpcException())
        }
}

@ExperimentalCoroutinesApi
public fun <ReqT, RespT> CoroutineScope.serverCallClientStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>) -> RespT
): StreamObserver<ReqT> {

    val rpcScope = newRpcScope(coroutineContext, methodDescriptor)

    val isMessagePreloaded = AtomicBoolean(false)
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { enableManualFlowControl(requestChannelDelegate, isMessagePreloaded) }

    val requestChannel = ServerRequestStreamChannel(
        coroutineContext = rpcScope.coroutineContext,
        delegateChannel = requestChannelDelegate,
        callStreamObserver = serverCallObserver,
        isMessagePreloaded = isMessagePreloaded,
        onErrorHandler = {
            rpcScope.cancel()
            responseObserver.onError(it.toRpcException())
        }
    )

    rpcScope
        .launch { responseObserver.onNext(block(requestChannel)) }
        .invokeOnCompletion(responseObserver.completionHandler)

    return requestChannel
}


@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
public fun <ReqT, RespT> CoroutineScope.serverCallBidiStreaming(
    methodDescriptor: MethodDescriptor<ReqT, RespT>,
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
): StreamObserver<ReqT> {

    val rpcScope = newRpcScope(coroutineContext, methodDescriptor)

    val isMessagePreloaded = AtomicBoolean(false)
    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>
    val requestChannelDelegate = Channel<ReqT>(capacity = 1)
    val responseChannel = rpcScope.newManagedServerResponseChannel(
        responseObserver = serverCallObserver,
        requestChannel = requestChannelDelegate,
        isMessagePreloaded = isMessagePreloaded
    )
    val requestChannel = ServerRequestStreamChannel(
        coroutineContext = rpcScope.coroutineContext,
        delegateChannel = requestChannelDelegate,
        callStreamObserver = serverCallObserver,
        isMessagePreloaded = isMessagePreloaded,
        onErrorHandler = {
            // In the event of a request error, we
            // need to close the responseChannel before
            // cancelling the rpcScope.
            responseChannel.close(it)
            rpcScope.cancel()
        }
    )

    rpcScope
        .launch { block(requestChannel, responseChannel) }
        .invokeOnCompletion(responseChannel.completionHandler)

    return requestChannel
}

private fun MethodDescriptor<*, *>.getUnimplementedException(): StatusRuntimeException =
    Status.UNIMPLEMENTED
        .withDescription("Method $fullMethodName is unimplemented")
        .asRuntimeException()

public fun <T> serverCallUnimplementedUnary(
    methodDescriptor: MethodDescriptor<*, *>
): T {
    throw methodDescriptor.getUnimplementedException()
}

public fun serverCallUnimplementedStream(methodDescriptor: MethodDescriptor<*, *>, responseChannel: SendChannel<*>) {
    responseChannel.close(methodDescriptor.getUnimplementedException())
}
