package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.CommonPackages
import com.github.marcoferrer.krotoplus.utils.matches
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*
import java.lang.IllegalStateException


object GrpcCoroutinesGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcCoroutinesList.isNotEmpty()

    private const val serviceDelegateName = "ServiceDelegate"

    private val ProtoService.outerObjectName: String
        get() = "${name}CoroutineGrpc"

    private val ProtoService.baseImplName: String
        get() = "${name}CoroutineImplBase"

    private val ProtoService.serviceDelegateClassName
        get() = ClassName(protoFile.javaPackage, outerObjectName, baseImplName, serviceDelegateName)

    private val ProtoService.serviceJavaBaseImplClassName
        get() = enclosingServiceClassName.nestedClass("${name}ImplBase")


    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (service in context.schema.protoServices) {
            for (options in context.config.grpcStubExtsList) {

                if (options.filter.matches(service.protoFile.name)) {
                    service.buildGrpcFileSpec()?.let {
                        responseBuilder.addFile(it.toResponseFileProto())
                    }
                }
            }
        }

        return responseBuilder.build()
    }

    private fun ProtoService.buildGrpcFileSpec(): FileSpec? {

        val fileSpecBuilder = FileSpec
            .builder(protoFile.javaPackage, outerObjectName)
            .addComment(AutoGenerationDisclaimer)
            .addType(buildOuterObject())

        return fileSpecBuilder.build()
            .takeIf { it.members.isNotEmpty() }
    }

    private fun ProtoService.buildOuterObject(): TypeSpec =
        TypeSpec.objectBuilder(outerObjectName)
            .addType(buildServiceBaseImpl())
            .build()

    private fun ProtoService.buildServiceBaseImpl(): TypeSpec {

        val delegateValName = "delegate"

        val baseImplBuilder = TypeSpec.classBuilder(baseImplName)
            .addModifiers(KModifier.ABSTRACT)
            .addSuperinterface(CommonClassNames.bindableService)
            .addSuperinterface(CommonClassNames.coroutineScope)
            .addProperty(
                PropertySpec
                    .builder("coroutineContext", CommonClassNames.coroutineContext)
                    .addModifiers(KModifier.OVERRIDE)
                    .addAnnotation(CommonClassNames.experimentalCoroutinesApi)
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode("return %T.Unconfined", CommonClassNames.dispatchers)
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec
                    .builder(delegateValName, serviceDelegateClassName)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T()", serviceDelegateClassName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("bindService")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(ClassName("io.grpc", "ServerServiceDefinition"))
                    .addCode("return %N.bindService()", delegateValName)
                    .build()
            )
            .addFunctions(buildBaseImplRpcMethods())
            .addFunctions(buildResponseLambdaOverloads())
            .addType(buildServiceBaseImplDelegate())

        return baseImplBuilder.build()
    }


    fun ProtoService.buildServiceBaseImplDelegate(): TypeSpec =
        TypeSpec.classBuilder(serviceDelegateName)
            .addModifiers(KModifier.PRIVATE, KModifier.INNER)
            .superclass(serviceJavaBaseImplClassName)
            .addFunctions(buildBaseImplRpcMethodDelegates())
            .build()

    fun ProtoMethod.buildUnaryBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                ParameterSpec.builder("request", requestClassName)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "completableResponse", ParameterizedTypeName
                        .get(CommonClassNames.completableDeferred, responseClassName)
                )
                    .build()
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),completableResponse)",
                        CommonClassNames.ServerCalls.serverCallUnimplementedUnary,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()


    fun ProtoMethod.buildUnaryBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("request", requestClassName)
            .addParameter(
                "responseObserver", ParameterizedTypeName
                    .get(CommonClassNames.streamObserver, responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(responseObserver) { completableResponse ->",
                        CommonClassNames.ServerCalls.serverCallUnary
                    )
                    .indent()
                    .addStatement("%N(request, completableResponse)", functionName)
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()

    fun ProtoMethod.buildClientStreamingBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                ParameterSpec.builder(
                    "requestChannel", ParameterizedTypeName
                        .get(CommonClassNames.receiveChannel, requestClassName)
                )
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "completableResponse", ParameterizedTypeName
                        .get(CommonClassNames.completableDeferred, responseClassName)
                )
                    .build()
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),completableResponse)",
                        CommonClassNames.ServerCalls.serverCallUnimplementedUnary,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()

    fun ProtoMethod.buildClientStreamingMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .returns(ParameterizedTypeName.get(CommonClassNames.streamObserver, requestClassName))
            .addParameter(
                "responseObserver", ParameterizedTypeName
                    .get(CommonClassNames.streamObserver, responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "val requestObserver = %T(responseObserver) { requestChannel: %T, completableResponse ->",
                        CommonClassNames.ServerCalls.serverCallClientStreaming,
                        ParameterizedTypeName.get(CommonClassNames.receiveChannel, requestClassName)
                    )
                    .indent()
                    .addStatement("%N(requestChannel, completableResponse)", functionName)
                    .unindent()
                    .addStatement("}")
                    .addStatement("return requestObserver")
                    .build()
            )
            .build()

    fun ProtoMethod.buildServerStreamingBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                ParameterSpec.builder("request", requestClassName)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "responseChannel", ParameterizedTypeName
                        .get(CommonClassNames.sendChannel, responseClassName)
                ).build()
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),responseChannel)",
                        CommonClassNames.ServerCalls.serverCallUnimplementedStream,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()


    fun ProtoMethod.buildServerStreamingMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(CommonClassNames.obsoleteCoroutinesApi)
            .addParameter("request", requestClassName)
            .addParameter(
                "responseObserver", ParameterizedTypeName
                    .get(CommonClassNames.streamObserver, responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(responseObserver) { responseChannel: %T ->",
                        CommonClassNames.ServerCalls.serverCallServerStreaming,
                        ParameterizedTypeName.get(CommonClassNames.sendChannel, responseClassName)
                    )
                    .indent()
                    .addStatement("%N(request, responseChannel)", functionName)
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()

    fun ProtoMethod.buildBidiBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter(
                ParameterSpec.builder("requestChannel", ParameterizedTypeName
                    .get(CommonClassNames.receiveChannel, requestClassName))
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "responseChannel", ParameterizedTypeName
                        .get(CommonClassNames.sendChannel, responseClassName)
                ).build()
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),responseChannel)",
                        CommonClassNames.ServerCalls.serverCallUnimplementedStream,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()


    fun ProtoMethod.buildBidiMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(CommonClassNames.obsoleteCoroutinesApi)
            .returns(ParameterizedTypeName.get(CommonClassNames.streamObserver, requestClassName))
            .addParameter(
                "responseObserver", ParameterizedTypeName
                    .get(CommonClassNames.streamObserver, responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "val requestChannel = %T(responseObserver) { requestChannel: %T, responseChannel: %T ->",
                        CommonClassNames.ServerCalls.serverCallBidiStreaming,
                        ParameterizedTypeName.get(CommonClassNames.receiveChannel, requestClassName),
                        ParameterizedTypeName.get(CommonClassNames.sendChannel, responseClassName)
                    )
                    .indent()
                    .addStatement("%N(requestChannel, responseChannel)", functionName)
                    .unindent()
                    .addStatement("}")
                    .addStatement("return requestChannel")
                    .build()
            )
            .build()

    fun ProtoService.buildBaseImplRpcMethods(): List<FunSpec> =
        methodDefinitions.map { method ->
            when {
                method.isUnary -> method.buildUnaryBaseImpl()
                method.isClientStream -> method.buildClientStreamingBaseImpl()
                method.isServerStream -> method.buildServerStreamingBaseImpl()
                method.isBidi -> method.buildBidiBaseImpl()
                else -> throw IllegalStateException("Unknown method type")
            }
        }

    fun ProtoService.buildBaseImplRpcMethodDelegates(): List<FunSpec> =
        methodDefinitions.map { method ->
            when {
                method.isUnary -> method.buildUnaryBaseImplDelegate()
                method.isClientStream -> method.buildClientStreamingMethodBaseImplDelegate()
                method.isServerStream -> method.buildServerStreamingMethodBaseImplDelegate()
                method.isBidi -> method.buildBidiMethodBaseImplDelegate()
                else -> throw IllegalStateException("Unknown method type")
            }
        }

    fun ProtoService.buildResponseLambdaOverloads(): List<FunSpec> =
        methodDefinitions.partition { it.isUnary || it.isClientStream }
            .let { (completableResponseMethods, streamingResponseMethods) ->

                mutableListOf<FunSpec>().apply {
                    streamingResponseMethods
                        .distinctBy { it.responseType }
                        .mapTo(this) { it.buildChannelLambdaExt() }

                    completableResponseMethods
                        .distinctBy { it.responseType }
                        .mapTo(this) { it.buildCompletableDeferredLambdaExt() }
                }
            }

    fun ProtoMethod.buildCompletableDeferredLambdaExt(): FunSpec {

        val receiverClassName = ParameterizedTypeName
            .get(CommonClassNames.completableDeferred, responseClassName)

        val jvmNameSuffix = responseType.canonicalJavaName
            .replace(responseType.javaPackage.orEmpty(), "")
            .replace(".", "")

        return FunSpec.builder("complete")
            .addModifiers(KModifier.INLINE)
            .receiver(receiverClassName)
            .addParameter(
                "block", LambdaTypeName.get(
                    receiver = (responseType as ProtoMessage).builderClassName,
                    returnType = UNIT
                )
            )
            .returns(BOOLEAN)
            .addAnnotation(
                AnnotationSpec
                    .builder(JvmName::class.asClassName())
                    .addMember("\"complete$jvmNameSuffix\"")
                    .build()
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement("val response = %T.newBuilder().apply(block).build()", responseClassName)
                    .addStatement("return complete(response)")
                    .build()
            )
            .build()
    }

    fun ProtoMethod.buildChannelLambdaExt(): FunSpec {

        val receiverClassName = ParameterizedTypeName
            .get(CommonClassNames.sendChannel, responseClassName)

        val jvmNameSuffix = responseType.canonicalJavaName
            .replace(responseType.javaPackage.orEmpty(), "")
            .replace(".", "")

        return FunSpec.builder("send")
            .addModifiers(KModifier.INLINE, KModifier.SUSPEND)
            .receiver(receiverClassName)
            .addParameter(
                "block", LambdaTypeName.get(
                    receiver = (responseType as ProtoMessage).builderClassName,
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
                    .addStatement("val response = %T.newBuilder().apply(block).build()", responseClassName)
                    .addStatement("send(response)")
                    .build()
            )
            .build()
    }
}


