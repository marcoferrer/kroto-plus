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

        val messageTypesToBuild = protoFile.messageTypeList
                .map { messageDescriptor ->
                    context.schema.typesByDescriptor[messageDescriptor] as? ProtoMessage
                            ?: throw IllegalStateException("${messageDescriptor.name} was not found in schema type map.")
                }

        buildFunSpecsForTypes(fileSpecBuilder,typeSpecBuilder,messageTypesToBuild)

        fileSpecBuilder.addType(typeSpecBuilder.build())

        return fileSpecBuilder.build().takeIf { it.members.isNotEmpty() }
    }

    private fun buildFunSpecsForTypes(
            fileSpecBuilder: FileSpec.Builder,
            enclosingTypeSpec: TypeSpec.Builder,
            messageTypeList: List<ProtoMessage>
    ){

        for(protoType in messageTypeList){

            val typeClassName = protoType.className
            val builderTypeName = LambdaTypeName.get(
                    receiver = typeClassName.nestedClass("Builder"),
                    returnType = UNIT)

            FunSpec.builder(protoType.name)
                    .addModifiers(KModifier.INLINE)
                    .addParameter("block", builderTypeName)
                    .addStatement("return %T.newBuilder().apply(block).build()", typeClassName)
                    .also {
                        enclosingTypeSpec.addFunction(it.build())
                    }

            FunSpec.builder("copy")
                    .receiver(typeClassName)
                    .addModifiers(KModifier.INLINE)
                    .addParameter("block",builderTypeName)
                    .addStatement("return this.toBuilder().apply(block).build()")
                    .also {
                        fileSpecBuilder.addFunction(it.build())
                    }

            if(protoType.nestedMessageTypes.isNotEmpty()){

                val nestedTypeSpecBuilder = TypeSpec.objectBuilder(protoType.name)

                buildFunSpecsForTypes(
                        fileSpecBuilder,
                        nestedTypeSpecBuilder,
                        protoType.nestedMessageTypes
                )

                enclosingTypeSpec.addType(nestedTypeSpecBuilder.build())
            }
        }
    }
}