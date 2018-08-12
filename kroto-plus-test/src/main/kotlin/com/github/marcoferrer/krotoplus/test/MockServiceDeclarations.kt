package com.github.marcoferrer.krotoplus.test

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.grpc.util.MutableHandlerRegistry
import org.junit.rules.ExternalResource

@Deprecated("removed due to change in 'io.grpc:grpc-testing' api", ReplaceWith("GrpcServerRule()"),DeprecationLevel.HIDDEN)
class ServiceBindingServerRule(
        private val services: List<BindableService>
) : ExternalResource() {
    constructor(vararg services: BindableService) : this(services.toList())
}

fun MutableHandlerRegistry.addServices(services: List<BindableService>) {
    for (service in services) addService(service)
}

interface MockServiceHelper {

    fun clearQueues()
}

fun <T> handleUnaryCall(responseObserver: StreamObserver<T>, queue: ResponseQueue<T>, fallback: T) {

    val response = queue.poll()?.let {
        when (it) {
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
