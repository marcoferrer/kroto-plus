package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach

fun MethodDescriptor<*,*>.getUnimplementedException(): StatusRuntimeException =
    Status.UNIMPLEMENTED
        .withDescription("Method $fullMethodName is unimplemented")
        .asRuntimeException()

fun serverCallUnimplementedUnary(
    methodDescriptor: MethodDescriptor<*, *>,
    completableResponse: CompletableDeferred<*>
) {
    completableResponse.completeExceptionally(methodDescriptor.getUnimplementedException())
}

fun serverCallUnimplementedStream(methodDescriptor: MethodDescriptor<*, *>, responseChannel: SendChannel<*>) {
    responseChannel.close(methodDescriptor.getUnimplementedException())
}

fun <ReqT, RespT> CoroutineScope.serverCallClientStreaming(
    responseObserver: StreamObserver<RespT>,
    block: suspend (ReceiveChannel<ReqT>, CompletableDeferred<RespT>)-> Unit
): StreamObserver<ReqT> {

    val requestChannel = InboundStreamChannel<ReqT>()

    launch(GrpcContextElement()){
        val completableResponse = CompletableDeferredResponse(responseObserver)
        block(requestChannel,completableResponse)
    }

    return requestChannel
}

@ObsoleteCoroutinesApi
fun <RespT> CoroutineScope.serverCallServerStreaming(
    responseObserver: StreamObserver<RespT>,
    block: suspend (SendChannel<RespT>) -> Unit
) {
    launch(GrpcContextElement()){
        val responseChannel = actor<RespT>(start = CoroutineStart.LAZY) {
            try {
                consumeEach { responseObserver.onNext(it) }
                responseObserver.onCompleted()
            } catch (e: Throwable) {
                responseObserver.onError(e)
            }
        }

        block(responseChannel)
    }
}

fun <RespT> CoroutineScope.serverCallUnary(
    responseObserver: StreamObserver<RespT>,
    block: suspend (CompletableDeferred<RespT>)-> Unit
) {
    launch(GrpcContextElement()) {
        responseObserver.toCompletableDeferred().handleRequest { completableResponse ->
            block(completableResponse)
        }
    }
}