package com.github.marcoferrer.krotoplus.gradle

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

private const val PROTOBUF_PLUGIN_ID = "com.google.protobuf"

class KrotoPlusGradlePlugin : Plugin<Project> {

    private lateinit var project: Project
    private var wasApplied = false

    override fun apply(project: Project) {
        this.project = project
        project.pluginManager.withPlugin(PROTOBUF_PLUGIN_ID) {
            if (wasApplied) {
//                project.logger.warn("The com.google.protobuf plugin was already applied to the project: ' + project.path
//                        + ' and will not be applied again after plugin: ' + prerequisitePlugin.id)
            } else {
                wasApplied = true
                doApply()
            }
        }
    }

    private fun doApply() {
        project.extensions.create(
            "krotoPlus",
            KrotoPlusPluginExtension::class.java,
            project.projectDir
        )
        val createConfigTask = project.tasks.create(
            "generateKrotoPlusConfig",
            GenerateKrotoPlusConfigTask::class.java
        )
        project.afterEvaluate {
            it.tasks.getByName("generateProto").dependsOn.add(createConfigTask)
        }
    }
}

open class GenerateKrotoPlusConfigTask : DefaultTask() {

    @TaskAction
    open fun generate() {
        val krotoPlusExt = project.extensions
            .findByType(KrotoPlusPluginExtension::class.java)!!

        krotoPlusExt.outputDir.mkdirs()
        krotoPlusExt.configs.forEach { name, config ->
            File(krotoPlusExt.outputDir, "$name.asciipb").apply { createNewFile() }
                .writeText(config.toString())
        }
    }
}