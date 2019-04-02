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

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.*
import com.github.marcoferrer.krotoplus.utils.*
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*
import io.grpc.MethodDescriptor.*
import java.lang.IllegalStateException


object GrpcCoroutinesGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcCoroutinesList.isNotEmpty()

    private const val serviceDelegateName = "ServiceDelegate"

    private val ProtoService.outerObjectName: String
        get() = "${name}CoroutineGrpc"

    private val ProtoService.baseImplName: String
        get() = "${name}ImplBase"

    private val ProtoService.serviceDelegateClassName: ClassName
        get() = ClassName(protoFile.javaPackage, outerObjectName, baseImplName, serviceDelegateName)

    private val ProtoService.serviceJavaBaseImplClassName: ClassName
        get() = enclosingServiceClassName.nestedClass("${name}ImplBase")

    private val ProtoService.stubName: String
        get() = "${name}CoroutineStub"

    private val ProtoService.stubClassName: ClassName
            get() = ClassName(protoFile.javaPackage, outerObjectName, stubName)


    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        val sendChannelMessageTypes = mutableListOf<ProtoMessage>()

        for (service in context.schema.protoServices) {

            for (options in context.config.grpcCoroutinesList) {

                if (options.filter.matches(service.protoFile.name)) {
                    service.buildGrpcFileSpec()?.let {
                        responseBuilder.addFile(it.toResponseFileProto())
                    }


                    sendChannelMessageTypes += service.getSendChannelMessageTypes()
                }
            }
        }

        sendChannelMessageTypes
            .buildSendChannelExtFiles()
            .forEach {
                responseBuilder.addFile(it.toResponseFileProto())
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
            .addAnnotation(protoFile.getGeneratedAnnotationSpec())
            .addFunction(buildNewStubMethod())
            .addType(buildClientStubImpl())
            .addType(buildServiceBaseImpl())
            .addProperty(
                PropertySpec.builder("SERVICE_NAME", String::class.asClassName())
                    .addModifiers(KModifier.CONST)
                    .initializer("%T.SERVICE_NAME", enclosingServiceClassName)
                    .build()
            )
            .build()

    private fun ProtoService.buildServiceBaseImpl(): TypeSpec {

        val delegateValName = "delegate"

        val baseImplBuilder = TypeSpec.classBuilder(baseImplName)
            .addModifiers(KModifier.ABSTRACT)
            .addSuperinterface(CommonClassNames.bindableService)
            .addSuperinterface(CommonClassNames.serviceScope)
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
                    .returns(CommonClassNames.grpcServerServiceDefinition)
                    .addCode("return %N.bindService()", delegateValName)
                    .build()
            )
            .addFunctions(buildBaseImplRpcMethods())
            .addFunctions(buildResponseLambdaOverloads())
            .addType(buildServiceBaseImplDelegate())

        return baseImplBuilder.build()
    }

    private fun ProtoService.buildServiceBaseImplDelegate(): TypeSpec =
        TypeSpec.classBuilder(serviceDelegateName)
            .addModifiers(KModifier.PRIVATE, KModifier.INNER)
            .superclass(serviceJavaBaseImplClassName)
            .addFunctions(buildBaseImplRpcMethodDelegates())
            .build()

    private fun ProtoMethod.buildUnaryBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
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


    private fun ProtoMethod.buildUnaryBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("request", requestClassName)
            .addParameter(
                name = "responseObserver",
                type = CommonClassNames.streamObserver.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),responseObserver) {",
                        CommonClassNames.ServerCalls.serverCallUnary,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    )
                    .indent()
                    .addStatement("%N(request)", functionName)
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()

    private fun ProtoMethod.buildClientStreamingBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
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

    private fun ProtoMethod.buildClientStreamingMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .returns(CommonClassNames.streamObserver.parameterizedBy(requestClassName))
            .addParameter(
                name ="responseObserver",
                type = CommonClassNames.streamObserver.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "val requestObserver = %T(%T.%N(),responseObserver) { requestChannel: %T ->",
                        CommonClassNames.ServerCalls.serverCallClientStreaming,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName,
                        CommonClassNames.receiveChannel.parameterizedBy(requestClassName)
                    )
                    .indent()
                    .addStatement("%N(requestChannel)", functionName)
                    .unindent()
                    .addStatement("}")
                    .addStatement("return requestObserver")
                    .build()
            )
            .build()

    private fun ProtoMethod.buildServerStreamingBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.OPEN)
            .addParameter("request", requestClassName)
            .addParameter(
                name = "responseChannel",
                type = CommonClassNames.sendChannel.parameterizedBy(responseClassName)
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


    private fun ProtoMethod.buildServerStreamingMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("request", requestClassName)
            .addParameter(
                name = "responseObserver",
                type = CommonClassNames.streamObserver.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T(%T.%N(),responseObserver) { responseChannel: %T ->",
                        CommonClassNames.ServerCalls.serverCallServerStreaming,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName,
                        CommonClassNames.sendChannel.parameterizedBy(responseClassName)
                    )
                    .indent()
                    .addStatement("%N(request, responseChannel)", functionName)
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .build()

    private fun ProtoMethod.buildBidiBaseImpl(): FunSpec =
        FunSpec.builder(functionName)
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
                    .addStatement(
                        "%T(%T.%N(),responseChannel)",
                        CommonClassNames.ServerCalls.serverCallUnimplementedStream,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName
                    ).build()
            )
            .build()


    private fun ProtoMethod.buildBidiMethodBaseImplDelegate(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .returns(CommonClassNames.streamObserver.parameterizedBy(requestClassName))
            .addParameter(
                name = "responseObserver",
                type = CommonClassNames.streamObserver.parameterizedBy(responseClassName)
            )
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "val requestChannel = %T(%T.%N(),responseObserver) { requestChannel: %T, responseChannel: %T ->",
                        CommonClassNames.ServerCalls.serverCallBidiStreaming,
                        protoService.enclosingServiceClassName,
                        methodDefinitionGetterName,
                        CommonClassNames.receiveChannel.parameterizedBy(requestClassName),
                        CommonClassNames.sendChannel.parameterizedBy(responseClassName)
                    )
                    .indent()
                    .addStatement("%N(requestChannel, responseChannel)", functionName)
                    .unindent()
                    .addStatement("}")
                    .addStatement("return requestChannel")
                    .build()
            )
            .build()

    private fun ProtoService.buildBaseImplRpcMethods(): List<FunSpec> =
        methodDefinitions.map { method ->
            when(method.type){
                MethodType.UNARY -> method.buildUnaryBaseImpl()
                MethodType.CLIENT_STREAMING -> method.buildClientStreamingBaseImpl()
                MethodType.SERVER_STREAMING -> method.buildServerStreamingBaseImpl()
                MethodType.BIDI_STREAMING -> method.buildBidiBaseImpl()
                MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
            }
        }

    private fun ProtoService.buildBaseImplRpcMethodDelegates(): List<FunSpec> =
        methodDefinitions.map { method ->
            when(method.type){
                MethodType.UNARY -> method.buildUnaryBaseImplDelegate()
                MethodType.CLIENT_STREAMING -> method.buildClientStreamingMethodBaseImplDelegate()
                MethodType.SERVER_STREAMING -> method.buildServerStreamingMethodBaseImplDelegate()
                MethodType.BIDI_STREAMING -> method.buildBidiMethodBaseImplDelegate()
                MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
            }
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

    private fun ProtoService.buildClientStubImpl(): TypeSpec {

        val paramNameChannel = "channel"
        val paramNameCallOptions = "callOptions"

        return TypeSpec.classBuilder(stubName)
            .superclass(CommonClassNames.grpcAbstractStub.parameterizedBy(stubClassName))
            .addSuperclassConstructorParameter(paramNameChannel)
            .addSuperclassConstructorParameter(paramNameCallOptions)
            .primaryConstructor(FunSpec
                .constructorBuilder()
                .addModifiers(KModifier.PRIVATE)
                .addParameter(paramNameChannel,CommonClassNames.grpcChannel)
                .addParameter(
                    ParameterSpec
                        .builder(paramNameCallOptions,CommonClassNames.grpcCallOptions)
                        .defaultValue("%T.DEFAULT",CommonClassNames.grpcCallOptions)
                        .build()
                )
                .build()
            )
            .addFunction(FunSpec
                .builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(paramNameChannel, CommonClassNames.grpcChannel )
                .addParameter(paramNameCallOptions, CommonClassNames.grpcCallOptions )
                .returns(stubClassName)
                .addStatement("return %T(channel,callOptions)",stubClassName)
                .build()
            )
            .addFunctions(buildClientStubRpcMethods())
            .addFunctions(buildClientStubRpcRequestOverloads())
            .addType(buildClientStubCompanion())
            .build()

    }

    private fun ProtoService.buildClientStubCompanion(): TypeSpec =
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
            .build()

    private fun ProtoService.buildNewStubMethod(): FunSpec =
        FunSpec.builder("newStub")
            .returns(stubClassName)
            .addParameter("channel",CommonClassNames.grpcChannel)
            .addCode("return %T.newStub(channel)",stubClassName)
            .build()

    private fun ProtoService.buildClientStubRpcMethods(): List<FunSpec> =
        methodDefinitions.map { method ->
            when(method.type){
                MethodType.UNARY -> method.buildStubUnaryMethod()
                MethodType.CLIENT_STREAMING -> method.buildStubClientStreamingMethod()
                MethodType.SERVER_STREAMING -> method.buildStubServerStreamingMethod()
                MethodType.BIDI_STREAMING -> method.buildStubBidiStreamingMethod()
                MethodType.UNKNOWN -> throw IllegalStateException("Unknown method type")
            }
        }

    private fun ProtoMethod.buildStubBidiStreamingMethod(): FunSpec =
        FunSpec.builder(functionName)
            .addAnnotation(buildRpcMethodAnnotation())
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

    private fun ProtoMessage.buildSendChannelLambdaExt(suffix: String = ""): FunSpec {

        val jvmNameSuffix = canonicalJavaName
            .replace(javaPackage.orEmpty(), "")
            .replace(".", "") + suffix

        return FunSpec.builder("send")
            .addModifiers(KModifier.INLINE, KModifier.SUSPEND)
            .addAnnotation(
                AnnotationSpec
                    .builder(JvmName::class.asClassName())
                    .addMember("\"send$jvmNameSuffix\"")
                    .build()
            )
            .receiver(CommonClassNames.sendChannel.parameterizedBy(className))
            .addParameter("block", className.builderLambdaTypeName)
            .addCode(className.requestValueBuilderCodeBlock)
            .addStatement("send(request)")
            .build()
    }

    fun ProtoService.buildSendChannelOverloads(): List<FunSpec> =
        getSendChannelMessageTypes()
            .map { it.buildSendChannelLambdaExt() }

    private fun ProtoService.getSendChannelMessageTypes(): List<ProtoMessage> =
        methodDefinitions
            .asSequence()
            .filter { it.isClientStream || it.isBidi }
            .distinctBy { it.requestType }
            .mapNotNull { (it.requestType as? ProtoMessage) }
            .toList()

    private fun List<ProtoMessage>.buildSendChannelExtFiles(): List<FileSpec> =
        distinct()
            .groupBy( { it.protoFile }, { it.buildSendChannelLambdaExt() })
            .map { (protoFile, funSpecs) ->

                FileSpec.builder(protoFile.javaPackage,"${protoFile.javaOuterClassname}GrpcExts")
                    .addFunctions(funSpecs)
                    .build()
            }

    private fun ProtoService.buildClientStubRpcRequestOverloads(): List<FunSpec> =
        methodDefinitions.mapNotNull {
            when(it.type){
                MethodType.UNARY -> it.buildStubUnaryMethodOverload()
                MethodType.SERVER_STREAMING -> it.buildStubServerStreamingMethodOverload()
                else -> null
            }
        }

    private fun ProtoMethod.buildRpcMethodAnnotation(): AnnotationSpec =
        AnnotationSpec.builder(CommonClassNames.grpcStubRpcMethod)
            .addMember("fullMethodName = \"\$SERVICE_NAME/${descriptorProto.name}\"")
            .addMember("requestType = %T::class", requestClassName)
            .addMember("responseType = %T::class", responseClassName)
            .addMember("methodType = %T.%N", MethodType::class, type.name)
            .build()

    private fun ProtoMethod.buildStubUnaryMethod(): FunSpec =
        FunSpec.builder(functionName)
            .addAnnotation(buildRpcMethodAnnotation())
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

    private fun ProtoMethod.buildStubUnaryMethodOverload(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.INLINE)
            .returns(responseClassName)
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)",functionName)
            .build()


    private fun ProtoMethod.buildStubServerStreamingMethodOverload(): FunSpec =
        FunSpec.builder(functionName)
            .addModifiers(KModifier.INLINE)
            .returns(CommonClassNames.receiveChannel.parameterizedBy(responseClassName))
            .addParameter("block", requestClassName.builderLambdaTypeName)
            .addCode(requestClassName.requestValueBuilderCodeBlock)
            .addStatement("return %N(request)",functionName)
            .build()

    private fun ProtoMethod.buildStubClientStreamingMethod(): FunSpec =
        FunSpec.builder(functionName)
            .addAnnotation(buildRpcMethodAnnotation())
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
            .addAnnotation(buildRpcMethodAnnotation())
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

