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
import com.github.marcoferrer.krotoplus.generators.builders.ServerStreamingStubExtsBuilder
import com.github.marcoferrer.krotoplus.generators.builders.UnaryStubExtsBuilder
import com.github.marcoferrer.krotoplus.generators.builders.addResponseObserverParamerter
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
                            addFunctions(unaryExtBuilder.buildStubExts(method, options))

                        MethodType.SERVER_STREAMING ->
                            addFunctions(serverStreamingExtBuilder.buildStubExts(method, options))

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

