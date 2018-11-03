@file:JvmName("KrotoPlusProtoCMain")

package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.generators.*
import com.google.protobuf.compiler.PluginProtos
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.jvm.util.KotlinJars


fun main(args: Array<String>) = runBlocking {

    setScriptClassPath()
    initializeContext()

    val generators = listOf(
        GrpcStubExtsGenerator,
        ProtoBuildersGenerator,
        ExtendableMessagesGenerator,
        MockServicesGenerator,
        InsertionsGenerator,
        GeneratorScriptsGenerator
    )

    generators
        .filter { it.isEnabled }
        .map { generator -> async { generator() } }
        .fold(PluginProtos.CodeGeneratorResponse.newBuilder()) { builder, deferredResult ->
            val result = deferredResult.await()

            if (result != PluginProtos.CodeGeneratorResponse.getDefaultInstance())
                builder.mergeFrom(result) else
                builder
        }
        .build()
        .writeTo(System.out)

    System.out.flush()
}

object Manifest {
    val name = this::class.java.`package`.name.orEmpty()
    val implTitle = this::class.java.`package`.implementationTitle.orEmpty()
    val implVersion = this::class.java.`package`.implementationVersion.orEmpty()
}

// Because of how we are bundling the executable with spring boot
// we have to explicitly set the kotlin script classpath
// https://youtrack.jetbrains.com/issue/KT-21443
private fun setScriptClassPath() {
    // The plugin jar is unpacked by spring boot
    // into the same directory as the compiler jar
    val scriptClasspath = KotlinJars.stdlib.parentFile.walkTopDown()
        .map { it.absolutePath }.joinToString(separator = CLASSPATH_SEPARATOR)

    System.setProperty("kotlin.script.classpath", scriptClasspath)
}

val CLASSPATH_SEPARATOR = if (System.getProperty("os.name").toLowerCase().contains("windows"))
    ";" else ":"