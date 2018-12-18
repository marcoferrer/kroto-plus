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


public fun <RespT> CoroutineScope.serverCallUnary(
    responseObserver: StreamObserver<RespT>,
    block: suspend (CompletableDeferred<RespT>) -> Unit
) {
    launch(GrpcContextElement()) {
        val completableResponse = responseObserver.toCompletableDeferred()
        try {
            block(completableResponse)
        } catch (e: Throwable) {
            completableResponse.completeExceptionally(e)
        }
    }
}

@ObsoleteCoroutinesApi
public fun <RespT> CoroutineScope.serverCallServerStreaming(
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    launch(GrpcContextElement()) {
        val responseChannel = newSendChannelFromObserver(responseObserver)
        try {
            block(responseChannel)
        } catch (e: Throwable) {
            responseChannel.close(e)
        }
    }
}

@ExperimentalCoroutinesApi
fun <ReqT, RespT> CoroutineScope.serverCallClientStreaming(
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, CompletableDeferred<RespT>) -> Unit
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)

    val requestChannelDelegate = Channel<ReqT>(capacity = 1)

    val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
        .apply { enableManualFlowControl(requestChannelDelegate, isMessagePreloaded) }

    val completableResponse = responseObserver.toCompletableDeferred()

    val requestChannel = ServerRequestStreamChannel(
        coroutineContext = coroutineContext,
        delegateChannel = requestChannelDelegate,
        callStreamObserver = serverCallObserver,
        isMessagePreloaded = isMessagePreloaded,
        onErrorHandler = {
            completableResponse.completeExceptionally(it)
        }
    )

    launch(GrpcContextElement()) {
        try {
            block(requestChannel, completableResponse)
        }catch (e: Throwable){
            completableResponse.completeExceptionally(e)
        }
    }

    return requestChannel
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
public fun <ReqT, RespT> CoroutineScope.serverCallBidiStreaming(
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, SendChannel<RespT>) -> Unit
): StreamObserver<ReqT> {

    val isMessagePreloaded = AtomicBoolean(false)

    val serverCallObserver = responseObserver as ServerCallStreamObserver<RespT>

    val requestChannelDelegate = Channel<ReqT>(capacity = 1)

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
            responseChannel.close(it)
        }
    )

    launch(GrpcContextElement()) {
        try {
            block(requestChannel, responseChannel)
        } catch (e: Throwable) {
            responseChannel.close(e)
        }
    }

    return requestChannel
}


private fun MethodDescriptor<*, *>.getUnimplementedException(): StatusRuntimeException =
    Status.UNIMPLEMENTED
        .withDescription("Method $fullMethodName is unimplemented")
        .asRuntimeException()

public fun serverCallUnimplementedUnary(
    methodDescriptor: MethodDescriptor<*, *>,
    completableResponse: CompletableDeferred<*>
) {
    completableResponse.completeExceptionally(methodDescriptor.getUnimplementedException())
}

public fun serverCallUnimplementedStream(methodDescriptor: MethodDescriptor<*, *>, responseChannel: SendChannel<*>) {
    responseChannel.close(methodDescriptor.getUnimplementedException())
}
