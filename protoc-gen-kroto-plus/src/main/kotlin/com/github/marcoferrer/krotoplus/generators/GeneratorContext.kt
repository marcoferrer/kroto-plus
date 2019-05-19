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

package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.CompilerConfig
import com.github.marcoferrer.krotoplus.proto.CompilerArgs
import com.github.marcoferrer.krotoplus.proto.Schema
import com.github.marcoferrer.krotoplus.proto.parseArgs
import com.github.marcoferrer.krotoplus.utils.yamlToJson
import com.google.protobuf.TextFormat
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.util.JsonFormat
import org.jetbrains.kotlin.utils.sure
import java.io.File

const val ARG_KEY_CONFIG_PATH = "ConfigPath"

data class GeneratorContext(
    val request: PluginProtos.CodeGeneratorRequest,
    val schema: Schema = Schema(request),
    val args: CompilerArgs = request.parseArgs(),
    val config: CompilerConfig = args.getCompilerConfig()
) {
    val currentWorkingDir by lazy {
        args.options[ARG_KEY_CONFIG_PATH]
            ?.firstOrNull()
            ?.let { path -> File(path).parentFile.takeIf { it.exists() } }
            .sure {
                "Unable to resolve current working directory. " +
                        "protoc option '$ARG_KEY_CONFIG_PATH' is not configured"
            }
    }
}

fun CompilerArgs.getCompilerConfig(): CompilerConfig =
    options[ARG_KEY_CONFIG_PATH]
        ?.firstOrNull()
        //TODO: Should we throw an error if the config is missing?
        ?.let { path -> File(path).takeIf { it.exists() } }
        ?.let { configFile ->
            CompilerConfig.newBuilder().also { builder ->
                when (configFile.extension.toLowerCase()) {
                    "json" -> JsonFormat.parser().merge(configFile.readText(), builder)
                    "asciipb" -> TextFormat.getParser().merge(configFile.readText(), builder)
                    "yaml", "yml" -> {
                        val jsonString = yamlToJson(configFile.readText())
                        JsonFormat.parser().merge(jsonString, builder)
                    }
                }
            }.build()
        }
        ?: CompilerConfig.getDefaultInstance()
