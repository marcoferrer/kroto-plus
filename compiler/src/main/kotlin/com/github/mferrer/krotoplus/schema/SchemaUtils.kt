package com.github.mferrer.krotoplus.schema

import com.squareup.kotlinpoet.ClassName
import com.squareup.wire.schema.*

fun ProtoType.toClassName(protoSchema: Schema): ClassName {

    val file = protoSchema.run {
        protoFile( getType(this@toClassName).location().path() )
    }

    return if (file.javaMultipleFiles)
        ClassName(file.javaPackage(), this.simpleName())
    else
        ClassName(file.javaPackage(), file.javaOuterClassname, this.simpleName())
}

val ProtoType.isEmptyMessage get() = this.enclosingTypeOrPackage() =="google.protobuf" && this.simpleName() == "Empty"

object ProtoOptions{

    object File{
        val JavaOuterClassname = ProtoMember.get(Options.FILE_OPTIONS, "java_outer_classname")
        val JavaMultipleFiles = ProtoMember.get(Options.FILE_OPTIONS, "java_multiple_files")
    }

    object Service{
        val ServiceIsDeprecated = ProtoMember.get(Options.SERVICE_OPTIONS, "deprecated")
    }
}

val ProtoFile.javaOuterClassname: String
    get() = options().get(ProtoOptions.File.JavaOuterClassname)?.toString() ?: this.name().capitalize()

val ProtoFile.javaMultipleFiles: Boolean
    get() = options().get(ProtoOptions.File.JavaMultipleFiles)?.toString()?.toBoolean() == true

val Service.isDeprecated: Boolean
    get() = options().get(ProtoOptions.Service.ServiceIsDeprecated)?.toString()?.toBoolean() == true