package com.github.mferrer.krotoplus.generators

import com.github.mferrer.krotoplus.cli.appendHelpEntry
import com.github.mferrer.krotoplus.defaultOutputPath
import com.github.mferrer.krotoplus.generators.GeneratorModule.Companion.AutoGenerationDisclaimer
import com.github.mferrer.krotoplus.schema.ServiceWrapper
import com.github.mferrer.krotoplus.schema.isCommonProtoFile
import com.github.mferrer.krotoplus.schema.isEmptyMessage
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.Schema
import kotlinx.cli.*
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import java.io.File
import kotlin.system.exitProcess

class StubRpcOverloadGenerator(
        override val resultChannel: SendChannel<GeneratorResult>
) : GeneratorModule {

    override var isEnabled = false

    private val cli = CommandLineInterface("StubOverloads")

    private val generateCoroutineSupport by cli
            .flagArgument("-coroutines", "Generate coroutine integrated overloads")

    private val outputPath by cli
            .flagValueArgument("-o", "output_path", "Destination directory for generated sources")

    private val outputDir by lazy {
        File(outputPath ?: defaultOutputPath).apply { mkdirs() }
    }

    override fun bindToCli(mainCli: CommandLineInterface) {
        mainCli.apply {
            flagValueAction("-StubOverloads", "-o|<output_path>|-coroutines", "Pipe delimited generator arguments") {
                try{
                    cli.parse(it.split("|"))
                    isEnabled = true
                }catch (e:Exception){
                    exitProcess(1)
                }
            }
            appendHelpEntry(cli)
        }
    }

    override fun generate(schema: Schema) = launch {
        schema.protoFiles()
                .asSequence()
                .filterNot { it.isCommonProtoFile }
                .forEach { protoFile ->
                    for (service in protoFile.services())
                        launch(coroutineContext) {
                            buildFileSpec(ServiceWrapper(service, protoFile, schema))
                        }
                }
    }

    private suspend fun buildFileSpec(serviceWrapper: ServiceWrapper) {
        val filename = "${serviceWrapper.name}RpcOverloads"
        val fileSpecBuilder = FileSpec
                .builder(serviceWrapper.protoFile.javaPackage(), filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())


        for(methodWrappers in serviceWrapper.methodDefinitions)
            buildFunSpecs(methodWrappers, fileSpecBuilder)

        fileSpecBuilder.build().takeIf { it.members.isNotEmpty() }?.let {
            resultChannel.send(GeneratorResult(it, outputDir))
        }
    }

    private fun buildFunSpecs(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ){
        if(methodWrapper.method.requestStreaming() || methodWrapper.method.responseStreaming())
            return //TODO Add support for streaming rpc calls

        val funSpecBuilder = FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.INLINE)
                .addParameter("block",LambdaTypeName.get(
                        receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                        returnType = UNIT))

        val requestValueTemplate = if(methodWrapper.method.requestType().isEmptyMessage)
            "val request = %T.getDefaultInstance()\n" else "val request = %T.newBuilder().apply(block).build()\n"

        val requestValueCb = CodeBlock.of(requestValueTemplate, methodWrapper.requestClassName)

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.futureStubClassName)
                .addCode(requestValueCb)
                .addStatement("return %N(request)", methodWrapper.functionName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.google.common.util.concurrent","ListenableFuture"),
                        methodWrapper.responseClassName))
                .also { fileSpecBuilder.addFunction(it.build()) }

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.blockingStubClassName)
                .returns(methodWrapper.responseClassName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        if(generateCoroutineSupport)
            buildAsyncStubOverloads(methodWrapper,fileSpecBuilder)
    }

    private fun buildAsyncStubOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ) {

        fileSpecBuilder
                .addStaticImport("com.github.mferrer.krotoplus.coroutines","suspendingUnaryCallObserver")

        val requestValueTemplate = if(methodWrapper.method.requestType().isEmptyMessage)
            "val request = %T.getDefaultInstance()\n" else "val request = %T.newBuilder().apply(block).build()\n"

        val requestValueCb = CodeBlock.of(requestValueTemplate,methodWrapper.requestClassName)

        FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .addParameter("request",methodWrapper.requestClassName)
                .returns(methodWrapper.responseClassName)
                .addStatement(
                        "return %W suspendingUnaryCallObserver{ observer -> %N(request,observer) }",
                        methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.SUSPEND,KModifier.INLINE)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .addParameter("block",LambdaTypeName.get(
                        receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                        returnType = UNIT))
                .returns(methodWrapper.responseClassName)
                .addCode(requestValueCb)
                .addStatement("return %N(request)", methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
    }
}