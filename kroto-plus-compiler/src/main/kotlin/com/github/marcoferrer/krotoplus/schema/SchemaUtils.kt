package com.github.marcoferrer.krotoplus.schema

import com.github.marcoferrer.krotoplus.Manifest
import com.google.common.base.CaseFormat
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.schema.*
import javax.annotation.Generated

object ProtoOptions{

    object File{
        val JavaOuterClassname = ProtoMember.get(Options.FILE_OPTIONS, "java_outer_classname")
        val JavaMultipleFiles = ProtoMember.get(Options.FILE_OPTIONS, "java_multiple_files")
    }

    object Service{
        val ServiceIsDeprecated = ProtoMember.get(Options.SERVICE_OPTIONS, "deprecated")
    }
}

fun ProtoType.toClassName(protoSchema: Schema): ClassName {

    val file = protoSchema.run {
        protoFile( getType(this@toClassName).location().path() )
    }

    return this.toClassName(file)
}

fun ProtoType.toClassName(protoFile: ProtoFile): ClassName {

    return if (protoFile.javaMultipleFiles)
        ClassName(protoFile.outputPackage(), this.simpleName())
    else
        ClassName(protoFile.outputPackage(), protoFile.javaOuterClassname, this.simpleName())
}

val ProtoType.isEmptyMessage
    get() = this.enclosingTypeOrPackage().startsWith("google.protobuf") && this.simpleName() == "Empty"

val ProtoFile.javaOuterClassname: String
    get() = options().get(ProtoOptions.File.JavaOuterClassname)?.toString()
            ?: CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,this.name())

val ProtoFile.javaMultipleFiles: Boolean
    get() = options().get(ProtoOptions.File.JavaMultipleFiles)?.toString()?.toBoolean() == true

val Service.isDeprecated: Boolean
    get() = options().get(ProtoOptions.Service.ServiceIsDeprecated)?.toString()?.toBoolean() == true

fun ProtoFile.getGeneratedAnnotationSpec() =
        AnnotationSpec.builder(Generated::class.asClassName())
                .addMember("value = [%S]", "by ${Manifest.implTitle} (version ${Manifest.implVersion})")
                .addMember("comments = %S", "Source: ${this.location().path()}")
                .build()

val ProtoFile.isCommonProtoFile get() = outputPackage().startsWith("com.google.protobuf")

fun ProtoFile.outputPackage(): String = javaPackage() ?: packageName()
