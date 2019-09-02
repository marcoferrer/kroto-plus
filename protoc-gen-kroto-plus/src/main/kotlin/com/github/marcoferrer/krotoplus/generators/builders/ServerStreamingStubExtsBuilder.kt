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

import com.github.marcoferrer.krotoplus.config.GrpcStubExtsGenOptions
import com.github.marcoferrer.krotoplus.generators.GeneratorContext
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.builderLambdaTypeName
import com.github.marcoferrer.krotoplus.utils.requestParamSpec
import com.github.marcoferrer.krotoplus.utils.requestValueBuilderCodeBlock
import com.github.marcoferrer.krotoplus.utils.requestValueMethodSigCodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName


class ServerStreamingStubExtsBuilder(val context: GeneratorContext){

    fun buildStubExts(method: ProtoMethod, options: GrpcStubExtsGenOptions): List<FunSpec> {
        val funSpecs = mutableListOf<FunSpec>()

        // Add method signature exts
        if(method.methodSignatureFields.isNotEmpty()){
            funSpecs += buildAsyncMethodSigExt(method)
            funSpecs += buildBlockingMethodSigExt(method)
        }

        // Add lambda builder exts
        funSpecs += buildAsyncLambdaBuilderExt(method)
        funSpecs += buildBlockingLambdaBuilderExt(method)

        // Add default arg exts
        funSpecs += buildAsyncDefaultArgExt(method)
        funSpecs += buildBlockingDefaultArgExt(method)

        if(options.supportCoroutines)
            addCoroutineStubExts(funSpecs, method)

        return funSpecs
    }

    private fun addCoroutineStubExts(funSpecs: MutableList<FunSpec>, method: ProtoMethod){
        if(method.methodSignatureFields.isNotEmpty()) {
            funSpecs += buildCoroutineMethodSigExt(method)
        }

        funSpecs += buildCoroutineExt(method)
        funSpecs += buildCoroutineLambdaBuilderExt(method)
    }

    // Coroutine Extension

    private fun buildCoroutineExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .receiver(protoService.asyncStubClassName)
            .addParameter(requestClassName.requestParamSpec)
            .addStatement(
                "return %T(request, %T.%N())",
                CommonClassNames.ClientCalls.clientCallServerStreaming,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    // Method Signature Extensions

    private fun buildAsyncMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .returns(UNIT)
            .addMethodSignatureParameter(methodSignatureFields, context.schema)
            .addResponseObserverParameter(responseClassName)
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("%N(request, responseObserver)",functionName)
            .build()
    }

    private fun buildCoroutineMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addMethodSignatureParameter(methodSignatureFields, context.schema)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildBlockingMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.blockingStubClassName)
            .addMethodSignatureParameter(methodSignatureFields, context.schema)
            .returns(Iterator::class.asClassName().parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    // Lambda Builder Extensions

    private fun buildAsyncLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .receiver(protoService.asyncStubClassName)
            .addResponseObserverParameter(responseClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .returns(UNIT)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("%N(request, responseObserver)", functionName)
            .build()
    }

    private fun buildCoroutineLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addModifiers(KModifier.INLINE)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)", functionName)
            .build()
    }

    private fun buildBlockingLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .receiver(protoService.blockingStubClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .returns(Iterator::class.asClassName().parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)", functionName)
            .build()
    }

    // Default Argument Extensions

    private fun buildAsyncDefaultArgExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addResponseObserverParameter(responseClassName)
            .returns(UNIT)
            .addStatement("%N(%T.getDefaultInstance(),responseObserver)", functionName, requestClassName)
            .build()
    }

    private fun buildBlockingDefaultArgExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.blockingStubClassName)
            .returns(Iterator::class.asClassName().parameterizedBy(responseClassName))
            .addStatement("return %N(%T.getDefaultInstance())", functionName, requestClassName)
            .build()
    }

}
