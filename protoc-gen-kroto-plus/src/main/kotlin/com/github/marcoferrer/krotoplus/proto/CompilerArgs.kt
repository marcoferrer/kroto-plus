package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.compiler.PluginProtos

class CompilerArgs(
        val options: Map<String, List<String>>,
        val flags: List<String>
){
    companion object {
        val EMPTY = CompilerArgs(emptyMap(), emptyList())
    }
}

fun PluginProtos.CodeGeneratorRequest.parseArgs(): CompilerArgs =
        if(parameter.isEmpty())
            CompilerArgs.EMPTY else parameter
                .split(",")
                .partition { "=" in it }
                .let { (keyValuesPairs, flags) ->

                    val options = keyValuesPairs
                            .asSequence()
                            .map { it.split("=") }
                            .groupBy({ it[0]}, {it[1] })

                    CompilerArgs(options, flags)
                }
