package com.github.marcoferrer.krotoplus.schema

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.ClassName

class Schema(generatorRequest: PluginProtos.CodeGeneratorRequest){

    val types: Map<String, ProtoType> =
            generatorRequest
                    .getAllProtoTypes()
                    .associateBy { ".${it.cannonicalProtoName}" }


    val typesByDescriptor: Map<GeneratedMessageV3, ProtoType> =
            types.entries
                    .associate { (_, value) ->
                        value.descriptorProto to value
                    }

//    val types: Map<String, ProtoType> = mutableMapOf<String, ProtoType>().also { map ->
//
//        for(fileDescriptor in generatorRequest.protoFileList){
//
//            for(messageDescriptor in fileDescriptor.messageTypeList) {
//                ProtoMessage(messageDescriptor, fileDescriptor)
//                        .registerByName(map)
//            }
//
//            for(enumDescriptor in fileDescriptor.enumTypeList) {
//                ProtoEnum(enumDescriptor, fileDescriptor)
//                        .registerByName(map)
//            }
//        }
//    }
//
//    val typesByDescriptor: Map<GeneratedMessageV3, ProtoType> = types.entries.associate { (_, value) ->
//        value.descriptorProto to value
//    }


    val services = generatorRequest.protoFileList.flatMap { fileDescriptorProto ->

        fileDescriptorProto.serviceList.map { serviceDescriptor ->
            Service(
                    service = serviceDescriptor,
                    protoFile = fileDescriptorProto,
                    schema = this@Schema
            )
        }
    }

}


data class Service(
        val service: DescriptorProtos.ServiceDescriptorProto,
        val protoFile: DescriptorProtos.FileDescriptorProto,
        val schema: Schema
) {

    val name: String get() = service.name

    val enclosingServiceClassName = ClassName(protoFile.javaPackage, "${name}Grpc")

    val asyncStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}Stub")

    val futureStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}FutureStub")

    val blockingStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}BlockingStub")

    val methodDefinitions = service.methodList.map { Method(it) }

    inner class Method(val method: DescriptorProtos.MethodDescriptorProto) {

        val functionName = method.name.decapitalize()

        val service: Service get() = this@Service

        val requestType = service.schema.types[method.inputType]
                ?: throw IllegalStateException("${method.inputType} was not found in schema type map.")

        val requestClassName = requestType.className

        val responseType = service.schema.types[method.outputType]
                ?: throw IllegalStateException("${method.inputType} was not found in schema type map.")

        val responseClassName = responseType.className

        val isUnary get() = !method.clientStreaming && !method.serverStreaming

        val isBidi get() = method.clientStreaming && method.serverStreaming

        val isServerStream get() = !method.clientStreaming && method.serverStreaming

        val isClientStream get() = method.clientStreaming && !method.serverStreaming

    }
}

