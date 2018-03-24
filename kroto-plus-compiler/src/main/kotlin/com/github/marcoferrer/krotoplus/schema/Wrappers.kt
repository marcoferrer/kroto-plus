package com.github.marcoferrer.krotoplus.schema

import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Service

data class ServiceWrapper(val service: Service, val protoFile: ProtoFile, val protoSchema: Schema) {

    val name: String get() = service.name()

    val enclosingServiceClassName = ClassName(protoFile.outputPackage(), "${name}Grpc")

    val asyncStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}Stub")

    val futureStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}FutureStub")

    val blockingStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}BlockingStub")

    val methodDefinitions = service.rpcs().map { MethodWrapper(it) }

    inner class MethodWrapper(val method: Rpc) {

        val functionName = method.name().decapitalize()

        val serviceWrapper: ServiceWrapper get() = this@ServiceWrapper

        val requestClassName = method.requestType().toClassName(serviceWrapper.protoSchema)

        val responseClassName = method.responseType().toClassName(serviceWrapper.protoSchema)

    }
}