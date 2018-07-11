@file:JvmName("KrotoPlusProtoCMain")
package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.generators.GrpcStubExtsGenerator
import com.github.marcoferrer.krotoplus.generators.ProtoBuildersGenerator
import com.google.protobuf.compiler.PluginProtos

object Manifest {
    val name        = javaClass.`package`.name.orEmpty()
    val implTitle   = javaClass.`package`.implementationTitle.orEmpty()
    val implVersion = javaClass.`package`.implementationVersion.orEmpty()
}

fun main(args: Array<String>) {

    val protoRequest = PluginProtos.CodeGeneratorRequest.parseFrom(System.`in`)

    val context = Generator.Context(protoRequest)

    val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

    val generators = listOf(
            GrpcStubExtsGenerator(context),
            ProtoBuildersGenerator(context)
    )

    generators
            .filter { it.isEnabled }
            .forEach{ generator ->
                generator(responseBuilder)
            }

    responseBuilder.build().writeTo(System.out)
    System.out.flush()
}

