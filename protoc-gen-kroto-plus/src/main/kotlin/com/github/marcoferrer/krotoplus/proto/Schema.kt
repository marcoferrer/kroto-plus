/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.compiler.PluginProtos


class Schema(generatorRequest: PluginProtos.CodeGeneratorRequest) {

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

