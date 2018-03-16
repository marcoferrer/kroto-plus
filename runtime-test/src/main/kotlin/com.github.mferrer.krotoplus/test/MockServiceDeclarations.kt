package com.github.mferrer.krotoplus.test

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule


class ServiceBindingServerRule(vararg services: BindableService) : GrpcServerRule() {

    private val services: List<BindableService> = services.toList()

    override fun before() {
        super.before()
        for(service in services)
            serviceRegistry?.addService(service)
    }
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
