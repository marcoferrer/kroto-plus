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

import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.generators.builders.GrpcServiceBaseImplBuilder
import com.github.marcoferrer.krotoplus.generators.builders.GrpcStubBuilder
import com.github.marcoferrer.krotoplus.generators.builders.outerObjectName
import com.github.marcoferrer.krotoplus.generators.builders.stubClassName
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.proto.ProtoMethod
import com.github.marcoferrer.krotoplus.proto.ProtoService
import com.github.marcoferrer.krotoplus.proto.getGeneratedAnnotationSpec
import com.github.marcoferrer.krotoplus.utils.CommonClassNames
import com.github.marcoferrer.krotoplus.utils.addFunctions
import com.github.marcoferrer.krotoplus.utils.builderLambdaTypeName
import com.github.marcoferrer.krotoplus.utils.requestValueBuilderCodeBlock
import com.github.marcoferrer.krotoplus.utils.toUpperCamelCase
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.grpc.MethodDescriptor.MethodType


object GrpcCoroutinesGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.grpcCoroutinesList.isNotEmpty()

    private val stubBuilder = GrpcStubBuilder(context)

    private val serviceBaseImplBuilder = GrpcServiceBaseImplBuilder(context)

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        val sendChannelMessageTypes = mutableListOf<ProtoMessage>()

        for (service in context.schema.protoServices) {

            for (options in context.config.grpcCoroutinesList) {

                if (isFileToGenerate(service.protoFile.name,options.filter)) {
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
            .addKdoc(attachedComments)
            .addAnnotation(protoFile.getGeneratedAnnotationSpec())
            .addFunction(
                FunSpec.builder("newStub")
                    .returns(stubClassName)
                    .addParameter("channel",CommonClassNames.grpcChannel)
                    .addCode("return %T.newStub(channel)",stubClassName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("newStubWithContext")
                    .returns(stubClassName)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("channel", CommonClassNames.grpcChannel)
                    .addCode("return %T.newStubWithContext(channel)", stubClassName)
                    .build()
            )
            .addType(stubBuilder.buildStub(this))
            .addType(serviceBaseImplBuilder.build(this))
            .addProperty(
                PropertySpec.builder("SERVICE_NAME", String::class.asClassName())
                    .addModifiers(KModifier.CONST)
                    .initializer("%T.SERVICE_NAME", enclosingServiceClassName)
                    .build()
            )
            .addProperties(buildMethodDefinitionProps())
            .build()

    private fun ProtoService.buildMethodDefinitionProps(): List<PropertySpec> =
        methodDefinitions.map { method ->
            val propName = "${method.descriptorProto.name.toUpperCamelCase().decapitalize()}Method"
            val propTypeName = CommonClassNames.grpcMethodDescriptor
                .parameterizedBy(method.requestClassName, method.responseClassName)
            val propGetter = FunSpec.getterBuilder()
                .addStatement("return %T.%N()", enclosingServiceClassName, method.methodDefinitionGetterName)
                .build()

            PropertySpec.builder(propName, propTypeName)
                .addAnnotation(JvmStatic::class.asClassName())
                .addAnnotation(method.buildRpcMethodAnnotation())
                .getter(propGetter)
                .build()
        }

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

    private fun List<ProtoMessage>.buildSendChannelExtFiles(): List<FileSpec> =
        distinct()
            .groupBy( { it.protoFile }, { it.buildSendChannelLambdaExt() })
            .map { (protoFile, funSpecs) ->

                FileSpec.builder(protoFile.javaPackage,"${protoFile.javaOuterClassname}GrpcExts")
                    .addFunctions(funSpecs)
                    .build()
            }

    private fun ProtoMethod.buildRpcMethodAnnotation(): AnnotationSpec =
        AnnotationSpec.builder(CommonClassNames.grpcStubRpcMethod)
            .addMember("fullMethodName = \"\$SERVICE_NAME/${descriptorProto.name}\"")
            .addMember("requestType = %T::class", requestClassName)
            .addMember("responseType = %T::class", responseClassName)
            .addMember("methodType = %T.%N", MethodType::class, type.name)
            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
            .build()

}

private fun ProtoService.getSendChannelMessageTypes(): List<ProtoMessage> =
    methodDefinitions
        .asSequence()
        .filter { it.isClientStream || it.isBidi }
        .map { it.requestType }
        .distinct()
        .toList()