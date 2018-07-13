package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.schema.ProtoMessage
import com.google.protobuf.compiler.PluginProtos

class ExtendableMessagesGenerator(override val context: Generator.Context) : Generator {

    override val key = "extendable-messages"

    private val companionFieldName = getOption("companion_field_name") ?: "Companion"

    private val companionClassName = getOption("companion_class_name") ?: "Companion"

    override fun invoke(responseBuilder: PluginProtos.CodeGeneratorResponse.Builder) =
        context.schema.types.values
                .asSequence()
                .filter { it is ProtoMessage && !it.cannonicalProtoName.startsWith("google")}
                .forEach { protoMessage ->

                    val canonicalKrotoMessageType = "com.github.marcoferrer.krotoplus.KrotoMessage"

                    responseBuilder.addFile(
                            PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                    .setName(protoMessage.outputFilePath)
                                    .setInsertionPoint("message_implements:${protoMessage.cannonicalProtoName}")
                                    .setContent("$canonicalKrotoMessageType,")
                                    .build())

                    responseBuilder.addFile(
                            PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                    .setName(protoMessage.outputFilePath)
                                    .setInsertionPoint("builder_implements:${protoMessage.cannonicalProtoName}")
                                    .setContent("$canonicalKrotoMessageType.Builder<${protoMessage.name}>,")
                                    .build())

                    responseBuilder.addFile(
                            PluginProtos.CodeGeneratorResponse.File.newBuilder()
                                    .setName(protoMessage.outputFilePath)
                                    .setInsertionPoint("class_scope:${protoMessage.cannonicalProtoName}")
                                    .setContent("""
                        public static final $companionClassName $companionFieldName = new $companionClassName();

                        @javax.annotation.Nonnull
                        @Override
                        public ${protoMessage.name}.$companionClassName getCompanion() {
                          return ${protoMessage.name}.$companionFieldName;
                        }

                        public static final class $companionClassName implements
                            $canonicalKrotoMessageType.Companion<${protoMessage.name},${protoMessage.name}.Builder> {

                          private $companionClassName(){}

                          @javax.annotation.Nonnull
                          @Override
                          public ${protoMessage.name} getDefaultInstance() {
                            return ${protoMessage.name}.getDefaultInstance();
                          }

                          @javax.annotation.Nonnull
                          @Override
                          public ${protoMessage.name}.Builder newBuilder() {
                            return ${protoMessage.name}.newBuilder();
                          }
                        }
                                    """.trimIndent())
                                    .build())
                }
}