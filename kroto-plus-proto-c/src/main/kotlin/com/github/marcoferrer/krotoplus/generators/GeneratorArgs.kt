package com.github.marcoferrer.krotoplus.generators

import com.google.protobuf.compiler.PluginProtos

class GeneratorArgs(
        val options: Map<String, String>,
        val flags: List<String>
){
    companion object {

        val EMPTY = GeneratorArgs(emptyMap(), emptyList())

        fun parse(parameter: String): GeneratorArgs = parameter
                .split(",")
                .partition { "=" in it }
                .let { (keyValuesPairs, flags) ->

                    val options = keyValuesPairs
                            .asSequence()
                            .map { it.split("=") }
                            .associateBy({ it[0] }, { it[1] })

                    GeneratorArgs(options, flags)
                }
    }
}

fun PluginProtos.CodeGeneratorRequest.parseArgs(): GeneratorArgs =
        if(parameter.isNullOrEmpty())
            GeneratorArgs.EMPTY else GeneratorArgs.parse(parameter)
