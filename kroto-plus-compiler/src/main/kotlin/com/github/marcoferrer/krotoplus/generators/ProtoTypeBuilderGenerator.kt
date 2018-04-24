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

    private fun buildFileSpec(protoFile: ProtoFile){
        val filename = "${protoFile.javaOuterClassname}Builders"
        val fileSpecBuilder = FileSpec.builder(protoFile.outputPackage(),filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        val typeSpecBuilder = TypeSpec.objectBuilder(filename)
                .addAnnotation(protoFile.getGeneratedAnnotationSpec())

        val builderFunSpecs = mutableListOf<FunSpec>()
        val copyFunSpecs = mutableListOf<FunSpec>()

        protoFile.types()
                .asSequence()
                .filterNot { it is EnumType }
                .map{ it.type() }
                .forEach { protoType ->

                    val typeClassName = protoType.toClassName(protoFile)
                    val builderTypeName = LambdaTypeName.get(
                            receiver = typeClassName.nestedClass("Builder"),
                            returnType = UNIT)

                    // Make optional with generator option?
                    FunSpec.builder(protoType.simpleName())
                            .addModifiers(KModifier.INLINE)
                            .addParameter("block", builderTypeName)
                            .addStatement("return %T.newBuilder().apply(block).build()", typeClassName)
                            .also {
                                builderFunSpecs.add(it.build())
                            }

                    // Make optional with generator option?
                    FunSpec.builder("copy")
                            .receiver(typeClassName)
                            .addModifiers(KModifier.INLINE)
                            .addParameter("block",builderTypeName)
                            .addStatement("return this.toBuilder().apply(block).build()")
                            .also {
                                copyFunSpecs.add(it.build())
                            }
                }

        if(builderFunSpecs.isNotEmpty()){
            typeSpecBuilder.addFunctions(builderFunSpecs)
            fileSpecBuilder.addType(typeSpecBuilder.build())
        }

        for(funSpec in copyFunSpecs)
            fileSpecBuilder.addFunction(funSpec)

        // This is a redundant check on any funSpec collection. This is done in case in the future
        // we decide to omit certain proto types from either collection
        if(builderFunSpecs.isNotEmpty() || copyFunSpecs.isNotEmpty())
            resultChannel.offer(GeneratorResult(fileSpecBuilder.build(), defaultOutputDir))

    }


}