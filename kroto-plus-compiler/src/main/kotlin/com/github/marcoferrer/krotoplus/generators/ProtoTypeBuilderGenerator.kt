package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.defaultOutputDir
import com.github.marcoferrer.krotoplus.generators.GeneratorModule.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.*
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.flagAction
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch

class ProtoTypeBuilderGenerator(
        override val resultChannel: SendChannel<GeneratorResult>
) : GeneratorModule {

    override var isEnabled: Boolean = false

    override fun bindToCli(mainCli: CommandLineInterface) {
        mainCli.apply {
            flagAction("-ProtoTypeBuilder", "Generate builder lambdas for proto message types") {
                isEnabled = true
            }
        }
    }

    override fun generate(schema: Schema) = launch {
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
        val fileSpecBuilder = FileSpec.builder(protoFile.outputPackage(),filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        val typeSpecBuilder = TypeSpec.objectBuilder(filename)
                .addAnnotation(protoFile.getGeneratedAnnotationSpec())

        protoFile.types()
                .asSequence()
                .filterNot { it is EnumType }
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
                    resultChannel.send(GeneratorResult(fileSpec, defaultOutputDir))
                }
    }


}