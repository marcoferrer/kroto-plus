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
