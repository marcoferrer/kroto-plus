@file:JvmName("KrotoPlusProtoCMain")
package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.generators.*
import com.google.protobuf.compiler.PluginProtos
import java.io.File

object Manifest {
    val name        = this::class.java.`package`.name.orEmpty()
    val implTitle   = this::class.java.`package`.implementationTitle.orEmpty()
    val implVersion = this::class.java.`package`.implementationVersion.orEmpty()
}

fun main(args: Array<String>) {

//    val file = File("/Users/mferrer/IdeaProjects/kroto-plus/kroto-plus-proto-c/sample_plugin_request.bin")
//    val protoRequest = PluginProtos.CodeGeneratorRequest.parseFrom(file.inputStream())

    val protoRequest = PluginProtos.CodeGeneratorRequest.parseFrom(System.`in`)

    val context = Generator.Context(protoRequest)

    val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

    val generators = listOf(
            GrpcStubExtsGenerator(context),
            ProtoBuildersGenerator(context),
            ExtendableMessagesGenerator(context),
            MockServiceGenerator(context)
    )

    generators
            .filter { it.isEnabled }
            .forEach{ generator ->
                generator(responseBuilder)
            }

    responseBuilder.build().writeTo(System.out)
    System.out.flush()
}

