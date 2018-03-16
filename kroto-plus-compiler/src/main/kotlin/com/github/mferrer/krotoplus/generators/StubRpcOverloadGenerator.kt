package com.github.mferrer.krotoplus.generators

import com.github.mferrer.krotoplus.generators.FileSpecProducer.Companion.AutoGenerationDisclaimer
import com.github.mferrer.krotoplus.schema.ServiceWrapper
import com.github.mferrer.krotoplus.schema.isCommonProtoFile
import com.github.mferrer.krotoplus.schema.isEmptyMessage
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.Schema
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch

class StubRpcOverloadGenerator(
        override val schema: Schema, override val fileSpecChannel: Channel<FileSpec>
) : SchemaConsumer, FileSpecProducer {

    override fun consume() = launch {
        schema.protoFiles()
                .asSequence()
                .filterNot { it.isCommonProtoFile }
                .forEach { protoFile ->
                    for (service in protoFile.services())
                        launch(coroutineContext) {
                            buildFileSpec(ServiceWrapper(service, protoFile, schema))
                        }
                }
    }

    private suspend fun buildFileSpec(serviceWrapper: ServiceWrapper) {
        val filename = "${serviceWrapper.name}RpcOverloads"
        val fileSpecBuilder = FileSpec
                .builder(serviceWrapper.protoFile.javaPackage(), filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        for(methodWrappers in serviceWrapper.methodDefinitions)
            buildFunSpecs(methodWrappers, fileSpecBuilder)

        fileSpecChannel.send(fileSpecBuilder.build())
    }

    private fun buildFunSpecs(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ){
        if(methodWrapper.method.requestStreaming() || methodWrapper.method.responseStreaming())
            return //TODO Add support for streaming rpc calls

        val funSpecBuilder = FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.INLINE)
                .addParameter("block",LambdaTypeName.get(
                        receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                        returnType = UNIT))

        val requestValueTemplate = if(methodWrapper.method.requestType().isEmptyMessage)
            "val request = %T.getDefaultInstance()\n" else "val request = %T.newBuilder().apply(block).build()\n"

        val requestValueCb = CodeBlock.of(requestValueTemplate, methodWrapper.requestClassName)

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.futureStubClassName)
                .addCode(requestValueCb)
                .addStatement("return %N(request)", methodWrapper.functionName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.google.common.util.concurrent","ListenableFuture"),
                        methodWrapper.responseClassName))
                .also { fileSpecBuilder.addFunction(it.build()) }

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.blockingStubClassName)
                .returns(methodWrapper.responseClassName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        buildAsyncStubOverloads(methodWrapper,fileSpecBuilder)
    }

    private fun buildAsyncStubOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ) {

        fileSpecBuilder
                .addStaticImport("com.github.mferrer.krotoplus.coroutines","suspendingAsyncUnaryCall")
                .addStaticImport(methodWrapper.serviceWrapper.enclosingServiceClassName,methodWrapper.methodDescriptorName)

        val requestValueTemplate = if(methodWrapper.method.requestType().isEmptyMessage)
            "val request = %T.getDefaultInstance()\n" else "val request = %T.newBuilder().apply(block).build()\n"

        val requestValueCb = CodeBlock.of(requestValueTemplate,methodWrapper.requestClassName)

        FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .addParameter("request",methodWrapper.requestClassName)
                .returns(methodWrapper.responseClassName)
                .addStatement(
                        "return %N(%N,channel,callOptions, request)",
                        "\n  suspendingAsyncUnaryCall",
                        methodWrapper.methodDescriptorName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.SUSPEND,KModifier.INLINE)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .addParameter("block",LambdaTypeName.get(
                        receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                        returnType = UNIT))
                .returns(methodWrapper.responseClassName)
                .addCode(requestValueCb)
                .addStatement("return %N(request)", methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }

    }

}