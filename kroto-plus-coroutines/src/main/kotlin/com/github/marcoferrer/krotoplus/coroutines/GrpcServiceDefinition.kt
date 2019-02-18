package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.ServiceDescriptor

private interface GrpcServiceDefinition<T> {

    public val serviceDescriptor: ServiceDescriptor

    public val methodDescriptors: List<MethodDescriptor<*,*>>

    public fun newStub(channel: Channel): T
}