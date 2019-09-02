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

import com.github.marcoferrer.krotoplus.generators.GeneratorContext
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName

/**
 * This builder is meant to eventually replace the usage of a service delegate
 */
class GrpcMethodHandlerBuilder(val context: GeneratorContext) {

    fun buildMethodIdConsts(protoService: ProtoService): List<PropertySpec> = with(protoService) {
        methodDefinitions.mapIndexed { index, method ->
            PropertySpec.builder(
                method.idPropertyName,
                Int::class.asClassName()
            )
                .addModifiers(KModifier.PRIVATE, KModifier.CONST)
                .initializer("%L", index)
                .build()
        }
    }

    fun buildMethodHandler(protoService: ProtoService): TypeSpec = with(protoService) {
        val reqTypeVarName = TypeVariableName("Req")
        val respTypeVarName = TypeVariableName("Resp")

        val serviceImplClassName = ClassName(protoFile.javaPackage, outerObjectName, baseImplName)

        TypeSpec.classBuilder("MessageHandler")
            .addModifiers(KModifier.PRIVATE)
            .addTypeVariable(reqTypeVarName)
            .addTypeVariable(respTypeVarName)
            .addSuperinterface(
                CommonClassNames.GrpcServerCallHandler.unary
                    .parameterizedBy(reqTypeVarName, respTypeVarName)
            )
            .addSuperinterface(
                CommonClassNames.GrpcServerCallHandler.serverStreaming
                    .parameterizedBy(reqTypeVarName, respTypeVarName)
            )
            .addSuperinterface(
                CommonClassNames.GrpcServerCallHandler.clientStreaming
                    .parameterizedBy(reqTypeVarName, respTypeVarName)
            )
            .addSuperinterface(
                CommonClassNames.GrpcServerCallHandler.bidiStreaming
                    .parameterizedBy(reqTypeVarName, respTypeVarName)
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(
                        ParameterSpec.builder("serviceImpl", serviceImplClassName)
                            .addModifiers(KModifier.PRIVATE)
                            .addAnnotation(JvmField::class.java.asClassName())
                            .build()
                    )
                    .addParameter(
                        ParameterSpec.builder("methodId", Int::class.asClassName())
                            .addModifiers(KModifier.PRIVATE)
                            .addAnnotation(JvmField::class.java.asClassName())
                            .build()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("invoke")
                    .addParameter("request", reqTypeVarName)
                    .addParameter(
                        "responseObserver",
                        CommonClassNames.streamObserver.parameterizedBy(respTypeVarName)
                    )
                    .apply {
                        // TODO: Build invoke impl for unary / server streaming
                        // TODO: Build invoke impl for client / bidi streaming
                    }
                    .build()
            )
            .build()
    }
}