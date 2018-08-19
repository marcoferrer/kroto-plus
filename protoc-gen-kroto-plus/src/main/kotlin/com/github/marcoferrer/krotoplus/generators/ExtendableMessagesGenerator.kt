package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.utils.matches
import com.google.protobuf.compiler.PluginProtos

object ExtendableMessagesGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.extendableMessagesCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()
        context.schema.protoTypes.values.asSequence()
                .map { it as? ProtoMessage }
                .filterNotNull().filterNot { it.isMapEntry }
                .forEach { protoMessage ->

                    for (options in context.config.extendableMessagesList) {

                        if(!options.filter.matches(protoMessage.protoFile.name) || protoMessage.isMapEntry)
                            continue

                        val kpPackage = "com.github.marcoferrer.krotoplus.message"
                        val companionExtends = options.companionExtends?.takeIf { it.isNotEmpty() }
                                ?.let { "extends ${it.replace("{{message_type}}", protoMessage.canonicalJavaName)}\n" }
                                .orEmpty()

                        val companionImplements = options.companionImplements?.takeIf { it.isNotEmpty() }
                                ?.let { it.replace("{{message_type}}", protoMessage.canonicalJavaName) + "," }
                                .orEmpty()

                        val companionFieldName = options.companionFieldName
                                ?.takeIf { it.isNotEmpty() } ?: "KpCompanion"

                        val companionClassName = options.companionClassName
                                ?.takeIf { it.isNotEmpty() } ?: "Companion"

                        responseBuilder.addFile(
                                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                        .setName(protoMessage.outputFilePath)
                                        .setInsertionPoint("message_implements:${protoMessage.canonicalProtoName}")
                                        .setContent("$kpPackage.KpMessage<${protoMessage.canonicalJavaName},${protoMessage.canonicalJavaName}.Builder>,")
                                        .build())

                        responseBuilder.addFile(
                                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                        .setName(protoMessage.outputFilePath)
                                        .setInsertionPoint("builder_implements:${protoMessage.canonicalProtoName}")
                                        .setContent("$kpPackage.KpBuilder<${protoMessage.canonicalJavaName}>,")
                                        .build())

                        responseBuilder.addFile(
                                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                        .setName(protoMessage.outputFilePath)
                                        .setInsertionPoint("class_scope:${protoMessage.canonicalProtoName}")
                                        .setContent(
                                                """
                            @org.jetbrains.annotations.NotNull
                            public static final $companionClassName $companionFieldName =
                                    $kpPackage.KpCompanion.Registry
                                            .initializeCompanion(${protoMessage.canonicalJavaName}.class, new $companionClassName());

                            @org.jetbrains.annotations.NotNull
                            @Override
                            public ${protoMessage.canonicalJavaName}.$companionClassName getCompanion() {
                              return ${protoMessage.canonicalJavaName}.$companionFieldName;
                            }

                            public static final class $companionClassName
                                $companionExtends implements
                                $companionImplements $kpPackage.KpCompanion<${protoMessage.canonicalJavaName},${protoMessage.canonicalJavaName}.Builder> {

                              private $companionClassName(){
                                ${if(companionExtends.isNotEmpty()) "super()" else ""}
                              }

                              @org.jetbrains.annotations.NotNull
                              @Override
                              public ${protoMessage.canonicalJavaName} getDefaultInstance() {
                                return ${protoMessage.canonicalJavaName}.getDefaultInstance();
                              }

                              @org.jetbrains.annotations.NotNull
                              @Override
                              public ${protoMessage.canonicalJavaName}.Builder newBuilder() {
                                return ${protoMessage.canonicalJavaName}.newBuilder();
                              }
                            }
                        """.trimIndent()).build())
                    }
                }

        return responseBuilder.build()
    }
}