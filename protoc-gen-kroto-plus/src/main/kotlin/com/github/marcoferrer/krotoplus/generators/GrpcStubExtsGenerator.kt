package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.GrpcStubExtsGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.matches
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*


object GrpcStubExtsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcStubExtsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (service in context.schema.protoServices) {
            for (options in context.config.grpcStubExtsList) {

                if (options.filter.matches(service.protoFile.name)) {
                    buildFileSpec(service, options)?.let { responseBuilder.addFile(it) }
                }
            }
        }

        return responseBuilder.build()
    }

    private fun buildFileSpec(
        service: ProtoService,
        options: GrpcStubExtsGenOptions
    ): PluginProtos.CodeGeneratorResponse.File? {
        val filename = "${service.name}RpcOverloads"
        val fileSpecBuilder = FileSpec
            .builder(service.protoFile.javaPackage, filename)
            .addComment(AutoGenerationDisclaimer)
            .addAnnotation(
                AnnotationSpec.builder(JvmName::class.asClassName())
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "-$filename")
                    .build()
            )

        for (method in service.methodDefinitions)
            buildFunSpecs(method, options, fileSpecBuilder)

        return fileSpecBuilder.build()
            .takeIf { it.members.isNotEmpty() }
            ?.toResponseFileProto()
    }

    private fun buildFunSpecs(
        method: ProtoMethod,
        options: GrpcStubExtsGenOptions,
        fileSpecBuilder: FileSpec.Builder
    ) {
        when {
            //Unary
            method.isUnary ->
                buildUnaryOverloads(method, options, fileSpecBuilder)

            //Server Streaming //TODO Add support for Blocking Server Streaming
            options.supportCoroutines && method.isServerStream ->
                buildServerStreamingOverloads(method, fileSpecBuilder)

            //Bidi && Client Streaming
            options.supportCoroutines && (method.isBidi || method.isClientStream) ->
                buildBidiStreamingOverloads(method, fileSpecBuilder)
        }
    }

    private fun buildUnaryOverloads(
        method: ProtoMethod,
        options: GrpcStubExtsGenOptions,
        fileSpecBuilder: FileSpec.Builder
    ) {

        val funSpecBuilder = FunSpec.builder(method.functionName)

        if (method.isNotEmptyInput) {
            funSpecBuilder
                .addModifiers(KModifier.INLINE)
                .addParameter(
                    "block", LambdaTypeName.get(
                        receiver = method.requestClassName.nestedClass("Builder"),
                        returnType = UNIT
                    )
                )
        }

        funSpecBuilder
            .receiver(method.protoService.futureStubClassName)
            .addCode(method.requestValueCodeBlock())
            .addStatement("return %N(request)", method.functionName)
            .returns(
                ParameterizedTypeName.get(
                    ClassName("com.google.common.util.concurrent", "ListenableFuture"),
                    method.responseClassName
                )
            )
            .also { fileSpecBuilder.addFunction(it.build()) }

        funSpecBuilder
            .receiver(method.protoService.blockingStubClassName)
            .returns(method.responseClassName)
            .also { fileSpecBuilder.addFunction(it.build()) }

        if (options.supportCoroutines)
            buildSuspendingUnaryOverloads(method, fileSpecBuilder)
    }

    private fun buildSuspendingUnaryOverloads(method: ProtoMethod, fileSpecBuilder: FileSpec.Builder) {

        fileSpecBuilder.addStaticImport("com.github.marcoferrer.krotoplus.coroutines", "suspendingUnaryCallObserver")

        FunSpec.builder(method.functionName)
            .addModifiers(KModifier.SUSPEND)
            .receiver(method.protoService.asyncStubClassName)
            .apply {
                if (method.isNotEmptyInput)
                    addParameter("request", method.requestClassName) else
                    addCode(method.requestValueCodeBlock())
            }
            .returns(method.responseClassName)
            .addStatement(
                "return suspendingUnaryCallObserver{ observer -> %N(request,observer) }",
                method.functionName
            )
            .also { fileSpecBuilder.addFunction(it.build()) }

        if (method.isNotEmptyInput) {
            FunSpec.builder(method.functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(method.protoService.asyncStubClassName)
                .apply {
                    if (method.isNotEmptyInput) {
                        addModifiers(KModifier.INLINE)
                        addParameter(
                            "block", LambdaTypeName.get(
                                receiver = method.requestClassName.nestedClass("Builder"),
                                returnType = UNIT
                            )
                        )
                    }
                }
                .returns(method.responseClassName)
                .addCode(method.requestValueCodeBlock())
                .addStatement("return %N(request)", method.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
        }
    }

    private fun buildServerStreamingOverloads(method: ProtoMethod, fileSpecBuilder: FileSpec.Builder) {

        val inboundChannelClassName =
            ClassName("com.github.marcoferrer.krotoplus.coroutines", "InboundStreamChannel")

        val returnType = ParameterizedTypeName.get(inboundChannelClassName, method.responseClassName)

        FunSpec.builder(method.functionName)
            .receiver(method.protoService.asyncStubClassName)
            .apply {
                if (method.isNotEmptyInput)
                    addParameter("request", method.requestClassName) else
                    addCode(method.requestValueCodeBlock())
            }
            .returns(returnType)
            .addStatement(
                "return %T().also { observer -> %N(request,observer) }",
                returnType,
                method.functionName
            )
            .also { fileSpecBuilder.addFunction(it.build()) }
    }


    private fun buildBidiStreamingOverloads(method: ProtoMethod, fileSpecBuilder: FileSpec.Builder) {

        fileSpecBuilder.addStaticImport("com.github.marcoferrer.krotoplus.coroutines", "bidiCallChannel")

        FunSpec.builder(method.functionName)
            .receiver(method.protoService.asyncStubClassName)
            .addAnnotation(ClassName("kotlinx.coroutines", "ObsoleteCoroutinesApi"))
            .addAnnotation(ClassName("kotlinx.coroutines", "ExperimentalCoroutinesApi"))
            .addAnnotation(
                ClassName(
                    "com.github.marcoferrer.krotoplus.coroutines",
                    "ExperimentalKrotoPlusCoroutinesApi"
                )
            )
            .returns(
                ParameterizedTypeName.get(
                    ClassName("com.github.marcoferrer.krotoplus.coroutines", "ClientBidiCallChannel"),
                    method.requestClassName,
                    method.responseClassName
                )
            )
            .addStatement(
                "return bidiCallChannel{ responseObserver -> %N(responseObserver) }",
                method.functionName
            )
            .also { fileSpecBuilder.addFunction(it.build()) }
    }

    private fun ProtoMethod.requestValueCodeBlock(): CodeBlock {
        val requestValueTemplate = if (isEmptyInput)
            "val request = %T.getDefaultInstance()\n" else
            "val request = %T.newBuilder().apply(block).build()\n"

        return CodeBlock.of(requestValueTemplate, requestClassName)
    }
}