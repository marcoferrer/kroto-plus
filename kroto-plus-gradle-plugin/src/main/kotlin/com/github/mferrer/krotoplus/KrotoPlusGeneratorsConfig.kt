package com.github.mferrer.krotoplus

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class KrotoPlusGeneratorsConfig @Inject constructor(objectFactory: ObjectFactory){

    val stubOverloadsConfig = objectFactory.newInstance(StubOverloadGeneratorConfig::class.java)
    val mockServicesConfig = objectFactory.newInstance(MockServicesGeneratorConfig::class.java)
    val protoTypeBuildersConfig = objectFactory.newInstance(ProtoTypeBuildersGeneratorConfig::class.java)

    open val generatorModules = mutableSetOf<GeneratorModuleConfig>()

    val protoTypeBuilders: ProtoTypeBuildersGeneratorConfig
        get() = protoTypeBuildersConfig.also {
            generatorModules.add(it)
        }

    val stubOverloads: StubOverloadGeneratorConfig
        get() = stubOverloadsConfig.also {
            generatorModules.add(it)
        }

    fun stubOverloads(action: Action<in StubOverloadGeneratorConfig>){
        action.execute(stubOverloadsConfig)
        generatorModules.add(stubOverloadsConfig)
    }

    val mockServices: MockServicesGeneratorConfig
        get() = mockServicesConfig.also {
            generatorModules.add(it)
        }

    fun mockServices(action: Action<in MockServicesGeneratorConfig>){
        action.execute(mockServicesConfig)
        generatorModules.add(mockServicesConfig)
    }

    @JvmOverloads
    fun external(canonicalClassName: String, action: Action<in ExternalGeneratorConfig>? = null){
        ExternalGeneratorConfig(canonicalClassName).also {
            action?.execute(it)
            generatorModules.remove(it)
            generatorModules.add(it)
        }
    }
}