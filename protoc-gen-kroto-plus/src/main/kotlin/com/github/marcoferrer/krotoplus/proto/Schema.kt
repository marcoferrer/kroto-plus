package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.compiler.PluginProtos


class Schema(generatorRequest: PluginProtos.CodeGeneratorRequest){

    val protoFiles: List<ProtoFile> = generatorRequest.protoFileList.map { fileDescriptor ->
        ProtoFile(fileDescriptor, this)
    }

    //Key by TypeName which is prefixed with a '.'
    val protoTypes: Map<String, ProtoType> = protoFiles.asSequence().flatMap { protoFile ->
        val protoMessages = protoFile.protoMessages
                .asSequence()
                .flattenProtoTypes()
        val protoEnums = protoFile.protoEnums
                .asSequence()
                .flattenProtoTypes()

        protoMessages + protoEnums
    }.associateBy { ".${it.canonicalProtoName}" }

    val protoServices: List<ProtoService> = protoFiles.flatMap { it.services }

    interface DescriptorWrapper {
        val descriptorProto: GeneratedMessageV3
    }
}

