/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("KrotoPlusProtoCMain")

package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.generators.*
import com.google.protobuf.compiler.PluginProtos
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.jvm.util.KotlinJars


fun main(args: Array<String>) {

    setScriptClassPath()
    initializeContext()

    val deferredGenResponse = GlobalScope.async {

        val generators = listOf(
            GrpcStubExtsGenerator,
            GrpcCoroutinesGenerator,
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
    }

    val response = runBlocking { deferredGenResponse.await() }

    response.writeTo(System.out)
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