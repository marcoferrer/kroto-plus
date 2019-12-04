/*
 *  Copyright 2019 Kroto+ Contributors
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

package com.github.marcoferrer.krotoplus.gradle

import com.github.marcoferrer.krotoplus.gradle.compiler.CompilerConfigWrapper
import com.google.protobuf.TextFormat
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File


open class KrotoPlusGenerateConfigTask : DefaultTask(){

    private val configContainer: NamedDomainObjectContainer<CompilerConfigWrapper>
        get() = project.krotoPlus.config

    @get:Input
    internal val configInputs: Map<String, String>
        get() = configContainer.asMap.mapValues { it.value.builder.toString() }

    @get:OutputFiles
    val configOutputs: List<File>
        get() = configContainer.map { it.outputFile }

    @TaskAction
    fun createConfigurations(){
        configContainer.forEach { configWrapper ->
            val compilerConfig = configWrapper.builder.build()
            configWrapper.outputFile.apply {
                parentFile.mkdirs()
                writer().use {
                    TextFormat.printer().print(compilerConfig, it)
                }
            }
        }
    }

    companion object{
        const val DEFAULT_TASK_NAME = "generateKrotoPlusConfig"
    }
}