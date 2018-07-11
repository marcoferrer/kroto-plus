package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.*
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*

class ProtoBuildersGenerator(override val context: Generator.Context) : Generator {

    override val key = "builder-lambdas"

    override fun invoke(responseBuilder: PluginProtos.CodeGeneratorResponse.Builder) {

        context.request.protoFileList.asSequence()
                .filter { !it.name.startsWith("google") }
                .forEach { fileDescriptorProto ->
                    buildFileSpec(fileDescriptorProto)?.let {
                        responseBuilder.addFile(it.toResponseFileProto())
                    }
                }
    }

    private fun buildFileSpec(protoFile: DescriptorProtos.FileDescriptorProto): FileSpec? {
        val filename = "${protoFile.javaOuterClassname}Builders"
        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage,filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        val typeSpecBuilder = TypeSpec.objectBuilder(filename)
                .addAnnotation(protoFile.getGeneratedAnnotationSpec())

        val builderFunSpecs = mutableListOf<FunSpec>()
        val copyFunSpecs = mutableListOf<FunSpec>()

        protoFile.messageTypeList
                .asSequence()
                .map{ messageDescriptor ->
                    // TODO fixme, not performant AT ALL
                    context.schema.types.values.find {
                        it is ProtoMessage && it.descriptorProto == messageDescriptor
                    }!!
                }
                .forEach { protoType ->

                    val typeClassName = protoType.className
                    val builderTypeName = LambdaTypeName.get(
                            receiver = typeClassName.nestedClass("Builder"),
                            returnType = UNIT)

                    // Make optional with generator option?
                    FunSpec.builder(protoType.name)
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
        return if(builderFunSpecs.isNotEmpty() || copyFunSpecs.isNotEmpty())
            fileSpecBuilder.build() else null
    }
}