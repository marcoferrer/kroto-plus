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

package com.github.marcoferrer.krotoplus.gradle.protobuf

import com.github.marcoferrer.krotoplus.gradle.KrotoPlusGenerateConfigTask
import com.github.marcoferrer.krotoplus.gradle.compiler.CompilerConfigWrapper
import com.github.marcoferrer.krotoplus.gradle.getOrCreate
import com.github.marcoferrer.krotoplus.gradle.krotoPlus
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.gradle.util.Configurable
import javax.inject.Inject

open class GenericProtobufConfigurator @Inject constructor(
    private val project: Project
) : Configurable<GenericProtobufConfigurator>{

    private var configurationName = DEFAULT_CONFIG_NAME

    class Versions (
        var protoc: String? = null,
        var grpc: String = DEFAULT_GRPC_VERSION,
        var krotoPlus: String = DEFAULT_KROTO_PLUS_VERSION
    ){
        companion object{
            const val DEFAULT_GRPC_VERSION = "1.23.0"
            val DEFAULT_KROTO_PLUS_VERSION = this::class.java.`package`.implementationVersion.orEmpty()
        }
    }

    val versions = Versions()

    open fun versions(action: Action<in Versions>){
        action.execute(versions)
    }

    open fun useConfig(closure: Closure<in CompilerConfigWrapper>){
        project.krotoPlus.config
            .getOrCreate(configurationName)
            .also { closure(it) }
    }

    open fun useConfig(name: String){
        project.krotoPlus.config.findByName(name) ?: error("Kroto Plus configuration with name '$name' was not found")
        configurationName = name
    }

    override fun configure(cl: Closure<in GenericProtobufConfigurator>): GenericProtobufConfigurator = also {
        cl.invoke(it)
        project.afterEvaluate {
            configureProtobufPlugin()
        }
    }

    private fun configureProtobufPlugin(){
        val compilerConfig = project.krotoPlus.config.getByName(configurationName)

        project.protobuf {
            versions.protoc?.let { v ->
                protoc {
                    artifact = "com.google.protobuf:protoc:$v"
                }
            }

            plugins {
                id(DEFAULT_PLUGIN_NAME_GRPC){
                    artifact = "io.grpc:protoc-gen-grpc-java:${versions.grpc}"
                }
                id(DEFAULT_PLUGIN_NAME_KROTO){
                    artifact = "com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus:${versions.krotoPlus}"
                }
            }

            generateProtoTasks.all().forEach { task ->
                with(task){
                    inputs.file(compilerConfig.outputFile)
                    dependsOn(KrotoPlusGenerateConfigTask.DEFAULT_TASK_NAME)
                    plugins{
                        id(DEFAULT_PLUGIN_NAME_GRPC)
                        id(DEFAULT_PLUGIN_NAME_KROTO){
                            option(compilerConfig.asOption())
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_CONFIG_NAME = "genericConfiguration"
        const val DEFAULT_PLUGIN_NAME_GRPC = "${DEFAULT_CONFIG_NAME}_grpc"
        const val DEFAULT_PLUGIN_NAME_KROTO = "${DEFAULT_CONFIG_NAME}_kroto"
    }
}