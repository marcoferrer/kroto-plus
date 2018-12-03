package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.proto.*
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*


object MPProtobufMessageGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.mpProtobufMessagesList.isNotEmpty()

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()
        context.schema.protoFiles.forEach { protoFile ->

            val typeSpecs =
                protoFile.protoMessages.map { it.buildType() } + protoFile.protoEnums.map { it.buildType() }


            if (protoFile.javaMultipleFiles) for (typeSpec in typeSpecs) {
                responseBuilder.addFile(FileSpec
                    .builder(protoFile.javaPackage,typeSpec.name!!)
                    .addType(typeSpec)
                    .build()
                    .toResponseFileProto())

            } else {

                FileSpec.builder(protoFile.javaPackage,protoFile.javaOuterClassname)
                    .addType(TypeSpec
                        .objectBuilder(protoFile.javaOuterClassname)
                        .addTypes(typeSpecs)
                        .build())
                    .build()
                    .also {
                        responseBuilder.addFile(it.toResponseFileProto())
                    }
            }

        }

        return responseBuilder.build()
    }


    private fun ProtoEnum.buildType(): TypeSpec {

        val enumSpec = TypeSpec.enumBuilder(name)
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder("val number",Int::class.asClassName())
                    .build())
                .build())

        val companionSpec = TypeSpec.companionObjectBuilder()

        descriptorProto.valueList.forEach { enumDescriptor ->

            enumSpec.addEnumConstant("${enumDescriptor.name}(${enumDescriptor.number})")

            companionSpec.addProperty(PropertySpec
                .builder(enumDescriptor.name,Int::class.asClassName())
                .initializer(enumDescriptor.number.toString())
                .build()
            )
        }

        enumSpec.addEnumConstant("UNRECOGNIZED(-1)")

        return enumSpec
            .companionObject(companionSpec.build())
            .build()
    }

    private fun ProtoMessage.buildType(): TypeSpec {

        //Check field count before applying data modifier

        val classSpecBuilder = TypeSpec.classBuilder(name)
            .addModifiers(KModifier.DATA)
            .addAnnotation(ANNO_SPEC_SERIAL_SERIALIZABLE)

        val constructorSpec = FunSpec.constructorBuilder()

        descriptorProto.fieldList
            .forEach { fieldDescriptor ->

                val fieldName = camelCaseFieldName(fieldDescriptor.name).decapitalize()
                val className = fieldDescriptor.getFieldClassName(context.schema)

                val parameterBuilder = when{
                    fieldDescriptor.isMapField(context.schema) ||
                    fieldDescriptor.isRepeated -> ParameterSpec
                        .builder("val $fieldName",fieldDescriptor.getParamaterizeTypeName(context.schema))

                    else -> ParameterSpec.builder("val $fieldName",className)
                }

                parameterBuilder
                    .defaultValue(fieldDescriptor.getDefaultInitializer(context.schema))
                    .addAnnotation(ANNO_SPEC_SERIAL_OPTIONAL)
                    .addAnnotation(AnnotationSpec.builder(ANNO_CN_SERIAL_SERIAL_ID)
                        .addMember(fieldDescriptor.number.toString())
                        .build())

                constructorSpec.addParameter(parameterBuilder.build())
            }

        classSpecBuilder.primaryConstructor(constructorSpec.build())
            .companionObject(TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec
                    .builder("defaultInstance",className)
                    .initializer("%T()",className)
                    .build())
                .build())

        nestedMessageTypes.filterNot { it.isMapEntry }.forEach { nestedMessage ->
            classSpecBuilder.addType(nestedMessage.buildType())
        }

        nestedEnumTypes.forEach { nestedEnum ->
            classSpecBuilder.addType(nestedEnum.buildType())
        }

        return classSpecBuilder.build()
    }
}



val ANNO_CN_SERIAL_SERIALIZABLE =
    ClassName("kotlinx.serialization","Serializable")

val ANNO_SPEC_SERIAL_SERIALIZABLE =
    AnnotationSpec.builder(ANNO_CN_SERIAL_SERIALIZABLE).build()

val ANNO_CN_SERIAL_OPTIONAL =
    ClassName("kotlinx.serialization","Optional")

val ANNO_SPEC_SERIAL_OPTIONAL =
    AnnotationSpec.builder(ANNO_CN_SERIAL_OPTIONAL).build()

val ANNO_CN_SERIAL_SERIAL_ID =
    ClassName("kotlinx.serialization","SerialId")

private val camelCaseFieldName = { it: String ->
    // We cant use CaseFormat.UPPER_CAMEL since
    // protoc is lenient with malformed field names
    if (it.contains("_"))
        it.split("_").joinToString(separator = "") { it.capitalize() } else
        it.capitalize()

}.memoize()