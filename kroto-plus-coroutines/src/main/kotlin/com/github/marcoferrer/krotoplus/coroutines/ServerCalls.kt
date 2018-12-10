package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.CompletableDeferred

object ServerCalls {

    fun suspendingUnimplementedUnaryCall(
        methodDescriptor: MethodDescriptor<*, *>, deferredResponse: CompletableDeferred<*>
    ) {
        deferredResponse.completeExceptionally(
            Status.UNIMPLEMENTED
                .withDescription("Method ${methodDescriptor.fullMethodName} is unimplemented")
                .asRuntimeException()
        )
    }

}
