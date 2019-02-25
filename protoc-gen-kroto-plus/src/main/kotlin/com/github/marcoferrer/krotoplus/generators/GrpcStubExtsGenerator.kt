package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.GrpcStubExtsGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.*
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

                fileBuilder.buildFile(service)?.let { file ->
                    responseBuilder.addFile(file)
                }
            }
        }

        return responseBuilder.build()
    }

    class FileBuilder(val options: GrpcStubExtsGenOptions) {

        fun buildFile(service: ProtoService): PluginProtos.CodeGeneratorResponse.File? = with(service) {
            if (!options.filter.matches(protoFile.name))
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
                addFunctions(buildDefaultStubExts())

                if (options.supportCoroutines) {
                    addFunctions(buildStubCoroutineExts())
                    addFunctions(buildClientStubRpcRequestOverloads())
                }
            }

            return fileSpecBuilder
                .build()
                .takeIf { it.members.isNotEmpty() }
                ?.toResponseFileProto()
        }

        private fun ProtoService.buildDefaultStubExts(): List<FunSpec> =
            methodDefinitions
                .filter { it.isUnary }
                .flatMap { it.buildUnaryDefaultOverloads() }

        private fun ProtoService.buildStubCoroutineExts(): List<FunSpec> =
            methodDefinitions.map { method ->
                when (method.type) {
                    MethodType.UNARY -> method.buildStubUnaryMethod()
                    MethodType.CLIENT_STREAMING -> method.buildStubClientStreamingMethod()
                    MethodType.SERVER_STREAMING -> method.buildStubServerStreamingMethod()
                    MethodType.BIDI_STREAMING -> method.buildStubBidiStreamingMethod()
                    MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
                }
            }

        private fun ProtoService.buildClientStubRpcRequestOverloads(): List<FunSpec> =
            methodDefinitions.mapNotNull {
                when {

                    it.type == MethodType.UNARY && it.isNotEmptyInput ->
                        it.buildUnaryCoroutineExtOverload()

                    it.type == MethodType.SERVER_STREAMING ->
                        it.buildStubServerStreamingMethodOverload()

                    else -> null
                }
            }


        private fun ProtoMethod.buildUnaryDefaultOverloads(): List<FunSpec> {

            val funSpecBuilder = newDefaultUnaryExtBuilder()
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

            return funSpecs
        }

        private fun ProtoMethod.newDefaultUnaryExtBuilder(): FunSpec.Builder =
            FunSpec.builder(functionName)
                .addModifiers(KModifier.INLINE)
                .addCode(requestClassName.requestValueBuilderCodeBlock)
                .addStatement("return %N(request)", functionName)
                .addParameter("block", requestClassName.builderLambdaTypeName)

        private fun ProtoMethod.buildUnaryCoroutineExtOverload(): FunSpec =
            FunSpec.builder(functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(protoService.asyncStubClassName)
                .addModifiers(KModifier.INLINE)
                .addParameter("block", requestClassName.builderLambdaTypeName)
                .returns(responseClassName)
                .addCode(requestClassName.requestValueBuilderCodeBlock)
                .addStatement("return %N(request)", functionName)
                .build()

        private fun ProtoMethod.buildStubServerStreamingMethodOverload(): FunSpec =
            FunSpec.builder(functionName)
                .receiver(protoService.asyncStubClassName)
                .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
                .addModifiers(KModifier.INLINE)
                .addParameter("block", requestClassName.builderLambdaTypeName)
                .addCode(requestClassName.requestValueBuilderCodeBlock)
                .addStatement("return %N(request)", functionName)
                .build()

        private fun ProtoMethod.buildStubUnaryMethod(): FunSpec =
            FunSpec.builder(functionName)
                .addModifiers(KModifier.SUSPEND)
                .returns(responseClassName)
                .receiver(protoService.asyncStubClassName)
                .addParameter(requestClassName.requestParamSpec)
                .addStatement(
                    "return %T(request, %T.%N())",
                    CommonClassNames.ClientCalls.clientCallUnary,
                    protoService.enclosingServiceClassName,
                    methodDefinitionGetterName
                )
                .build()

        private fun ProtoMethod.buildStubClientStreamingMethod(): FunSpec =
            FunSpec.builder(functionName)
                .addAnnotation(CommonClassNames.obsoleteCoroutinesApi)
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


        private fun ProtoMethod.buildStubServerStreamingMethod(): FunSpec =
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

        private fun ProtoMethod.buildStubBidiStreamingMethod(): FunSpec =
            FunSpec.builder(functionName)
                .addAnnotation(CommonClassNames.obsoleteCoroutinesApi)
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