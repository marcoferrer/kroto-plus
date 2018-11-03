package com.github.marcoferrer.krotoplus.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class KrotoPlusPluginExtension @Inject constructor(objectFactory: ObjectFactory) {

    open val generatorsConfig: KrotoPlusGeneratorsConfig =
        objectFactory.newInstance(KrotoPlusGeneratorsConfig::class.java, objectFactory)

    open fun generators(action: Action<in KrotoPlusGeneratorsConfig>) {
        action.execute(generatorsConfig)
    }

    open fun generators(block: KrotoPlusGeneratorsConfig.() -> Unit) =
        generators(Action(block))

}