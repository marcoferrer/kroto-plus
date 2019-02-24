package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.GrpcStubExtsGenOptions
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.addFunctions
import com.github.marcoferrer.krotoplus.utils.matches
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*


object GrpcStubExtsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcStubExtsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (options in context.config.grpcStubExtsList) {
            val fileBuilder = FileBuilder(options)

            for (service in context.schema.protoServices) {

                fileBuilder.buildFileSpec(service)?.let { fileSpec ->
                    responseBuilder.addFile(fileSpec)
                }
            }
        }

        return responseBuilder.build()
    }

    class FileBuilder(val options: GrpcStubExtsGenOptions){

        fun buildFileSpec(service: ProtoService): PluginProtos.CodeGeneratorResponse.File? {

            if (!options.filter.matches(service.protoFile.name))
                return null

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

            val stubExtsSpecs = service.buildStubExts()

            return fileSpecBuilder
                .takeIf { stubExtsSpecs.isNotEmpty() }
                ?.apply { addFunctions(stubExtsSpecs) }
                ?.build()
                ?.toResponseFileProto()
        }

        private fun ProtoService.buildStubExts(): List<FunSpec> {

            val funSpecs = mutableListOf<FunSpec>()
            for(method in methodDefinitions){
                when {
                    //Unary
                    method.isUnary ->
                        funSpecs += method.buildUnaryOverloads()

                    //Server Streaming //TODO Add support for Blocking Server Streaming
                    options.supportCoroutines && method.isServerStream ->
                        funSpecs += method.buildServerStreamingOverloads()

                    //Bidi && Client Streaming
                    options.supportCoroutines && (method.isBidi || method.isClientStream) ->
                        funSpecs += method.buildBidiStreamingOverloads()
                }
            }

            return funSpecs
        }

        private fun ProtoMethod.buildUnaryOverloads(): List<FunSpec> {

            val funSpecBuilder = FunSpec.builder(functionName)
                .addCode(requestValueCodeBlock())
                .addStatement("return %N(request)", functionName)

            if (isNotEmptyInput) {
                funSpecBuilder
                    .addModifiers(KModifier.INLINE)
                    .addParameter(
                        "block", LambdaTypeName.get(
                            receiver = requestClassName.nestedClass("Builder"),
                            returnType = UNIT
                        )
                    )
            }

            val funSpecs = mutableListOf<FunSpec>()

            // Future Stub Ext
            funSpecs += funSpecBuilder
                .receiver(protoService.futureStubClassName)
                .returns(CommonClassNames.listenableFuture.parameterizedBy(responseClassName))
                .build()

            // Blocking Stub Ext
            funSpecs += funSpecBuilder
                .receiver(protoService.blockingStubClassName)
                .returns(responseClassName)
                .build()

            if (options.supportCoroutines) {
                funSpecs += buildUnaryCoroutineExt()

                if(isNotEmptyInput){
                    funSpecs += buildUnaryCoroutineExtOverload()
                }
            }

            return funSpecs
        }

        private fun ProtoMethod.buildUnaryCoroutineExt(): FunSpec =
            FunSpec.builder(functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(protoService.asyncStubClassName)
                .apply {
                    if (isNotEmptyInput)
                        addParameter("request", requestClassName) else
                        addCode(requestValueCodeBlock())
                }
                .returns(responseClassName)
                .addStatement(
                    "return %T{ observer -> %N(request,observer) }",
                    CommonClassNames.suspendingUnaryCallObserver,
                    functionName
                )
                .build()

        private fun ProtoMethod.buildUnaryCoroutineExtOverload(): FunSpec =
            FunSpec.builder(functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(protoService.asyncStubClassName)
                .apply {
                    if (isNotEmptyInput) {
                        addModifiers(KModifier.INLINE)
                        addParameter(
                            "block", LambdaTypeName.get(
                                receiver = requestClassName.nestedClass("Builder"),
                                returnType = UNIT
                            )
                        )
                    }
                }
                .returns(responseClassName)
                .addCode(requestValueCodeBlock())
                .addStatement("return %N(request)", functionName)
                .build()


        private fun ProtoMethod.buildServerStreamingOverloads(): FunSpec {

            val returnType = CommonClassNames.inboundStreamChannel
                .parameterizedBy(responseClassName)

            return FunSpec.builder(functionName)
                .receiver(protoService.asyncStubClassName)
                .apply {
                    if (isNotEmptyInput)
                        addParameter("request", requestClassName) else
                        addCode(requestValueCodeBlock())
                }
                .returns(returnType)
                .addStatement(
                    "return %T().also { observer -> %N(request,observer) }",
                    returnType,
                    functionName
                )
                .build()
        }

        private fun ProtoMethod.buildBidiStreamingOverloads(): FunSpec =
            FunSpec.builder(functionName)
                .receiver(protoService.asyncStubClassName)
                .addAnnotation(CommonClassNames.obsoleteCoroutinesApi)
                .addAnnotation(CommonClassNames.experimentalCoroutinesApi)
                .addAnnotation(CommonClassNames.experimentalKrotoPlusCoroutinesApi)
                .returns(
                    CommonClassNames.clientBidiCallChannel.parameterizedBy(
                        requestClassName,
                        responseClassName
                    )
                )
                .addStatement(
                    "return %T{ responseObserver -> %N(responseObserver) }",
                    CommonClassNames.bidiCallChannel,
                    functionName
                )
                .build()

        private fun ProtoMethod.requestValueCodeBlock(): CodeBlock {
            val requestValueTemplate = if (isEmptyInput)
                "val request = %T.getDefaultInstance()\n" else
                "val request = %T.newBuilder().apply(block).build()\n"

            return CodeBlock.of(requestValueTemplate, requestClassName)
        }

    }
}