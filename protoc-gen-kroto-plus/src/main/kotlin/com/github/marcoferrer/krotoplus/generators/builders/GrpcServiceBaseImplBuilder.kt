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
import com.github.marcoferrer.krotoplus.utils.messageBuilderValueCodeBlock
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition

class GrpcServiceBaseImplBuilder(val context: GeneratorContext){

    fun build(protoService: ProtoService): TypeSpec = with(protoService){

        TypeSpec.classBuilder(baseImplName)
            .addKdoc(attachedComments)
            .addModifiers(KModifier.ABSTRACT)
            .addSuperinterface(CommonClassNames.bindableService)
            .addSuperinterface(CommonClassNames.serviceScope)
            .addFunction(buildBindServiceFunSpec(protoService))
            .apply {
                for(method in methodDefinitions) when(method.type){
                    MethodDescriptor.MethodType.UNARY -> addFunction(buildUnaryBaseImpl(method))
                    MethodDescriptor.MethodType.CLIENT_STREAMING -> addFunction(buildClientStreamingBaseImpl(method))
                    MethodDescriptor.MethodType.SERVER_STREAMING -> addFunction(buildServerStreamingBaseImpl(method))
                    MethodDescriptor.MethodType.BIDI_STREAMING -> addFunction(buildBidiStreamingBaseImpl(method))
                    MethodDescriptor.MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
                }
            }
            .addFunctions(buildResponseLambdaOverloads())
            .build()
    }

    private fun ProtoService.buildResponseLambdaOverloads(): List<FunSpec> =
        methodDefinitions
            .asSequence()
            .filter { it.isServerStream || it.isBidi }
            .distinctBy { it.responseType }
            .map { it.buildChannelLambdaExt() }
            .toList()

    private fun ProtoMethod.buildChannelLambdaExt(): FunSpec {

        val receiverClassName = CommonClassNames.sendChannel
            .parameterizedBy(responseClassName)

        val jvmNameSuffix = responseType.canonicalJavaName
            .replace(responseType.javaPackage.orEmpty(), "")
            .replace(".", "")

        return FunSpec.builder("send")
            .addModifiers(KModifier.INLINE, KModifier.SUSPEND)
            .receiver(receiverClassName)
            .addParameter(
                "block", LambdaTypeName.get(
                    receiver = responseType.builderClassName,
                    returnType = UNIT
                )
            )
            .addAnnotation(
                AnnotationSpec
                    .builder(JvmName::class.asClassName())
                    .addMember("\"send$jvmNameSuffix\"")
                    .build()
            )
            .addCode(
                CodeBlock.builder()
                    .add(messageBuilderValueCodeBlock(responseClassName, "response", "block"))
                    .addStatement("send(response)")
                    .build()
            )
            .build()
    }

    private fun buildUnaryBaseImpl(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addKdoc(attachedComments)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter("request", requestClassName)
            .returns(responseClassName)
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "return %T(%T.%N())",
                        CommonClassNames.ServerCalls.serverCallUnimplementedUnary,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()
    }

    private fun buildServerStreamingBaseImpl(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addKdoc(attachedComments)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter("request", requestClassName)
            .addParameter(
                name = "responseChannel",
                type = CommonClassNames.sendChannel.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%T(", CommonClassNames.ServerCalls.serverCallUnimplementedStream)
                    .indent()
                    .addStatement(
                        "%T.%N(),",
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    )
                    .addStatement("responseChannel")
                    .unindent()
                    .addStatement(")")
                    .build()
            )
            .build()
    }

    private fun buildClientStreamingBaseImpl(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addKdoc(attachedComments)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                name = "requestChannel",
                type = CommonClassNames.receiveChannel.parameterizedBy(requestClassName)
            )
            .returns(responseClassName)
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "return %T(%T.%N())",
                        CommonClassNames.ServerCalls.serverCallUnimplementedUnary,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()
    }

    private fun buildBidiStreamingBaseImpl(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addKdoc(attachedComments)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                name = "requestChannel",
                type = CommonClassNames.receiveChannel.parameterizedBy(requestClassName)
            )
            .addParameter(
                name = "responseChannel",
                type = CommonClassNames.sendChannel.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement("%T(", CommonClassNames.ServerCalls.serverCallUnimplementedStream)
                    .indent()
                    .addStatement(
                        "%T.%N(),",
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    )
                    .addStatement("responseChannel")
                    .unindent()
                    .addStatement(")")
                    .build()
            )
            .build()
    }

    private fun buildBindServiceFunSpec(protoService: ProtoService): FunSpec =
        FunSpec.builder("bindService")
            .addModifiers(KModifier.OVERRIDE)
            .returns(CommonClassNames.grpcServerServiceDefinition)
            .addCode(addMethodsToServiceCodeBlock(protoService))
            .build()

    private fun addMethodsToServiceCodeBlock(protoService: ProtoService): CodeBlock {
        val builder = CodeBlock.builder()
            .addStatement("val builder = %T.builder(%T.getServiceDescriptor())",
                CommonClassNames.grpcServerServiceDefinition,
                protoService.enclosingServiceClassName
            )
            .indent()

        protoService.methodDefinitions.forEach { method ->
            val handlerClassName = when(method.type){
                MethodDescriptor.MethodType.UNARY -> CallHandlerClassNames.unary
                MethodDescriptor.MethodType.CLIENT_STREAMING -> CallHandlerClassNames.clientStreaming
                MethodDescriptor.MethodType.SERVER_STREAMING -> CallHandlerClassNames.serverStreaming
                MethodDescriptor.MethodType.BIDI_STREAMING -> CallHandlerClassNames.bidiStreaming
                MethodDescriptor.MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
            }

            builder.addStatement("""
                |.addMethod(
                |   ${method.methodDefinitionPropName},
                |   %T(
                |       ${method.methodDefinitionPropName},
                |       MethodHandlers(this, %N)
                |   )
                |)
            """.trimMargin(),
                handlerClassName,
                method.idPropertyName
                )
        }

        return builder.addStatement("return builder.build()").unindent().build()
    }

    object CallHandlerClassNames {
        val unary = ClassName(CommonPackages.krotoCoroutineLib+".server","unaryServerCallHandler")
        val serverStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","serverStreamingServerCallHandler")
        val clientStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","clientStreamingServerCallHandler")
        val bidiStreaming = ClassName(CommonPackages.krotoCoroutineLib+".server","bidiStreamingServerCallHandler")
    }

}