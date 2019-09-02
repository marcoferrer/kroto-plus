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
import com.github.marcoferrer.krotoplus.proto.getFieldClassName
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.addForEach
import com.github.marcoferrer.krotoplus.utils.builderLambdaTypeName
import com.github.marcoferrer.krotoplus.utils.requestParamSpec
import com.github.marcoferrer.krotoplus.utils.requestValueBuilderCodeBlock
import com.github.marcoferrer.krotoplus.utils.requestValueMethodSigCodeBlock
import com.github.marcoferrer.krotoplus.utils.toUpperCamelCase
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.grpc.MethodDescriptor


class GrpcStubBuilder(val context: GeneratorContext){


    fun buildStub(protoService: ProtoService): TypeSpec = with(protoService) {

        val paramNameChannel = "channel"
        val paramNameCallOptions = "callOptions"

        TypeSpec.classBuilder(stubName)
            .superclass(CommonClassNames.grpcAbstractStub.parameterizedBy(stubClassName))
            .addSuperclassConstructorParameter(paramNameChannel)
            .addSuperclassConstructorParameter(paramNameCallOptions)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter(paramNameChannel, CommonClassNames.grpcChannel)
                    .addParameter(
                        ParameterSpec
                            .builder(paramNameCallOptions, CommonClassNames.grpcCallOptions)
                            .defaultValue("%T.DEFAULT", CommonClassNames.grpcCallOptions)
                            .build()
                    )
                    .build()
            )
            .addFunction(
                FunSpec
                    .builder("build")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(paramNameChannel, CommonClassNames.grpcChannel )
                    .addParameter(paramNameCallOptions, CommonClassNames.grpcCallOptions )
                    .returns(stubClassName)
                    .addStatement("return %T(channel,callOptions)",stubClassName)
                    .build()
            )
            .addRpcMethods(protoService)
            .addType(buildCompanionObject())
            .build()
    }

    fun TypeSpec.Builder.addRpcMethods(service: ProtoService): TypeSpec.Builder = apply {

        for(method in service.methodDefinitions) when(method.type){
            MethodDescriptor.MethodType.UNARY -> {
                addFunction(buildUnaryMethod(method))
                addFunction(buildUnaryLambdaOverload(method))
                buildUnaryMethodSigOverload(method)?.let { addFunction(it) }
            }

            MethodDescriptor.MethodType.SERVER_STREAMING -> {
                addFunction(buildServerStreamingMethod(method))
                addFunction(buildServerStreamingLambdaOverload(method))
                buildServerStreamingMethodSigOverload(method)?.let { addFunction(it) }
            }

            MethodDescriptor.MethodType.CLIENT_STREAMING ->
                addFunction(buildClientStreamingMethod(method))

            MethodDescriptor.MethodType.BIDI_STREAMING ->
                addFunction(buildBidiStreamingMethod(method))

            MethodDescriptor.MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
        }
    }

    // Default method

    private fun buildUnaryMethod(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND)
            .returns(responseClassName)
            .addParameter(requestClassName.requestParamSpec)
            .addStatement(
                "return %T(request, %T.%N())",
                CommonClassNames.ClientCalls.clientCallUnary,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    private fun buildServerStreamingMethod(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addParameter(requestClassName.requestParamSpec)
            .addStatement(
                "return %T(request, %T.%N())",
                CommonClassNames.ClientCalls.clientCallServerStreaming,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    private fun buildClientStreamingMethod(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .returns(
                CommonClassNames.ClientChannels.clientStreamingCallChannel.parameterizedBy(
                    requestClassName,
                    responseClassName
                )
            )
            .addStatement(
                "return %T(%T.%N())",
                CommonClassNames.ClientCalls.clientCallClientStreaming,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    private fun buildBidiStreamingMethod(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .returns(
                CommonClassNames.ClientChannels.clientBidiCallChannel.parameterizedBy(
                    requestClassName,
                    responseClassName
                )
            )
            .addStatement(
                "return %T(%T.%N())",
                CommonClassNames.ClientCalls.clientCallBidiStreaming,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    // Lambda builder overloads

    private fun buildUnaryLambdaOverload(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.INLINE)
            .returns(responseClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildServerStreamingLambdaOverload(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)",functionName)
            .build()
    }

    // Method signature overloads

    private fun buildUnaryMethodSigOverload(protoMethod: ProtoMethod): FunSpec? = with(protoMethod){
        if(methodSignatureFields.isEmpty())
            null else FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND)
            .returns(responseClassName)
            .addForEach(methodSignatureFields){
                addParameter(it.name.toUpperCamelCase().decapitalize(), it.getFieldClassName(context.schema))
            }
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildServerStreamingMethodSigOverload(protoMethod: ProtoMethod): FunSpec? = with(protoMethod){
        if(methodSignatureFields.isEmpty())
            null else FunSpec.builder(functionName)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addForEach(methodSignatureFields){
                addParameter(it.name.toUpperCamelCase().decapitalize(), it.getFieldClassName(context.schema))
            }
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    // Stub companion object

    private fun ProtoService.buildCompanionObject(): TypeSpec =
        TypeSpec.companionObjectBuilder()
            .addSuperinterface(CommonClassNames.stubDefinition.parameterizedBy(stubClassName))
            .addProperty(
                PropertySpec.builder("serviceName", String::class.asClassName())
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T.SERVICE_NAME", enclosingServiceClassName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("newStub")
                    .returns(stubClassName)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("channel", CommonClassNames.grpcChannel)
                    .addCode("return %T(channel)", stubClassName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("newStubWithContext")
                    .returns(stubClassName)
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                    .addParameter("channel", CommonClassNames.grpcChannel)
                    .addCode("return %T(channel).%T()", stubClassName, CommonClassNames.withCoroutineContext)
                    .build()
            )
            .build()

}
