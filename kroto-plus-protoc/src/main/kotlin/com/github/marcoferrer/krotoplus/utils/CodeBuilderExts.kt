package com.github.marcoferrer.krotoplus.utils

import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

fun PluginProtos.CodeGeneratorResponse.Builder.addFile(block: PluginProtos.CodeGeneratorResponse.File.Builder.()->Unit)
        : PluginProtos.CodeGeneratorResponse.Builder {
    this.addFile(PluginProtos.CodeGeneratorResponse.File.newBuilder().apply(block).build())
    return this
}

fun FileSpec.Builder.addFunctions(funSpecs: Iterable<FunSpec>): FileSpec.Builder{
    for(funSpec in funSpecs) this.addFunction(funSpec)
    return this
}

fun FileSpec.Builder.addTypes(typeSpecs: Iterable<TypeSpec>): FileSpec.Builder{
    for(typeSpec in typeSpecs) this.addType(typeSpec)
    return this
}