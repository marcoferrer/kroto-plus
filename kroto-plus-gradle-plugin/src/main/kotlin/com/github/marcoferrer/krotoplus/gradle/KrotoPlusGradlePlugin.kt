/*
 *  Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
