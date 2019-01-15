package com.github.marcoferrer.krotoplus.coroutines

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
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launch a [Job] within a [ProducerScope] using the supplied channel as the Receiver.
 * This is useful for emulating the behavior of [CoroutineScope.produce] using an existing
 * channel. The supplied channel is then closed upon completion of the newly created Job.
 *
 * @param channel The channel that will be used as receiver of the [ProducerScope]
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param block the coroutine code which will be invoked in the context of the [ProducerScope].
 *
 * @return [Job] Returns a handle to the [Job] that is executing the [ProducerScope] block
 */
@ExperimentalCoroutinesApi
public suspend fun <T> CoroutineScope.launchProducerJob(
    channel: SendChannel<T>,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend ProducerScope<T>.()->Unit
): Job =
    launch(context) { newProducerScope(channel).block() }
        .apply { invokeOnCompletion(channel.completionHandler) }


@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@KrotoPlusInternalApi
fun <RespT> CoroutineScope.newSendChannelFromObserver(
    observer: StreamObserver<RespT>,
    capacity: Int = 1
): SendChannel<RespT> =
    CoroutineScope(coroutineContext + Dispatchers.Unconfined )
        .actor<RespT>(capacity = capacity, start = CoroutineStart.LAZY) {
            consumeEach { observer.onNext(it) }
        }
        .apply { invokeOnClose(observer.completionHandler) }

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal fun <ReqT, RespT> CoroutineScope.newManagedServerResponseChannel(
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

internal val StreamObserver<*>.completionHandler: CompletionHandler
    get() = {
        if(it != null)
            onError(it.toRpcException()) else
            onCompleted()
    }

internal val SendChannel<*>.completionHandler: CompletionHandler
    get() = {
        close(it?.toRpcException())
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
