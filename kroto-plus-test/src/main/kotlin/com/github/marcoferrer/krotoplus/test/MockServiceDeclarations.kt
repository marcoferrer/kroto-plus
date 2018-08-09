package com.github.marcoferrer.krotoplus.test

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule


class ServiceBindingServerRule(
        private val services: List<BindableService>
) : GrpcServerRule() {

    constructor(vararg services: BindableService) : this(services.toList())

    override fun before() {
        super.before()
        for(service in services)
            serviceRegistry?.addService(service)
    }

}

interface MockServiceHelper {

    fun clearQueues()
}

fun <T> handleUnaryCall(responseObserver: StreamObserver<T>, queue: ResponseQueue<T>, fallback: T){

    val response = queue.poll()?.let{
        when(it){
            is QueueMessage -> it.value
            is QueueError -> {
                responseObserver.onError(it.asException())
                return
            }
        }
    }

    responseObserver.run {
        onNext(response ?: fallback)
        onCompleted()
    }
}
