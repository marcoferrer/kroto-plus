package com.github.marcoferrer.krotoplus.gradle

import com.github.marcoferrer.krotoplus.config.CompilerConfig
import org.gradle.api.Action
import java.io.File
import javax.inject.Inject

open class KrotoPlusPluginExtension @Inject constructor(
    var outputDir: File
) {
    public val configs = mutableMapOf<String, CompilerConfig>()

    open fun createConfig(name:String, action: Action<in CompilerConfig.Builder>) {
        val compilerConfig = CompilerConfig.newBuilder()
            .also { action.execute(it) }
            .build()
        configs[name] = compilerConfig
    }
}