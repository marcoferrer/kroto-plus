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

package com.github.marcoferrer.krotoplus.generators.builders

import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.proto.Schema
import com.github.marcoferrer.krotoplus.proto.getFieldClassName
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.addForEach
import com.github.marcoferrer.krotoplus.utils.toUpperCamelCase
import com.github.marcoferrer.krotoplus.utils.toUpperSnakeCase
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal const val serviceDelegateName = "ServiceDelegate"

internal val ProtoService.outerObjectName: String
    get() = "${name}CoroutineGrpc"

internal val ProtoService.baseImplName: String
    get() = "${name}ImplBase"

internal val ProtoService.serviceDelegateClassName: ClassName
    get() = ClassName(protoFile.javaPackage, outerObjectName, baseImplName, serviceDelegateName)

internal val ProtoService.serviceJavaBaseImplClassName: ClassName
    get() = enclosingServiceClassName.nestedClass("${name}ImplBase")

internal val ProtoService.stubName: String
    get() = "${name}CoroutineStub"

internal val ProtoService.stubClassName: ClassName
    get() = ClassName(protoFile.javaPackage, outerObjectName, stubName)

internal val ProtoMethod.idPropertyName: String
    get() = "METHODID_${name.toUpperSnakeCase()}"

internal fun FunSpec.Builder.addResponseObserverParamerter(responseClassName: ClassName): FunSpec.Builder = apply {
    addParameter("responseObserver", CommonClassNames.streamObserver.parameterizedBy(responseClassName))
}

internal fun FunSpec.Builder.addMethodSignatureParamerter(
    methodSignatureFields: List<DescriptorProtos.FieldDescriptorProto>,
    schema: Schema
): FunSpec.Builder = apply {
    addForEach(methodSignatureFields){
        addParameter(it.name.toUpperCamelCase().decapitalize(), it.getFieldClassName(schema))
    }
}