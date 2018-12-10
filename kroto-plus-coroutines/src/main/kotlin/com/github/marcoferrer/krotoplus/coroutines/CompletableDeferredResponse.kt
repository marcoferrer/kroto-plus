package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableDeferred

class CompletableDeferredResponse<RespT>(
    private val responseObserver: StreamObserver<RespT>,
    private val delegate: CompletableDeferred<RespT> = CompletableDeferred()
) : CompletableDeferred<RespT> by delegate {

    override fun complete(value: RespT): Boolean =
        delegate.complete(value).also { isCompleted ->
            if(isCompleted) responseObserver.apply {
                onNext(value)
                onCompleted()
            }
        }

    override fun completeExceptionally(exception: Throwable): Boolean =
        delegate.completeExceptionally(exception).also { isCompleted ->
            if(isCompleted){
                responseObserver.onError(exception)
            }
        }
}

fun <RespT> StreamObserver<RespT>.toCompletableDeferred(): CompletableDeferredResponse<RespT> =
    CompletableDeferredResponse(responseObserver = this)

inline fun <RespT> CompletableDeferredResponse<RespT>.handleRequest(block: (CompletableDeferred<RespT>)->Unit){
    try{
        block(this)
    }catch (t: Throwable){
        completeExceptionally(t)
    }
}

