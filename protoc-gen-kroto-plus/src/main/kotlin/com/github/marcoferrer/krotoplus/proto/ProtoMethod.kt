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

import com.github.marcoferrer.krotoplus.utils.toUpperCamelCase
import com.google.api.ClientProto
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location
import com.squareup.kotlinpoet.CodeBlock
import io.grpc.MethodDescriptor

class ProtoMethod(
    override val descriptorProto: DescriptorProtos.MethodDescriptorProto,
    val sourceLocation: Location,
    val protoService: ProtoService
) : Schema.DescriptorWrapper {

    val name: String
        get() = descriptorProto.name

    val functionName = descriptorProto.name.toUpperCamelCase().let{
        if(descriptorProto.name.startsWith("_"))
            "_$it" else it.decapitalize()
    }

    val attachedComments: String = sourceLocation.buildAttachedComments()

    val methodDefinitionGetterName = "get${descriptorProto.name.toUpperCamelCase()}Method"

    val requestType: ProtoMessage = requireNotNull(protoService.protoFile.schema.protoTypes[descriptorProto.inputType] as? ProtoMessage){
        "${descriptorProto.inputType} was not found in schema type map, or was not or was not of type 'ProtoMessage'"
    }

    val requestClassName = requestType.className

    val responseType: ProtoMessage = requireNotNull(protoService.protoFile.schema.protoTypes[descriptorProto.outputType] as? ProtoMessage){
        "${descriptorProto.outputType} was not found in schema type map, or was not or was not of type 'ProtoMessage'"
    }

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

    val methodSignatureVariants: List<List<DescriptorProtos.FieldDescriptorProto>> = getMethodSignatureVariants()
}

private fun ProtoMethod.getMethodSignatureVariants(): List<List<DescriptorProtos.FieldDescriptorProto>> {
    descriptorProto.options
        .runCatching { getExtension(ClientProto.methodSignature) }
        .getOrNull()
        ?.takeUnless { it.isEmpty() }
        ?.let { signatureFields ->
            requestType.descriptorProto.fieldList
                .filter { it.name in signatureFields }
        }
        .orEmpty()

    val methodSignatures = descriptorProto.options
        .runCatching { getExtension(ClientProto.methodSignature) }
        .getOrNull()
        ?.takeUnless { it.isEmpty() }

    val signatureVariants = methodSignatures?.map { delimitedFields ->
        val signatureFields = delimitedFields.split(",")

        // Get descriptors for fields defined
        requestType.descriptorProto.fieldList
            .filter { it.name in signatureFields }
    }

    return signatureVariants.orEmpty()
}

/**
 * Comment parsing is based on the following implementation
 * https://github.com/salesforce/reactive-grpc/blob/ab8e86e1d951f3a7ab9802a31ab73cd5ba3b8e3b/common/reactive-grpc-gencommon/src/main/java/com/salesforce/reactivegrpc/gen/ReactiveGrpcGenerator.java#L133
 */
private const val METHOD_NUMBER_OF_PATHS = 4

fun List<Location>.findByMethodIndex(methodIndex: Int): Location = find {
    it.pathCount == METHOD_NUMBER_OF_PATHS && it.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodIndex
} ?: Location.getDefaultInstance()
