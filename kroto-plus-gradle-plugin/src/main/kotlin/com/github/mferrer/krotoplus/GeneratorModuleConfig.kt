package com.github.mferrer.krotoplus

import java.io.File

interface GeneratorModuleConfig{
    fun toCliArgs(): List<String>
}

open class StubOverloadGeneratorConfig: GeneratorModuleConfig{

    var outputDir: File? = null
    var supportCoroutines: Boolean = false

    override fun toCliArgs(): List<String> =
            mutableListOf("-StubOverloads").also { list ->
                val outputPath = outputDir?.absolutePath ?: krotoPlusExt.defaultOutputDir!!.absolutePath

                if(supportCoroutines)
                    list.add("-o|$outputPath|-coroutines")
                else
                    list.add("-o|$outputPath")
            }
}

open class MockServicesGeneratorConfig: GeneratorModuleConfig{

    var outputDir: File? = null

    override fun toCliArgs(): List<String> =
            mutableListOf("-MockServices").also { list ->
                val outputPath = outputDir?.absolutePath ?: krotoPlusExt.defaultOutputDir!!.absolutePath

                list.add("-o|$outputPath")
            }
}

open class ProtoTypeBuildersGeneratorConfig: GeneratorModuleConfig{

    override fun toCliArgs(): List<String> =
        listOf("-ProtoTypeBuilder")
}

open class ExternalGeneratorConfig(val canonicalClassName: String) : GeneratorModuleConfig {

    var args: List<String> = emptyList()

    override fun toCliArgs(): List<String> =
            listOf("-EXT-$canonicalClassName",args.joinToString(separator = "|"))
}
