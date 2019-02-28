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

package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.compiler.PluginProtos

class CompilerArgs(
    val options: Map<String, List<String>>,
    val flags: List<String>
) {
    companion object {
        val EMPTY = CompilerArgs(emptyMap(), emptyList())
    }
}

fun PluginProtos.CodeGeneratorRequest.parseArgs(): CompilerArgs =
    if (parameter.isEmpty())
        CompilerArgs.EMPTY else parameter
        .split(",")
        .partition { "=" in it }
        .let { (keyValuesPairs, flags) ->

            val options = keyValuesPairs
                .asSequence()
                .map { it.split("=") }
                .groupBy({ it[0] }, { it[1] })

            CompilerArgs(options, flags)
        }
