package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.InsertionPoint
import com.github.marcoferrer.krotoplus.config.ProtoBuildersGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoFile
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.proto.getGeneratedAnnotationSpec
import com.github.marcoferrer.krotoplus.utils.*
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*

object ProtoBuildersGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.protoBuildersCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        // Build Exts & DSL Marker
        context.schema.protoFiles.asSequence()
            .filter { it.protoMessages.isNotEmpty() }
            .forEach { protoFile ->
                for (options in context.config.protoBuildersList) {
                    if (options.filter.matches(protoFile.name))
                        buildFileSpec(protoFile, options, responseBuilder)
                }
            }

        // Build DSL Insertions
        context.schema.protoTypes.asSequence()
            .filterIsInstance<ProtoMessage>()
            .filterNot { it.isMapEntry }
            .forEach { protoMessage ->
                for (options in context.config.protoBuildersList) {
                    if (options.filter.matches(protoMessage.protoFile.name))
                        responseBuilder.addFile(protoMessage.buildDslInsertion())
                }
            }

        return responseBuilder.build()
    }

    private fun buildFileSpec(
        protoFile: ProtoFile,
        options: ProtoBuildersGenOptions,
        responseBuilder: PluginProtos.CodeGeneratorResponse.Builder
    ) {

        val filename = "${protoFile.javaOuterClassname}Builders"
        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage, filename)
            .addComment(AutoGenerationDisclaimer)
            .addAnnotation(
                AnnotationSpec.builder(JvmName::class.asClassName())
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "-$filename")
                    .build()
            )

        val typeSpec = TypeSpec.objectBuilder(filename)
            .addAnnotation(protoFile.getGeneratedAnnotationSpec())
            .buildFunSpecsForTypes(fileSpecBuilder, protoFile.protoMessages)
            .build()

        if (options.unwrapBuilders) {
            fileSpecBuilder.addFunctions(typeSpec.funSpecs)
            fileSpecBuilder.addTypes(typeSpec.typeSpecs)
        } else {
            fileSpecBuilder.addType(typeSpec)
        }

        if (options.useDslMarkers) {
            fileSpecBuilder.buildDslMarkerInterface(protoFile)
        }

        fileSpecBuilder.build().takeIf { it.members.isNotEmpty() }?.let {
            responseBuilder.addFile(it.toResponseFileProto())
        }
    }

    private fun TypeSpec.Builder.buildFunSpecsForTypes(
        fileSpecBuilder: FileSpec.Builder,
        messageTypeList: List<ProtoMessage>
    ): TypeSpec.Builder {

        for (protoType in messageTypeList) if (!protoType.isMapEntry) {

            val builderLambdaTypeName = LambdaTypeName.get(
                receiver = protoType.builderClassName,
                returnType = UNIT
            )

            // Builder Spec
            FunSpec.builder(protoType.name)
                .addModifiers(KModifier.INLINE)
                .addParameter("block", builderLambdaTypeName)
                .returns(protoType.className)
                .addStatement("return %T.newBuilder().apply(block).build()", protoType.className)
                .also {
                    //Add generator option check "suppress_deprecated_warnings"
//                        if(protoType.descriptorProto.options.deprecated){
//                            it.addAnnotation()
                    this.addFunction(it.build())
                }

            // Copy Spec
            FunSpec.builder("copy")
                .receiver(protoType.className)
                .addModifiers(KModifier.INLINE)
                .addParameter("block", builderLambdaTypeName)
                .returns(protoType.className)
                .addStatement("return this.toBuilder().apply(block).build()")
                .also {
                    fileSpecBuilder.addFunction(it.build())
                }

            // Plus Operator Spec
            FunSpec.builder("plus")
                .receiver(protoType.className)
                .addModifiers(KModifier.OPERATOR)
                .addParameter("other", protoType.className)
                .returns(protoType.className)
                .addStatement("return this.toBuilder().mergeFrom(other).build()")
                .also {
                    fileSpecBuilder.addFunction(it.build())
                }

            // Or Default Spec
            FunSpec.builder("orDefault")
                .receiver(protoType.className.asNullable())
                .returns(protoType.className)
                .addStatement("return this ?: %T.getDefaultInstance()", protoType.className)
                .also {
                    fileSpecBuilder.addFunction(it.build())
                }

            fileSpecBuilder.addFunctions(buildNestedBuildersForMessage(protoType))

            if (protoType.nestedMessageTypes.isNotEmpty()) {
                addType(
                    TypeSpec.objectBuilder(protoType.name)
                        .buildFunSpecsForTypes(fileSpecBuilder, protoType.nestedMessageTypes)
                        .build()
                )
            }
        }
        return this
    }

    private fun buildNestedBuildersForMessage(protoType: ProtoMessage): List<FunSpec> =
        protoType.descriptorProto.fieldList.asSequence()
            .filter { it.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE }
            .map {
                it to requireNotNull(context.schema.protoTypes[it.typeName] as? ProtoMessage){
                    "${it.typeName} was not found in schema type map."
                }
            }
            .filterNot { it.second.isMapEntry }
            .map { (fieldDescriptorProto, protoMessageForField) ->

                val fieldNameCamelCase = camelCaseFieldName(fieldDescriptorProto.name)
                val statementTemplate = "return this.%N(%T.newBuilder().apply(block).build())"

                val funSpecBuilder = if (fieldDescriptorProto.label == LABEL_REPEATED)
                    FunSpec.builder("add$fieldNameCamelCase")
                        .addStatement(statementTemplate, "add$fieldNameCamelCase", protoMessageForField.className)
                else FunSpec.builder(fieldNameCamelCase.decapitalize())
                    .addStatement(statementTemplate, "set$fieldNameCamelCase", protoMessageForField.className)

                funSpecBuilder
                    .receiver(protoType.builderClassName)
                    .addModifiers(KModifier.INLINE)
                    .addParameter(
                        "block", LambdaTypeName.get(
                            receiver = protoMessageForField.builderClassName,
                            returnType = UNIT
                        )
                    )
                    .returns(protoType.builderClassName)
                    .build()
            }.toList()


    private fun FileSpec.Builder.buildDslMarkerInterface(protoFile: ProtoFile) {

        val dslMarker = TypeSpec
            .annotationBuilder(protoFile.dslAnnotationClassName.simpleName())
            .addAnnotation(DslMarker::class)
            .addAnnotation(
                AnnotationSpec.builder(Target::class)
                    .addMember("%T.CLASS", AnnotationTarget::class).build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Retention::class)
                    .addMember("%T.BINARY", AnnotationRetention::class).build()
            )
            .build()
            .also { this@buildDslMarkerInterface.addType(it) }

        TypeSpec.interfaceBuilder(protoFile.dslBuilderClassName.simpleName())
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName(protoFile.javaPackage, dslMarker.name!!))
                    .build()
            )
            .build()
            .also { this@buildDslMarkerInterface.addType(it) }
    }

    private fun ProtoMessage.buildDslInsertion() =
        PluginProtos.CodeGeneratorResponse.File.newBuilder()
            .apply {
                name = outputFilePath
                insertionPoint = "${InsertionPoint.BUILDER_IMPLEMENTS.key}:$canonicalProtoName"
                content = protoFile.dslBuilderClassName.canonicalName + ","
            }
            .build()

    private val ProtoFile.dslAnnotationClassName: ClassName
        get() = ClassName(javaPackage, "${javaOuterClassname}DslMarker")

    private val ProtoFile.dslBuilderClassName: ClassName
        get() = ClassName(javaPackage, "${javaOuterClassname}DslBuilder")
}

private val camelCaseFieldName = { it: String ->
    // We cant use CaseFormat.UPPER_CAMEL since
    // protoc is lenient with malformed field names
    if (it.contains("_"))
        it.split("_").joinToString(separator = "") { it.capitalize() } else
        it.capitalize()

}.memoize()