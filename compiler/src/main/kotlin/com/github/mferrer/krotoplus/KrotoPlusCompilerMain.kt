@file:JvmName("KrotoPlusCompilerMain")
package com.github.mferrer.krotoplus

import com.github.mferrer.krotoplus.generators.MockServiceGenerator
import com.github.mferrer.krotoplus.generators.ProtoTypeBuilderGenerator
import com.github.mferrer.krotoplus.generators.StubRpcOverloadGenerator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.schema.SchemaLoader
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.flagValueArgument
import kotlinx.cli.parse
import kotlinx.cli.positionalArgumentsList
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

object Manifest {
    val name        = javaClass.`package`.name.orEmpty()
    val implTitle   = javaClass.`package`.implementationTitle.orEmpty()
    val implVersion = javaClass.`package`.implementationVersion.orEmpty()
}

class Cli(args: Array<out String>){

    private val cli = CommandLineInterface("Kroto-Plus")

    val protoSources by cli
            .positionalArgumentsList("source_dir_1 source_dir_2 ...",
                    "Proto Source Directories / Jars (Space delimited) ", minArgs = 1)

    val outputPath by cli
            .flagValueArgument("-o", "output_path", "Destination dir for generated sources")

    init {
        // Parse arguments or exit
        try {
            cli.parse(args)
        } catch (e: Exception) {
            exitProcess(1)
        }
    }
}

lateinit var cli: Cli

fun main(vararg args: String){
    val executionTime = measureTimeMillis { runBlocking {
        cli = Cli(args)

        val outDir = File(cli.outputPath).also { it.mkdirs() }

        val schema = SchemaLoader().run {
            for (sourcePath in cli.protoSources) {
                addSource(File(sourcePath))
            }
            load()
        }

        val fileSpecChannel = Channel<FileSpec>()

        val fileWriterJob = launch {
            fileSpecChannel.consumeEach { it.writeTo(outDir) }
        }

        val schemaConsumerJobs = listOf(
                StubRpcOverloadGenerator(schema, fileSpecChannel).consume(),
                ProtoTypeBuilderGenerator(schema, fileSpecChannel).consume(),
                MockServiceGenerator(schema,fileSpecChannel).consume()
        )

        for(job in schemaConsumerJobs) job.join()
        fileSpecChannel.close()
        fileWriterJob.join()
    }}

    println("Kroto+ Completed in ${executionTime}ms")
}
