package com.github.marcoferrer.krotoplus

import kotlin.String
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class KrotoPlusGeneratorsConfig @Inject constructor(objectFactory: ObjectFactory){

    private val stubOverloadsConfig = objectFactory.newInstance(StubOverloadGeneratorConfig::class.java)
    private val mockServicesConfig = objectFactory.newInstance(MockServicesGeneratorConfig::class.java)
    private val protoTypeBuildersConfig = objectFactory.newInstance(ProtoTypeBuildersGeneratorConfig::class.java)

    open val generatorModules = mutableSetOf<GeneratorModuleConfig>()

    val protoTypeBuilders: ProtoTypeBuildersGeneratorConfig
        get() = protoTypeBuildersConfig.also {
            generatorModules.add(it)
        }

    val stubOverloads: StubOverloadGeneratorConfig
        get() = stubOverloadsConfig.also {
            generatorModules.add(it)
        }

    val mockServices: MockServicesGeneratorConfig
        get() = mockServicesConfig.also {
            generatorModules.add(it)
        }

    fun protoTypeBuilders(action: Action<in ProtoTypeBuildersGeneratorConfig>){
        action.execute(protoTypeBuilders)
        generatorModules.add(protoTypeBuilders)
    }

    fun protoTypeBuilders(block: ProtoTypeBuildersGeneratorConfig.() -> Unit) =
            protoTypeBuilders(Action(block))

    fun stubOverloads(action: Action<in StubOverloadGeneratorConfig>){
        action.execute(stubOverloadsConfig)
        generatorModules.add(stubOverloadsConfig)
    }

    fun stubOverloads(block: StubOverloadGeneratorConfig.() -> Unit) =
            stubOverloads(Action(block))

    fun mockServices(action: Action<in MockServicesGeneratorConfig>){
        action.execute(mockServicesConfig)
        generatorModules.add(mockServicesConfig)
    }

    fun mockServices(block: MockServicesGeneratorConfig.() -> Unit) =
            mockServices(Action(block))

    @JvmOverloads
    fun external(canonicalClassName: String, action: Action<in ExternalGeneratorConfig>? = null){
        ExternalGeneratorConfig(canonicalClassName).also {
            action?.execute(it)
            generatorModules.remove(it)
            generatorModules.add(it)
        }
    }
}