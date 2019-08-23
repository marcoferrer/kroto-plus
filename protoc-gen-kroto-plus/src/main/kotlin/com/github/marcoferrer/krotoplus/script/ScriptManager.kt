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

import com.github.marcoferrer.krotoplus.Manifest
import com.github.marcoferrer.krotoplus.generators.context
import com.github.marcoferrer.krotoplus.utils.memoize
import org.jetbrains.kotlin.cli.common.repl.GenericReplEvaluatorState
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmInvocableScriptEngine
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import java.io.*
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.DigestInputStream
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext
import java.security.MessageDigest

private const val CONFIG_KEY_SCRIPT_CACHE_DIR = "script_cache_dir"
private const val PROP_KEY_SCRIPT_CACHE_DIR = "krotoplus.script.cache.dir"
private const val ENV_KEY_SCRIPT_CACHE_DIR = "KROTOPLUS_SCRIPT_CACHE_DIR"

object ScriptManager {

    private val cacheDirPath = run {
        val systemProp = System.getProperty(PROP_KEY_SCRIPT_CACHE_DIR)
        val envVar = System.getenv(ENV_KEY_SCRIPT_CACHE_DIR)
        val configVal = context.args.options[CONFIG_KEY_SCRIPT_CACHE_DIR]
        val userHome = System.getProperty("user.home") ?: System.getProperty("HOME")

        val baseCachePath = systemProp ?: envVar ?: configVal ?: userHome

        "$baseCachePath/.kroto/cache/${Manifest.implVersion}"
    }
    private val compileCacheDir = File(cacheDirPath).apply { mkdirs() }

    private val scriptEngine by lazy {
        ScriptEngineManager().getEngineByExtension("kts")
                as KotlinJsr223JvmLocalScriptEngine
    }

    private val scriptCache = mutableMapOf<String, RenderScript>()

    private val scriptBundleClassLoaders = mutableMapOf<String, ClassLoader>()

    private fun loadScriptClass(scriptPath: String, bundle: File): Class<*>? {

        require(bundle.exists()){ "Bundle not found: ${bundle.absolutePath}" }

        //Convert script path to java package
        val scriptPackage = scriptPath
            .takeIf { !it.startsWith("/") && "/" in it }
            ?.substringBeforeLast("/")
            ?.replace("/", ".")
            ?.let { "$it." }
            .orEmpty()
        //Convert script name to valid java classname
        val scriptName = scriptPath
            .substringAfterLast("/").substringBeforeLast(".")
            .replace(Regex("[.-]"), "_")
            .capitalize()

        val classLoader = getClassLoaderForBundle(bundle)

        return Class.forName("$scriptPackage$scriptName", true, classLoader)
    }

    private fun getClassLoaderForBundle(bundle: File) =
        scriptBundleClassLoaders.getOrPut(bundle.absolutePath) {
            URLClassLoader(arrayOf(bundle.toURI().toURL()), this.javaClass.classLoader)
        }

    private fun getScriptFromBundle(scriptPath: String, bundle: File): RenderScript =
        scriptCache.getOrPut("${bundle.absolutePath}/$scriptPath") {
            loadScriptClass(scriptPath, bundle)?.let { clazz ->
                val instance = clazz.getConstructor(Array<String>::class.java).newInstance(emptyArray<String>())
                RenderScript(instance::class, instance = instance)
            }
                ?: throw IllegalArgumentException("Script: '$scriptPath' was not found in bundle '${bundle.absolutePath}'")
        }

    internal fun getScript(scriptFile: File): RenderScript =
        scriptCache.getOrPut(scriptFile.absolutePath) {
            getOrLoadCompiledClasses(scriptFile).toRenderScript(scriptEngine)
        }

    internal fun getScript(scriptPath: String, bundlePath: String? = null): RenderScript =
        if (bundlePath.orEmpty().isNotEmpty())
            getScriptFromBundle(scriptPath, File(context.currentWorkingDir, bundlePath)) else
            getScript(File(context.currentWorkingDir, scriptPath))

    private val getFileContentToHash = { scriptFile: File ->
        val md = MessageDigest.getInstance("MD5")
        DigestInputStream(scriptFile.inputStream(), md).use { dis ->
            InputStreamReader(dis).readText() to md.digest().toHex()
        }
    }.memoize()

    private fun getOrLoadCompiledClasses(scriptFile: File): ReplCompileResult.CompiledClasses {

        val (fileContent, checksum) = getFileContentToHash(scriptFile)
        val cachedFile = File(compileCacheDir, "$checksum.kts.compiled")

        return if (cachedFile.exists()) try {
            cachedFile.inputStream().use { fis ->
                ObjectInputStream(fis).use { it.readObject() } as ReplCompileResult.CompiledClasses
            }
        } catch (e: EOFException) {
            // Corrupted compiled file found try recompiling it
            compileAndWriteToFs(cachedFile, fileContent)
        } else {
            compileAndWriteToFs(cachedFile, fileContent)
        }
    }

    private fun compileAndWriteToFs(cacheFile: File, scriptContents: String): ReplCompileResult.CompiledClasses {
        if (cacheFile.exists()) cacheFile.delete()

        cacheFile.createNewFile()
        val compiledScript = scriptEngine.compile(scriptContents)
                as KotlinJsr223JvmScriptEngineBase.CompiledKotlinScript
        cacheFile.outputStream().use { fos ->
            ObjectOutputStream(fos).use { it.writeObject(compiledScript.compiledData) }
        }

        return compiledScript.compiledData
    }
}

private inline val KotlinJsr223JvmLocalScriptEngine.invocable
    get() = this as KotlinJsr223JvmInvocableScriptEngine

private fun ReplCompileResult.CompiledClasses.toRenderScript(scriptEngine: KotlinJsr223JvmLocalScriptEngine): RenderScript {
    val cleanScriptContext = SimpleScriptContext()
    val cleanCodeLine = scriptEngine.nextCodeLine(cleanScriptContext, "")
    scriptEngine.context = cleanScriptContext

    val compiledScript = KotlinJsr223JvmScriptEngineBase
        .CompiledKotlinScript(scriptEngine, cleanCodeLine, this)

    scriptEngine.eval(compiledScript, cleanScriptContext)

    return scriptEngine.invocable.buildRenderScript()
}

private fun KotlinJsr223JvmInvocableScriptEngine.buildRenderScript(): RenderScript {

    val prioritizedCallOrder = state
        .asState(GenericReplEvaluatorState::class.java)
        .history
        .map { it.item }
        .filter { it.instance != null }
        .reversed()

    return prioritizedCallOrder.first().let { (klass, instance, _, _) ->
        RenderScript(klass, instance = instance)
    }
}

private fun ByteArray.toHex() = memoizedByteArrayToHex(this)

private val memoizedByteArrayToHex = { bytes: ByteArray ->
    val bi = BigInteger(1, bytes)
    String.format("%0" + (bytes.size shl 1) + "X", bi)
}.memoize()