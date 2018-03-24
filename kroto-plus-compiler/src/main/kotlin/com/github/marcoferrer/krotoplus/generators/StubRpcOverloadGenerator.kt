package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.cli.appendHelpEntry
import com.github.marcoferrer.krotoplus.defaultOutputPath
import com.github.marcoferrer.krotoplus.generators.GeneratorModule.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.ServiceWrapper
import com.github.marcoferrer.krotoplus.schema.isCommonProtoFile
import com.github.marcoferrer.krotoplus.schema.isEmptyMessage
import com.github.marcoferrer.krotoplus.schema.outputPackage
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

    private val supportCoroutines by cli
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
                .builder(serviceWrapper.protoFile.outputPackage(), filename)
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
        when{
            //Unary
            methodWrapper.isUnary ->
                buildUnaryOverloads(methodWrapper,fileSpecBuilder)

            //Server Streaming //TODO Add support for Blocking Server Streaming
            supportCoroutines && methodWrapper.isServerStream ->
                buildServerStreamingOverloads(methodWrapper,fileSpecBuilder)

            //Bidi && Client Streaming
            supportCoroutines && (methodWrapper.isBidi || methodWrapper.isClientStream)->
                buildBidiStreamingOverloads(methodWrapper,fileSpecBuilder)
        }
    }

    private fun buildUnaryOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ) {
        val funSpecBuilder = FunSpec.builder(methodWrapper.functionName)

        if(!methodWrapper.method.requestType().isEmptyMessage) {
            funSpecBuilder
                    .addModifiers(KModifier.INLINE)
                    .addParameter("block", LambdaTypeName.get(
                            receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                            returnType = UNIT))
        }

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.futureStubClassName)
                .addCode(methodWrapper.requestValueCodeBlock())
                .addStatement("return %N(request)", methodWrapper.functionName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.google.common.util.concurrent","ListenableFuture"),
                        methodWrapper.responseClassName))
                .also { fileSpecBuilder.addFunction(it.build()) }

        funSpecBuilder
                .receiver(methodWrapper.serviceWrapper.blockingStubClassName)
                .returns(methodWrapper.responseClassName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        if(supportCoroutines)
            buildSuspendingUnaryOverloads(methodWrapper,fileSpecBuilder)
    }

    private fun buildSuspendingUnaryOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ) {

        fileSpecBuilder
                .addStaticImport("com.github.marcoferrer.krotoplus.coroutines","suspendingUnaryCallObserver")

        FunSpec.builder(methodWrapper.functionName)
                .addModifiers(KModifier.SUSPEND)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .apply {
                    if(!methodWrapper.method.requestType().isEmptyMessage) {
                        addParameter("request", methodWrapper.requestClassName)
                    }else{
                        addCode(methodWrapper.requestValueCodeBlock())
                    }
                }
                .returns(methodWrapper.responseClassName)
                .addStatement(
                        "return %W suspendingUnaryCallObserver{ observer -> %N(request,observer) }",
                        methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }

        if(!methodWrapper.method.requestType().isEmptyMessage) {
            FunSpec.builder(methodWrapper.functionName)
                    .addModifiers(KModifier.SUSPEND)
                    .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                    .apply {
                        if (!methodWrapper.method.requestType().isEmptyMessage) {
                            addModifiers(KModifier.INLINE)
                            addParameter("block", LambdaTypeName.get(
                                    receiver = methodWrapper.requestClassName.nestedClass("Builder"),
                                    returnType = UNIT))
                        }
                    }
                    .returns(methodWrapper.responseClassName)
                    .addCode(methodWrapper.requestValueCodeBlock())
                    .addStatement("return %N(request)", methodWrapper.functionName)
                    .also { fileSpecBuilder.addFunction(it.build()) }
        }
    }

    private fun buildServerStreamingOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ){

        val inboundChannelClassName = ClassName("com.github.marcoferrer.krotoplus.coroutines","InboundStreamChannel")

        val returnType = ParameterizedTypeName.get(inboundChannelClassName,methodWrapper.responseClassName)

        FunSpec.builder(methodWrapper.functionName)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .apply {
                    if(!methodWrapper.method.requestType().isEmptyMessage) {
                        addParameter("request", methodWrapper.requestClassName)
                    }else{
                        addCode(methodWrapper.requestValueCodeBlock())
                    }
                }
                .returns(returnType)
                .addStatement(
                        "return %T().also { observer -> %N(request,observer) }",
                        returnType,
                        methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
    }


    private fun buildBidiStreamingOverloads(
            methodWrapper: ServiceWrapper.MethodWrapper,
            fileSpecBuilder: FileSpec.Builder
    ){

        fileSpecBuilder
                .addStaticImport("com.github.marcoferrer.krotoplus.coroutines","bidiCallChannel")

        FunSpec.builder(methodWrapper.functionName)
                .receiver(methodWrapper.serviceWrapper.asyncStubClassName)
                .returns(ParameterizedTypeName.get(
                        ClassName("com.github.marcoferrer.krotoplus.coroutines","RpcBidiChannel"),
                        methodWrapper.requestClassName,
                        methodWrapper.responseClassName
                ))
                .addStatement("return %W bidiCallChannel{ responseObserver -> %N(responseObserver) }",
                        methodWrapper.functionName)
                .also { fileSpecBuilder.addFunction(it.build()) }
    }

    private fun ServiceWrapper.MethodWrapper.requestValueCodeBlock(): CodeBlock{
        val requestValueTemplate = if(method.requestType().isEmptyMessage)
            "val request = %T.getDefaultInstance()\n" else "val request = %T.newBuilder().apply(block).build()\n"

        return CodeBlock.of(requestValueTemplate, requestClassName)
    }
}

private val ServiceWrapper.MethodWrapper.isUnary
    get() = !method.requestStreaming() && !method.responseStreaming()

private val ServiceWrapper.MethodWrapper.isBidi
    get() = method.requestStreaming() && method.responseStreaming()

private val ServiceWrapper.MethodWrapper.isServerStream
    get() = !method.requestStreaming() && method.responseStreaming()

private val ServiceWrapper.MethodWrapper.isClientStream
    get() = method.requestStreaming() && !method.responseStreaming()

