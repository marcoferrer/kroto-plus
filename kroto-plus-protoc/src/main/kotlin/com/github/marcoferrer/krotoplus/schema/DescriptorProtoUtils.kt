package com.github.marcoferrer.krotoplus.schema

import com.github.marcoferrer.krotoplus.Manifest
import com.google.common.base.CaseFormat
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

val DescriptorProtos.MethodDescriptorProto.isEmptyInput
    get() = this.inputType == ".google.protobuf.Empty"

val DescriptorProtos.MethodDescriptorProto.isNotEmptyInput
    get() = !isEmptyInput

fun DescriptorProtos.DescriptorProto.outputPath(protoFile: DescriptorProtos.FileDescriptorProto): String {

    val basePath = protoFile.javaPackage.replace(".","/")

    return if(protoFile.options.javaMultipleFiles)
        "$basePath/${this.name}.java" else
        "$basePath/${protoFile.javaOuterClassname}.java"
}

val DescriptorProtos.FileDescriptorProto.javaOuterClassname: String
    get() = if(this.options.hasJavaOuterClassname()) this.options.javaOuterClassname else
        this.name
            .substringAfterLast("/")
            .substringBefore(".proto")
            .let {
                val outerClassname = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,it)

                if ( enumTypeList.any { it.name == outerClassname }    ||
                     messageTypeList.any { it.name == outerClassname } ||
                     serviceList.any { it.name == outerClassname } )

                    "${outerClassname}OuterClass" else outerClassname
            }

val DescriptorProtos.FileDescriptorProto.javaPackage: String
    get() = when{
        this.options.hasJavaPackage() -> this.options.javaPackage
        this.hasPackage() -> this.`package`
        else -> ""
    }

val DescriptorProtos.FileDescriptorProto.protoPackage: String
    get() = if (this.hasPackage()) this.`package` else ""

fun PluginProtos.CodeGeneratorRequest.getAllProtoTypes(): Sequence<ProtoType> =
        protoFileList
                .asSequence()
                .flatMap { fileDescriptor ->
                    val protoMessages = fileDescriptor
                            .messageTypeList
                            .asSequence()
                            .map { ProtoMessage(it,fileDescriptor) }
                            .flattenProtoTypes()

                    val protoEnums = fileDescriptor
                            .enumTypeList
                            .asSequence()
                            .map { ProtoEnum(it,fileDescriptor) }
                            .flattenProtoTypes()

                    protoMessages + protoEnums
                }

fun DescriptorProtos.FileDescriptorProto.getGeneratedAnnotationSpec() =
        AnnotationSpec.builder(ClassName("javax.annotation","Generated"))
                .addMember("value = [%S]", "by ${Manifest.implTitle} (version ${Manifest.implVersion})")
                .addMember("comments = %S", "Source: $name")
                .build()