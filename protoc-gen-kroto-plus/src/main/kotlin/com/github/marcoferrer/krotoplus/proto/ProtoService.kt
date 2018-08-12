package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.ClassName


data class ProtoService(
        override val descriptorProto: DescriptorProtos.ServiceDescriptorProto,
        val protoFile: ProtoFile
) : Schema.DescriptorWrapper {

    val name: String get() = descriptorProto.name

    val enclosingServiceClassName = ClassName(protoFile.javaPackage, "${name}Grpc")

    val asyncStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}Stub")

    val futureStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}FutureStub")

    val blockingStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}BlockingStub")

    val methodDefinitions by lazy { descriptorProto.methodList.map { ProtoMethod(it, this@ProtoService) } }

}