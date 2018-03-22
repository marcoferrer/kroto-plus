package com.github.mferrer.krotoplus

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class KrotoPlusPluginExtension @Inject constructor(objectFactory: ObjectFactory) : GeneratorModuleConfig{

    //TODO add debug flag

    open var sources:List<String> = emptyList()
    open var defaultOutputDir: File? = null
    open var fileWriterCount: Int = 3

    open var generatorsConfig: KrotoPlusGeneratorsConfig =
            objectFactory.newInstance(KrotoPlusGeneratorsConfig::class.java, objectFactory)

    open fun generators(action: Action<in KrotoPlusGeneratorsConfig>){
        action.execute(generatorsConfig)
    }

    override fun toCliArgs(): List<String> {
        assert(defaultOutputDir != null){ "Default output directory is not set." }
        assert(sources.isNotEmpty()){ "Sources is empty." }

        return sources.toMutableList().apply {
            add("-default-out")
            add(defaultOutputDir!!.path)
            add("-writers")
            add(fileWriterCount.toString())

            generatorsConfig.generatorModules
                    .flatMapTo(this){ it.toCliArgs() }
        }
    }
}