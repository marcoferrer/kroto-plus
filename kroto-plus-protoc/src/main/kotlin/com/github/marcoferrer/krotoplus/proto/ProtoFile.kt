package com.github.marcoferrer.krotoplus.proto

import com.github.marcoferrer.krotoplus.Manifest
import com.google.common.base.CaseFormat
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

data class ProtoFile(
        override val descriptorProto: DescriptorProtos.FileDescriptorProto,
        val schema: Schema
): Schema.DescriptorWrapper {
    inline val name: String get() = descriptorProto.name
    val javaPackage: String = descriptorProto.javaPackage
    val protoPackage: String = descriptorProto.protoPackage
    val javaOuterClassname: String = descriptorProto.javaOuterClassname
    val javaClassName: ClassName = ClassName(javaPackage,javaOuterClassname)
    val javaMultipleFiles get() = descriptorProto.options.javaMultipleFiles

    val outputPath = javaClassName.canonicalName.replace(".","/") + ".java"

    val protoMessages: List<ProtoMessage> = descriptorProto.messageTypeList.map { messageType ->
        ProtoMessage(messageType,this@ProtoFile)
    }

    val protoEnums: List<ProtoEnum> = descriptorProto.enumTypeList.map { enumType ->
        ProtoEnum(enumType,this@ProtoFile)
    }

    val services: List<ProtoService> = descriptorProto.serviceList.map { serviceDescriptor ->
        ProtoService(serviceDescriptor, this@ProtoFile)
    }
}

val DescriptorProtos.FileDescriptorProto.javaPackage: String
    get() = when{
        this.options.hasJavaPackage() -> this.options.javaPackage
        this.hasPackage() -> this.`package`
        else -> ""
    }

val DescriptorProtos.FileDescriptorProto.protoPackage: String
    get() = if (this.hasPackage()) this.`package` else ""

val DescriptorProtos.FileDescriptorProto.javaOuterClassname: String
    get() = if(this.options.hasJavaOuterClassname())
        this.options.javaOuterClassname else this.name
                .substringAfterLast("/")
                .substringBefore(".proto")
                .let { fileName ->
                    val outerClassname = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,fileName)

                    if ( enumTypeList.any { it.name == outerClassname }    ||
                            messageTypeList.any { it.name == outerClassname } ||
                            serviceList.any { it.name == outerClassname } )

                        "${outerClassname}OuterClass" else outerClassname
                }

fun DescriptorProtos.FileDescriptorProto.getGeneratedAnnotationSpec() =
        AnnotationSpec.builder(ClassName("javax.annotation","Generated"))
                .addMember("value = [%S]", "by ${Manifest.implTitle} (version ${Manifest.implVersion})")
                .addMember("comments = %S", "Source: $name")
                .build()

fun ProtoFile.getGeneratedAnnotationSpec() = descriptorProto.getGeneratedAnnotationSpec()