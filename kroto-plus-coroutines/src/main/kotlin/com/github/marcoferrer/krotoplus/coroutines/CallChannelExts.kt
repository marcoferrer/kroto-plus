package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import java.util.concurrent.atomic.AtomicBoolean


@ObsoleteCoroutinesApi
fun <RespT> CoroutineScope.newSendChannelFromObserver(
    responseObserver: StreamObserver<RespT>,
    capacity: Int = 1
): SendChannel<RespT> =
    actor(capacity = capacity, start = CoroutineStart.LAZY) {
        try {
            consumeEach { responseObserver.onNext(it) }
            responseObserver.onCompleted()
        } catch (e: Throwable) {
            responseObserver.onError(e)
        }
    }

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun <ReqT, RespT> CoroutineScope.newManagedServerResponseChannel(
    responseObserver: ServerCallStreamObserver<RespT>,
    isMessagePreloaded: AtomicBoolean,
    requestChannel: Channel<ReqT> = Channel()
): SendChannel<RespT> {

    val responseChannel = newSendChannelFromObserver(responseObserver)

    responseObserver.apply {
        enableManualFlowControl(requestChannel,isMessagePreloaded)
        setOnCancelHandler {
            responseChannel.close(Status.CANCELLED.asRuntimeException())
        }
    }

    return responseChannel
}