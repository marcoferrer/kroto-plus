/*
 * Copyright 2019 Kroto+ Contributors
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

package com.github.marcoferrer.krotoplus.script

import com.github.marcoferrer.krotoplus.generators.Generator
import org.jetbrains.kotlin.script.tryCreateCallableMapping
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf

data class RenderScript(
    val klass: KClass<*>,
    val functions: List<KFunction<*>> = klass.functions.toList(),
    val instance: Any? = null
) {

    private val generatorClasses = klass.nestedClasses.filter { it.isSubclassOf(Generator::class) }

    val generators by lazy {
        generatorClasses
            .map { it.objectInstance }
            .filterIsInstance<Generator>()
    }

    fun invoke(name: String, vararg args: Any?): Any? {
        return functions
            .asSequence()
            .filter { it.name == name }
            .map { it to it.getMapping(args) }
            .find { it.second != null }
            ?.let { (func, mapping) -> func.callBy(mapping!!) }
    }

    private fun KFunction<*>.getMapping(args: Array<out Any?>): Map<KParameter, Any?>? =
    // TODO: Handle exception and throw error detailing the issue with the script
        tryCreateCallableMapping(this, listOf(instance) + args)
}
