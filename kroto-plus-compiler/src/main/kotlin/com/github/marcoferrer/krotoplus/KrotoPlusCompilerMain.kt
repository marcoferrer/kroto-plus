@file:JvmName("KrotoPlusCompilerMain")
package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.generators.GeneratorResult
import com.github.marcoferrer.krotoplus.generators.MockServiceGenerator
import com.github.marcoferrer.krotoplus.generators.ProtoTypeBuilderGenerator
import com.github.marcoferrer.krotoplus.generators.StubRpcOverloadGenerator
import com.squareup.wire.schema.SchemaLoader
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.flagValueArgument
import kotlinx.cli.parse
import kotlinx.cli.positionalArgumentsList
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import kotlin.coroutines.experimental.coroutineContext
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

object Manifest {
    val name        = javaClass.`package`.name.orEmpty()
    val implTitle   = javaClass.`package`.implementationTitle.orEmpty()
    val implVersion = javaClass.`package`.implementationVersion.orEmpty()
}

internal val appCli = CommandLineInterface("Kroto-Plus")

internal val protoSources by appCli
        .positionalArgumentsList("source_dir_1 source_dir_2 ...",
                "Proto Source Directories / Jars (Space delimited) ", minArgs = 1)

internal val fileWriterCount by appCli
        .flagValueArgument("-writers", "<int>","Number of concurrent file writers (default 3)",3){ it.toInt() }

internal val defaultOutputPath by appCli
        .flagValueArgument("-default-out", "default/output/path", "Default destination dir for generated sources")

internal val defaultOutputDir by lazy { File(defaultOutputPath).apply { mkdirs() } }

fun main(vararg args: String){

    val executionTime = measureTimeMillis { runBlocking {

        val fileSpecActor = actor<GeneratorResult>(capacity = Channel.UNLIMITED) {
            repeat(fileWriterCount){
                launch(coroutineContext) {
                    channel.consumeEach { it.fileSpec.writeTo(it.outputDir) }
                }
            }
        }

        val generators = listOf(
                ProtoTypeBuilderGenerator(fileSpecActor).apply { bindToCli(appCli) },
                StubRpcOverloadGenerator(fileSpecActor).apply { bindToCli(appCli) },
                MockServiceGenerator(fileSpecActor).apply { bindToCli(appCli) }
        )

        try {
            appCli.parse(args)
        } catch (e: Exception) {
            exitProcess(1)
        }

        val schema = SchemaLoader().run {
            for (sourcePath in protoSources) {
                val file = File(sourcePath)
                assert(file.exists())
                addSource(file)
            }
            load()
        }

        generators
                .filter { it.isEnabled }
                .map { it.generate(schema) }
                .forEach { it.join() }

        fileSpecActor.close()
        (fileSpecActor as Job).join()
    }}

    println("Kroto+ Completed in ${executionTime}ms")
}
