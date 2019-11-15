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
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock


data class ProtoService(
    override val descriptorProto: DescriptorProtos.ServiceDescriptorProto,
    val sourceLocation: Location,
    val protoFile: ProtoFile
) : Schema.DescriptorWrapper {

    val name: String get() = descriptorProto.name

    val attachedComments: String = sourceLocation.buildAttachedComments()

    val enclosingServiceClassName = ClassName(protoFile.javaPackage, "${name}Grpc")

    val asyncStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}Stub")

    val futureStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}FutureStub")

    val blockingStubClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}BlockingStub")

    val methodDefinitions by lazy {
        descriptorProto.methodList.mapIndexed { methodIndex, methodDescriptor ->
            val sourceLocation = this@ProtoService.protoFile.descriptorProto.sourceCodeInfo.locationList
                .findByMethodIndex(methodIndex)
            ProtoMethod(methodDescriptor, sourceLocation, this@ProtoService)
        }
    }

}

/**
 * Comment parsing is based on the following implementation
 * https://github.com/salesforce/reactive-grpc/blob/ab8e86e1d951f3a7ab9802a31ab73cd5ba3b8e3b/common/reactive-grpc-gencommon/src/main/java/com/salesforce/reactivegrpc/gen/ReactiveGrpcGenerator.java#L89
 */
private const val SERVICE_NUMBER_OF_PATHS = 2

fun List<Location>.findByServiceIndex(serviceIndex: Int): Location {

    val allServiceLocations = filter {
        it.pathCount >= 2 &&
                it.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                it.getPath(1) == serviceIndex
    }

    return allServiceLocations
        .find { it.pathCount == SERVICE_NUMBER_OF_PATHS } ?: Location.getDefaultInstance()
}

