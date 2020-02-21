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
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.CommonPackages
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName

/**
 * This builder is meant to eventually replace the usage of a service delegate
 */
class GrpcMethodHandlerBuilder(val context: GeneratorContext) {

    private val reqTypeVarName = TypeVariableName("Req")
    private val respTypeVarName = TypeVariableName("Resp")
    private val suppressUncheckedAnnotation = AnnotationSpec.builder(Suppress::class.asClassName())
        .addMember("\"UNCHECKED_CAST\"")
        .build()

    private val requestParameter = ParameterSpec.builder("request", reqTypeVarName).build()

    private val requestChannelParameter = ParameterSpec.builder(
        "requestChannel", CommonClassNames.receiveChannel.parameterizedBy(reqTypeVarName)).build()

    private val responseChannelParameter = ParameterSpec.builder(
        "responseChannel", CommonClassNames.sendChannel.parameterizedBy(respTypeVarName)).build()

    object CallHandlerClassNames {
        val unary = ClassName(CommonPackages.krotoCoroutineLib+".server","UnaryMethod")
        val serverStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","ServerStreamingMethod")
        val clientStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","ClientStreamingMethod")
        val bidiStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","BidiStreamingMethod")
    }

    fun buildMethodIdConsts(protoService: ProtoService): List<PropertySpec> = with(protoService) {
        methodDefinitions.map { method ->
            PropertySpec.builder(
                method.idPropertyName,
                Int::class.asClassName()
            )
                .addModifiers(KModifier.PRIVATE, KModifier.CONST)
                .initializer("%L", method.index)
                .build()
        }
    }

    private val ProtoService.serviceImplClassName: ClassName
        get() = ClassName(protoFile.javaPackage, outerObjectName, baseImplName)

