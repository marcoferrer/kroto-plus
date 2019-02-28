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