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

import com.github.marcoferrer.krotoplus.script.ScriptManager
import com.google.protobuf.compiler.PluginProtos

object GeneratorScriptsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.generatorScriptsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (options in context.config.generatorScriptsList) {
            options.scriptPathList
                .flatMap { ScriptManager.getScript(it, options.scriptBundle).generators }
                .also {
                    assert(it.isNotEmpty())
                }
                .forEach { generator ->
                    responseBuilder.mergeFrom(generator())
                }
        }

        return responseBuilder.build()
    }
}