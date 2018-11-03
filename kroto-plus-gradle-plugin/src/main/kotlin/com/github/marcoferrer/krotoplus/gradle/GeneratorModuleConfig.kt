package com.github.marcoferrer.krotoplus.gradle

import java.io.File
import kotlin.String

interface GeneratorModuleConfig {
    fun toCliArgs(defaultOutputPath: String): List<String>
}

open class StubOverloadGeneratorConfig : GeneratorModuleConfig {

    var outputDir: File? = null
    var supportCoroutines: Boolean = false

    override fun toCliArgs(defaultOutputPath: String): List<String> =
        mutableListOf("-StubOverloads").also { list ->
            val outputPath = outputDir?.absolutePath ?: defaultOutputPath

            if (supportCoroutines)
                list.add("-o|$outputPath|-coroutines")
            else
                list.add("-o|$outputPath")
        }
}

open class MockServicesGeneratorConfig : GeneratorModuleConfig {

    var outputDir: File? = null

    override fun toCliArgs(defaultOutputPath: String): List<String> =
        mutableListOf("-MockServices").also { list ->
            val outputPath = outputDir?.absolutePath ?: defaultOutputPath

            list.add("-o|$outputPath")
        }
}

open class ProtoTypeBuildersGeneratorConfig : GeneratorModuleConfig {

    override fun toCliArgs(defaultOutputPath: String): List<String> =
        listOf("-ProtoTypeBuilder")
}

open class ExternalGeneratorConfig(val canonicalClassName: String) : GeneratorModuleConfig {

    var args: List<String> = emptyList()

    override fun toCliArgs(defaultOutputPath: String): List<String> =
        listOf("-EXT-$canonicalClassName", args.joinToString(separator = "|"))
}
