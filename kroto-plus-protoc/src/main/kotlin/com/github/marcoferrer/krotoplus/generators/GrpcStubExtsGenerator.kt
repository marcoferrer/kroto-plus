package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.Service
import com.github.marcoferrer.krotoplus.schema.isEmptyInput
import com.github.marcoferrer.krotoplus.schema.isNotEmptyInput
import com.github.marcoferrer.krotoplus.schema.javaPackage
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*


class GrpcStubExtsGenerator(override val context: Generator.Context) : Generator {

    override val key = "grpc-stub-exts"

    private val supportCoroutines = "support_coroutines".let {
        hasFlag(it) || getOption(it).orEmpty().toBoolean()
    }

    override fun invoke(responseBuilder: PluginProtos.CodeGeneratorResponse.Builder) {

        for (service in context.schema.services)
            buildFileSpec(service)?.let { responseBuilder.addFile(it) }
    }

    private fun buildFileSpec(service: Service): PluginProtos.CodeGeneratorResponse.File? {
        val filename = "${service.name}RpcOverloads"
        val fileSpecBuilder = FileSpec
                .builder(service.protoFile.javaPackage, filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        for(method in service.methodDefinitions)
            buildFunSpecs(method, fileSpecBuilder)

        return fileSpecBuilder.build()
                .takeIf { it.members.isNotEmpty() }
                ?.toResponseFileProto()
    }

    private fun buildFunSpecs(method: Service.Method, fileSpecBuilder: FileSpec.Builder){
        when{
            //Unary
            method.isUnary ->
                buildUnaryOverloads(method,fileSpecBuilder)

            //Server Streaming //TODO Add support for Blocking Server Streaming
            supportCoroutines && method.isServerStream ->
                buildServerStreamingOverloads(method,fileSpecBuilder)

            //Bidi && Client Streaming
            supportCoroutines && (method.isBidi || method.isClientStream)->
                buildBidiStreamingOverloads(method,fileSpecBuilder)
        }
    }

    private fun buildUnaryOverloads(method: Service.Method, fileSpecBuilder: FileSpec.Builder) {

        val funSpecBuilder = FunSpec.builder(method.functionName)

        if(method.method.isNotEmptyInput) {
            funSpecBuilder
                    .addModifiers(KModifier.INLINE)
                    .addParameter("block", LambdaTypeName.get(
                            receiver = method.requestClassName.nestedClass("Builder"),
                            returnType = UNIT))
        }

        funSpecBuilder
                .receiver(method.service.futureStubClassName)
                .addCode(method.requestValueCodeBlock())
                .addStatement("return %N(request)", method.functionName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.google.common.util.concurrent","ListenableFuture"),
                        method.responseClassName))
                .also { fileSpecBuilder.addFunction(it.build()) }

        funSpecBuilder
                .receiver(method.service.blockingStubClassName)
                .returns(method.responseClassName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        if(supportCoroutines)
            buildSuspendingUnaryOverloads(method,fileSpecBuilder)
    }

    private fun buildSuspendingUnaryOverloads(method: Service.Method, fileSpecBuilder: FileSpec.Builder) {

        fileSpecBuilder.addStaticImport("com.github.marcoferrer.krotoplus.coroutines", "suspendingUnaryCallObserver")

        FunSpec.builder(method.functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(method.service.asyncStubClassName)
                .apply {
                    if(method.method.isNotEmptyInput)
                        addParameter("request", method.requestClassName) else
                        addCode(method.requestValueCodeBlock())
                }
                .returns(method.responseClassName)
                .addStatement(
                        "return %W suspendingUnaryCallObserver{ observer -> %N(request,observer) }",
                        method.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        if(method.method.isNotEmptyInput) {
            FunSpec.builder(method.functionName)
                    .addModifiers(KModifier.SUSPEND)
                    .receiver(method.service.asyncStubClassName)
                    .apply {
                        if(method.method.isNotEmptyInput) {
                            addModifiers(KModifier.INLINE)
                            addParameter("block", LambdaTypeName.get(
                                    receiver = method.requestClassName.nestedClass("Builder"),
                                    returnType = UNIT))
                        }
                    }
                    .returns(method.responseClassName)
                    .addCode(method.requestValueCodeBlock())
                    .addStatement("return %N(request)", method.functionName)
                    .also { fileSpecBuilder.addFunction(it.build()) }
        }
    }

    private fun buildServerStreamingOverloads(method: Service.Method, fileSpecBuilder: FileSpec.Builder){

        val inboundChannelClassName =
                ClassName("com.github.marcoferrer.krotoplus.coroutines","InboundStreamChannel")

        val returnType = ParameterizedTypeName.get(inboundChannelClassName,method.responseClassName)

        FunSpec.builder(method.functionName)
                .receiver(method.service.asyncStubClassName)
                .apply {
                    if(method.method.isNotEmptyInput)
                        addParameter("request", method.requestClassName) else
                        addCode(method.requestValueCodeBlock())
                }
                .returns(returnType)
                .addStatement(
                        "return %T().also { observer -> %N(request,observer) }",
                        returnType,
                        method.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
    }


    private fun buildBidiStreamingOverloads(method: Service.Method, fileSpecBuilder: FileSpec.Builder){

        fileSpecBuilder.addStaticImport("com.github.marcoferrer.krotoplus.coroutines","bidiCallChannel")

        FunSpec.builder(method.functionName)
                .receiver(method.service.asyncStubClassName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.github.marcoferrer.krotoplus.coroutines","ClientBidiCallChannel"),
                        method.requestClassName,
                        method.responseClassName
                ))
                .addStatement("return %W bidiCallChannel{ responseObserver -> %N(responseObserver) }",
                        method.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
    }

    private fun Service.Method.requestValueCodeBlock(): CodeBlock{
        val requestValueTemplate = if(method.isEmptyInput)
            "val request = %T.getDefaultInstance()\n" else
            "val request = %T.newBuilder().apply(block).build()\n"

        return CodeBlock.of(requestValueTemplate, requestClassName)
    }
}