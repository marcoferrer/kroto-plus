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

package com.github.marcoferrer.krotoplus.gradle.compiler

import com.github.marcoferrer.krotoplus.config.CompilerConfig
import com.github.marcoferrer.krotoplus.generators.ARG_KEY_CONFIG_PATH
import com.github.marcoferrer.krotoplus.gradle.osDetector
import com.google.protobuf.gradle.protobuf
import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.gradle.util.Configurable
import java.io.File
import java.util.*

class CompilerConfigWrapper(
    private val configName: String,
    private val project: Project
) : Named, Configurable<CompilerConfigWrapper> {

    override fun configure(closure: Closure<in CompilerConfig.Builder>): CompilerConfigWrapper = apply {
        closure.apply {
            delegate = builder
            resolveStrategy = Closure.DELEGATE_FIRST
            invoke(builder)
        }
    }

    override fun getName(): String = configName

    val builder = CompilerConfig.newBuilder()

    val outputFile = File(project.buildDir, "kroto/config/$name.asciipb")

    fun asOption(): String {

        // Windows needs the configuration path to be relative to the current working directory
        return if(project.osDetector.os == "windows"){
            "$ARG_KEY_CONFIG_PATH=${outputFile.absolutePath.replace(System.getProperty("user.dir"), "").drop(1)}"
        }else{
            "$ARG_KEY_CONFIG_PATH=${outputFile.absolutePath}"
        }
    }
}