package com.github.mferrer.krotoplus.generators

import com.github.mferrer.krotoplus.generators.FileSpecProducer.Companion.AutoGenerationDisclaimer
import com.github.mferrer.krotoplus.schema.*
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch

class ProtoTypeBuilderGenerator(
        override val schema: Schema, override val fileSpecChannel: Channel<FileSpec>
) : SchemaConsumer, FileSpecProducer {

    override fun consume() = launch {
        schema.protoFiles()
                .asSequence()
                .filterNot { it.isCommonProtoFile }
                .forEach { protoFile ->
                    launch(coroutineContext) {
                        buildFileSpec(protoFile)
                    }
                }
    }

    private suspend fun buildFileSpec(protoFile: ProtoFile){
        val filename = "${protoFile.javaOuterClassname}Builders"
        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage(),filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        val typeSpecBuilder = TypeSpec.objectBuilder(filename)
                .addAnnotation(protoFile.getGeneratedAnnotationSpec())

        protoFile.types()
                .asSequence()
                .map{ it.type() }
                .forEach { protoType ->

                    val typeClassName = protoType.toClassName(protoFile)

                    FunSpec.builder(protoType.simpleName())
                            .addModifiers(KModifier.INLINE)
                            .addParameter("block",LambdaTypeName.get(
                                receiver = typeClassName.nestedClass("Builder"),
                                returnType = UNIT))
                            .addStatement("return %T.newBuilder().apply(block).build()", typeClassName)
                            .also {
                                typeSpecBuilder.addFunction(it.build())
                            }
                }

        typeSpecBuilder.build()
                .takeIf { it.funSpecs.isNotEmpty() }
                ?.let { typeSpec ->

                    val fileSpec = fileSpecBuilder.addType(typeSpec).build()
                    fileSpecChannel.send(fileSpec)
                }
    }


}