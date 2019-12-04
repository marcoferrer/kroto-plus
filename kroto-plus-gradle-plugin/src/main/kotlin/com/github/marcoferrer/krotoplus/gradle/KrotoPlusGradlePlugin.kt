package com.github.marcoferrer.krotoplus.gradle

import com.google.protobuf.gradle.GenerateProtoTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class KrotoPlusGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin(PROTOBUF_PLUGIN_ID) {
            project.extensions.create(
                "krotoPlus",
                KrotoPlusPluginExtension::class.java,
                project
            )
            val generateConfigTask = project.tasks.create(
                KrotoPlusGenerateConfigTask.DEFAULT_TASK_NAME,
                KrotoPlusGenerateConfigTask::class.java
            )
            project.afterEvaluate {
                it.tasks.withType(GenerateProtoTask::class.java).forEach { task ->
                    task.dependsOn(generateConfigTask)
                }
            }
        }
    }

    companion object {
        private const val PROTOBUF_PLUGIN_ID = "com.google.protobuf"
    }
}
