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

import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import io.grpc.MethodDescriptor

class ProtoMethod(
    override val descriptorProto: DescriptorProtos.MethodDescriptorProto,
    val protoService: ProtoService
) : Schema.DescriptorWrapper {

    val functionName = descriptorProto.name.decapitalize()

    val methodDefinitionGetterName = "get${descriptorProto.name}Method"

    val requestType = protoService.protoFile.schema.protoTypes[descriptorProto.inputType]
        ?: throw IllegalStateException("${descriptorProto.inputType} was not found in schema type map.")

    val requestClassName = requestType.className

    val responseType = protoService.protoFile.schema.protoTypes[descriptorProto.outputType]
        ?: throw IllegalStateException("${descriptorProto.inputType} was not found in schema type map.")

    val responseClassName = responseType.className

    val isEmptyInput get() = descriptorProto.inputType == ".google.protobuf.Empty"

    val isNotEmptyInput get() = !isEmptyInput

    val isUnary get() = !descriptorProto.clientStreaming && !descriptorProto.serverStreaming

    val isBidi get() = descriptorProto.clientStreaming && descriptorProto.serverStreaming

    val isServerStream get() = !descriptorProto.clientStreaming && descriptorProto.serverStreaming

    val isClientStream get() = descriptorProto.clientStreaming && !descriptorProto.serverStreaming

    val type: MethodDescriptor.MethodType
        get() = when{
            isUnary -> MethodDescriptor.MethodType.UNARY
            isBidi ->  MethodDescriptor.MethodType.BIDI_STREAMING
            isServerStream ->  MethodDescriptor.MethodType.SERVER_STREAMING
            isClientStream ->  MethodDescriptor.MethodType.CLIENT_STREAMING
            else -> throw IllegalStateException("Unknown method type")
        }
}