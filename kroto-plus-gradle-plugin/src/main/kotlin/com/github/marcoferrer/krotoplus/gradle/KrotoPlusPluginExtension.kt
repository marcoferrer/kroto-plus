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