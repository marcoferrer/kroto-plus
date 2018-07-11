package com.github.marcoferrer.krotoplus.schema

import com.github.marcoferrer.krotoplus.Manifest
import com.google.common.base.CaseFormat
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName

//fun DescriptorProtos.DescriptorProto.canonicalProtoName(protoFile: DescriptorProtos.FileDescriptorProto) =
//        if(protoFile.hasPackage())
//            "${protoFile.`package`}.${this.name}" else this.name
//
//fun DescriptorProtos.DescriptorProto.canonicalJavaClassName(protoFile: DescriptorProtos.FileDescriptorProto): String{
//
//    val pkg = protoFile.javaPackage.let {
//        if(it.isEmpty()) "" else "$it."
//    }
//
//    return if(protoFile.options.javaMultipleFiles)
//        "$pkg${this.name}" else
//        "$pkg${protoFile.javaOuterClassname}.${this.name}"
//}

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

fun DescriptorProtos.FileDescriptorProto.getGeneratedAnnotationSpec() =
        AnnotationSpec.builder(javax.annotation.Generated::class.asClassName())
                .addMember("value = [%S]", "by ${Manifest.implTitle} (version ${Manifest.implVersion})")
                .addMember("comments = %S", "Source: $name")
                .build()