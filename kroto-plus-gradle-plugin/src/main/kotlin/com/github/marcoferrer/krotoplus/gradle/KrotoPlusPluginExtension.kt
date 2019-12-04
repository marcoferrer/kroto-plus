package com.github.marcoferrer.krotoplus.gradle

import com.github.marcoferrer.krotoplus.gradle.compiler.CompilerConfigWrapper
import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

open class KrotoPlusPluginExtension @Inject constructor(
    private val project: Project
) {

    public val config = project.container(CompilerConfigWrapper::class.java) { name ->
        CompilerConfigWrapper(name, project)
    }

    open fun config(closure: Closure<in NamedDomainObjectContainer<CompilerConfigWrapper>>) {
        ConfigureUtil.configure(closure, config)
    }
}