    private fun baseInvokeImplBuilder(): FunSpec.Builder =
        FunSpec.builder("invoke")
            .addAnnotation(suppressUncheckedAnnotation)
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND, KModifier.OPERATOR)

    private fun buildUnaryInvokeImpl(methods: List<ProtoMethod>): FunSpec =
        baseInvokeImplBuilder()
            .addParameter(requestParameter)
            .returns(respTypeVarName)
            .addCode(CodeBlock.builder()
                .beginControlFlow("return when(methodId)")
                .apply {
                    methods.forEach { method ->
                        addStatement(
                            "%N -> serviceImpl.%N(request as %T) as %T",
                            method.idPropertyName,
                            method.functionName,
                            method.requestClassName,
                            respTypeVarName
                        )
                        addStatement("\n")
                    }
                }
                .addStatement("else -> throw %T()", AssertionError::class.asClassName())
                .endControlFlow()
                .build())
            .build()

    private fun buildClientStreamingInvokeImpl(methods: List<ProtoMethod>): FunSpec =
        baseInvokeImplBuilder()
            .addParameter(requestChannelParameter)
            .returns(respTypeVarName)
            .addCode(CodeBlock.builder()
                .beginControlFlow("return when(methodId)")
                .apply {
                    methods.forEach { method ->
                        addStatement(
                            "%N -> serviceImpl.%N(requestChannel as %T) as %T",
                            method.idPropertyName,
                            method.functionName,
                            CommonClassNames.receiveChannel.parameterizedBy(method.requestClassName),
                            respTypeVarName
                        )
                        addStatement("\n")
                    }
                }
                .addStatement("else -> throw %T()", AssertionError::class.asClassName())
                .endControlFlow()
                .build())
            .build()

    private fun buildServerStreamingInvokeImpl(methods: List<ProtoMethod>): FunSpec =
        baseInvokeImplBuilder()
            .addParameter(requestParameter)
            .addParameter(responseChannelParameter)
            .returns(UNIT)
            .addCode(CodeBlock.builder()
                .beginControlFlow("return when(methodId)")
                .apply {
                    methods.forEach { method ->
                        add(CodeBlock.of(
                            """
                            |%N -> serviceImpl.%N(
                            |   request as %T,
                            |   responseChannel as %T
                            |)
                            """.trimMargin(),
                            method.idPropertyName,
                            method.functionName,
                            method.requestClassName,
                            CommonClassNames.sendChannel.parameterizedBy(method.responseClassName)
                        ))
                        addStatement("\n")
                    }
                }
                .addStatement("else -> throw %T()", AssertionError::class.asClassName())
                .endControlFlow()
                .build())
            .build()

    private fun buildBidiStreamingInvokeImpl(methods: List<ProtoMethod>): FunSpec =
        baseInvokeImplBuilder()
            .addParameter(requestChannelParameter)
            .addParameter(responseChannelParameter)
            .returns(UNIT)
            .addCode(CodeBlock.builder()
                .beginControlFlow("return when(methodId)")
                .apply {
                    methods.forEach { method ->
                        add(CodeBlock.of(
                            """
                            |%N -> serviceImpl.%N(
                            |   requestChannel as %T,
                            |   responseChannel as %T
                            |)
                            """.trimMargin(),
                            method.idPropertyName,
                            method.functionName,
                            CommonClassNames.receiveChannel.parameterizedBy(method.requestClassName),
                            CommonClassNames.sendChannel.parameterizedBy(method.responseClassName)
                        ))
                        addStatement("\n")
                    }
                }
                .addStatement("else -> throw %T()", AssertionError::class.asClassName())
                .endControlFlow()
                .build())
            .build()

    fun buildMethodHandlersTypeSpec(protoService: ProtoService): TypeSpec = with(protoService) {

        val unaryMethods = methodDefinitions.filter { it.isUnary }
        val clientStreamingMethods = methodDefinitions.filter { it.isClientStream }
        val serverStreamingMethods = methodDefinitions.filter { it.isServerStream }
        val bidiStreamingMethods = methodDefinitions.filter { it.isBidi }

        TypeSpec.classBuilder("MethodHandlers")
            .addModifiers(KModifier.PRIVATE)
            .addTypeVariable(reqTypeVarName)
            .addTypeVariable(respTypeVarName)
            .apply {
                if(unaryMethods.isNotEmpty()){
                    addSuperinterface(
                        CallHandlerClassNames.unary
                            .parameterizedBy(reqTypeVarName, respTypeVarName)
                    )
                    addFunction(buildUnaryInvokeImpl(unaryMethods))
                }
            }
            .apply {
                if(clientStreamingMethods.isNotEmpty()){
                    addSuperinterface(
                        CallHandlerClassNames.clientStreaming
                            .parameterizedBy(reqTypeVarName, respTypeVarName)
                    )
                    addFunction(buildClientStreamingInvokeImpl(clientStreamingMethods))
                }
            }
            .apply {
                if(serverStreamingMethods.isNotEmpty()){
                    addSuperinterface(
                        CallHandlerClassNames.serverStreaming
                            .parameterizedBy(reqTypeVarName, respTypeVarName)
                    )
                    addFunction(buildServerStreamingInvokeImpl(serverStreamingMethods))
                }
            }
            .apply {
                if(bidiStreamingMethods.isNotEmpty()){
                    addSuperinterface(
                        CallHandlerClassNames.bidiStreaming
                            .parameterizedBy(reqTypeVarName, respTypeVarName)
                    )
                    addFunction(buildBidiStreamingInvokeImpl(bidiStreamingMethods))
                }
            }
            .addConstructorAndProps(protoService)
            .build()
    }

    private fun TypeSpec.Builder.addConstructorAndProps(protoService: ProtoService): TypeSpec.Builder {

        return primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("serviceImpl", protoService.serviceImplClassName)
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("methodId", Int::class.asClassName())
                        .build()
                )
                .build()
        )
            .addProperty(
                PropertySpec
                    .builder("serviceImpl", protoService.serviceImplClassName)
                    .addModifiers(KModifier.PRIVATE)
                    .addAnnotation(JvmField::class.java.asClassName())
                    .initializer("serviceImpl")
                    .build()
            )
            .addProperty(
                PropertySpec
                    .builder("methodId", Int::class.asClassName())
                    .addModifiers(KModifier.PRIVATE)
                    .addAnnotation(JvmField::class.java.asClassName())
                    .initializer("methodId")
                    .build()
            )
    }

}