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

package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.GrpcStubExtsGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.proto.Schema
import com.github.marcoferrer.krotoplus.proto.getFieldClassName
import com.github.marcoferrer.krotoplus.utils.*
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.MethodDescriptor.*


object GrpcStubExtsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcStubExtsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (options in context.config.grpcStubExtsList) {
            val fileBuilder = FileBuilder(options)

            for (service in context.schema.protoServices) {
                if(isFileToGenerate(service.protoFile.name, options.filter)){
                    fileBuilder.buildFile(service)?.let { file ->
                        responseBuilder.addFile(file)
                    }
                }
            }
        }

        return responseBuilder.build()
    }

    class FileBuilder(val options: GrpcStubExtsGenOptions) {

        private val unaryExtBuilder = UnaryStubExtsBuilder(context)
        private val serverStreamingExtBuilder = ServerStreamingStubExtsBuilder(context)

        fun buildFile(service: ProtoService): PluginProtos.CodeGeneratorResponse.File? = with(service) {

            if (!isFileToGenerate(protoFile.name,options.filter))
                return null

            val filename = "${name}RpcOverloads"
            val fileSpecBuilder = FileSpec
                .builder(protoFile.javaPackage, filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(
                    AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S", "-$filename")
                        .build()
                )

            fileSpecBuilder.apply {
                methodDefinitions.forEach { method ->
                    when (method.type) {
                        MethodType.UNARY ->
                            addFunctions(unaryExtBuilder.buildStubExts(service, options))

                        MethodType.SERVER_STREAMING ->
                            addFunctions(serverStreamingExtBuilder.buildStubExts(service, options))

                        MethodType.CLIENT_STREAMING -> if(options.supportCoroutines)
                            addFunction(method.buildStubClientStreamingMethod())

                        MethodType.BIDI_STREAMING -> if(options.supportCoroutines)
                            addFunction(method.buildStubCoroutineBidiStreamingMethod())

                        MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
                    }
                }
            }

            return fileSpecBuilder
                .build()
                .takeIf { it.members.isNotEmpty() }
                ?.toResponseFileProto()
        }

        private fun ProtoMethod.buildStubClientStreamingMethod(): FunSpec =
            FunSpec.builder(functionName)
                .receiver(protoService.asyncStubClassName)
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

        private fun ProtoMethod.buildStubCoroutineBidiStreamingMethod(): FunSpec =
            FunSpec.builder(functionName)
                .receiver(protoService.asyncStubClassName)
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
}

class ServerStreamingStubExtsBuilder(val context: GeneratorContext){

    fun buildStubExts(protoService: ProtoService, options: GrpcStubExtsGenOptions): List<FunSpec> {
        val funSpecs = mutableListOf<FunSpec>()

        protoService.methodDefinitions
            .filter { it.isServerStream }
            .forEach { method ->

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
            }

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
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .addResponseObserverParamerter(responseClassName)
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("%N(request, responseObserver)",functionName)
            .build()
    }

    private fun buildCoroutineMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildBlockingMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.blockingStubClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
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
            .addResponseObserverParamerter(responseClassName)
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
            .addResponseObserverParamerter(responseClassName)
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

class UnaryStubExtsBuilder(val context: GeneratorContext){

    fun buildStubExts(protoService: ProtoService, options: GrpcStubExtsGenOptions): List<FunSpec> {
        val funSpecs = mutableListOf<FunSpec>()

        protoService.methodDefinitions
            .filter { it.isUnary }
            .forEach { method ->

                // Add method signature exts
                if(method.methodSignatureFields.isNotEmpty()){
                    funSpecs += buildAsyncMethodSigExt(method)
                    funSpecs += buildFutureMethodSigExt(method)
                    funSpecs += buildBlockingMethodSigExt(method)
                }

                // Add lambda builder exts
                funSpecs += buildAsyncLambdaBuilderExt(method)
                funSpecs += buildFutureLambdaBuilderExt(method)
                funSpecs += buildBlockingLambdaBuilderExt(method)

                // Add default arg exts
                funSpecs += buildAsyncDefaultArgExt(method)
                funSpecs += buildFutureDefaultArgExt(method)
                funSpecs += buildBlockingDefaultArgExt(method)

                if(options.supportCoroutines)
                    addCoroutineStubExts(funSpecs, method)
            }

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
            .addModifiers(KModifier.SUSPEND)
            .receiver(protoService.asyncStubClassName)
            .addParameter(requestClassName.requestParamSpec)
            .returns(responseClassName)
            .addStatement(
                "return %T(request, %T.%N())",
                CommonClassNames.ClientCalls.clientCallUnary,
                protoService.enclosingServiceClassName,
                methodDefinitionGetterName
            )
            .build()
    }

    // Method Signature Extensions

    private fun buildAsyncMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .addResponseObserverParamerter(responseClassName)
            .returns(UNIT)
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("%N(request, responseObserver)",functionName)
            .build()
    }

    private fun buildCoroutineMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND)
            .receiver(protoService.asyncStubClassName)
            .returns(responseClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildFutureMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.futureStubClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .returns(CommonClassNames.listenableFuture.parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    private fun buildBlockingMethodSigExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.blockingStubClassName)
            .returns(responseClassName)
            .addMethodSignatureParamerter(methodSignatureFields, context.schema)
            .addCode(requestClassName.requestValueMethodSigCodeBlock(methodSignatureFields))
            .addStatement("return %N(request)",functionName)
            .build()
    }

    // Lambda Builder Extensions

    private fun buildAsyncLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addModifiers(KModifier.INLINE)
            .addResponseObserverParamerter(responseClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .returns(UNIT)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("%N(request, responseObserver)", functionName)
            .build()
    }

    private fun buildCoroutineLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addModifiers(KModifier.INLINE, KModifier.SUSPEND)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .returns(responseClassName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)", functionName)
            .build()
    }

    private fun buildFutureLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .receiver(protoService.futureStubClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .returns(CommonClassNames.listenableFuture.parameterizedBy(responseClassName))
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)", functionName)
            .build()
    }

    private fun buildBlockingLambdaBuilderExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addStatement("return %N(request)", functionName)
            .receiver(protoService.blockingStubClassName)
            .returns(responseClassName)
            .build()
    }

    // Default Argument Extensions

    private fun buildAsyncDefaultArgExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .receiver(protoService.asyncStubClassName)
            .addResponseObserverParamerter(responseClassName)
            .addStatement("%N(%T.getDefaultInstance(),responseObserver)", functionName, requestClassName)
            .returns(UNIT)
            .build()
    }

    private fun buildFutureDefaultArgExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addStatement("return %N(%T.getDefaultInstance())", functionName, requestClassName)
            .receiver(protoService.futureStubClassName)
            .returns(CommonClassNames.listenableFuture.parameterizedBy(responseClassName))
            .build()
    }

    private fun buildBlockingDefaultArgExt(protoMethod: ProtoMethod): FunSpec = with(protoMethod){
        FunSpec.builder(functionName)
            .addStatement("return %N(%T.getDefaultInstance())", functionName, requestClassName)
            .receiver(protoService.blockingStubClassName)
            .returns(responseClassName)
            .build()
    }
}

private fun FunSpec.Builder.addResponseObserverParamerter(responseClassName: ClassName): FunSpec.Builder = apply {
    addParameter("responseObserver", CommonClassNames.streamObserver.parameterizedBy(responseClassName))
}

private fun FunSpec.Builder.addMethodSignatureParamerter(
    methodSignatureFields: List<DescriptorProtos.FieldDescriptorProto>,
    schema: Schema
): FunSpec.Builder = apply {
    addForEach(methodSignatureFields){
        addParameter(it.name.toUpperCamelCase().decapitalize(), it.getFieldClassName(schema))
    }
}