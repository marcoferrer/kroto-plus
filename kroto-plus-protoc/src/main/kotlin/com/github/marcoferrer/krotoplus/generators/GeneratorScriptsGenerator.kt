package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.script.ScriptManager
import com.google.protobuf.compiler.PluginProtos

object GeneratorScriptsGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.generatorScriptsCount > 0

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        for (options in context.config.generatorScriptsList) {
            options.templateScriptPathList
                    .flatMap { ScriptManager.getScript(it,options.templateScriptBundle).generators }
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