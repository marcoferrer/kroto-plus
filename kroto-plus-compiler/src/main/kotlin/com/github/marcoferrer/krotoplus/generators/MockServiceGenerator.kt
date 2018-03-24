package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.cli.appendHelpEntry
import com.github.marcoferrer.krotoplus.defaultOutputPath
import com.github.marcoferrer.krotoplus.generators.GeneratorModule.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.*
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.flagValueAction
import kotlinx.cli.flagValueArgument
import kotlinx.cli.parse
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import java.io.File
import kotlin.system.exitProcess

class MockServiceGenerator(
        override val resultChannel: SendChannel<GeneratorResult>
) : GeneratorModule {

    override var isEnabled: Boolean = false

    private val cli = CommandLineInterface("MockServices")

    private val outputPath by cli
            .flagValueArgument("-o", "output_path", "Destination directory for generated sources")

    private val outputDir by lazy {
        File(outputPath ?: defaultOutputPath).apply { mkdirs() }
    }

    override fun bindToCli(mainCli: CommandLineInterface) {
        mainCli.apply {
            flagValueAction("-MockServices", "-o|<output_path>", "Pipe delimited generator arguments") {
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
                    launch(coroutineContext) {
                        buildResponseQueueOverloads(protoFile)
                    }
                    for(service in protoFile.services())
                        launch(coroutineContext) {
                            ServiceWrapper(service, protoFile, schema).buildFileSpec()
                        }
                }
    }

    private suspend fun ServiceWrapper.buildFileSpec(){

        val mockClassNameString = "Mock$name"

        val fileSpecBuilder = FileSpec.builder(protoFile.outputPackage(), mockClassNameString)
                .addComment(AutoGenerationDisclaimer)
                .addStaticImport("com.github.marcoferrer.krotoplus.test", "handleUnaryCall")

        val classBuilder = TypeSpec.classBuilder(mockClassNameString).apply {
            superclass(ClassName(enclosingServiceClassName.canonicalName, "${name}ImplBase"))
        }

        val companionBuilder = TypeSpec.companionObjectBuilder()

        //TODO Add Support for mocking streaming calls
        methodDefinitions.asSequence()
                .filterNot { it.method.requestStreaming() || it.method.responseStreaming() }
                .forEach { method ->
                    method.buildFunSpec(classBuilder,companionBuilder)
                }

        companionBuilder.build()
                .takeIf { it.propertySpecs.isNotEmpty() }
                ?.let { classBuilder.companionObject(it) }

        classBuilder.addAnnotation(protoFile.getGeneratedAnnotationSpec()).build()
                .takeIf { it.funSpecs.isNotEmpty() }
                ?.let { typeSpec ->
                    val result = GeneratorResult(fileSpecBuilder.addType(typeSpec).build(), outputDir)
                    resultChannel.send(result)
                }
    }


    private fun ServiceWrapper.MethodWrapper.buildFunSpec(
            classBuilder: TypeSpec.Builder,
            companionBuilder:TypeSpec.Builder
    ){
        val queuePropertyName = "${functionName}ResponseQueue"
        val queueTypeName = ParameterizedTypeName.get(responseQueueClassName, responseClassName)
        val observerTypeName = ParameterizedTypeName.get(ClassName("io.grpc.stub", "StreamObserver"), responseClassName)

        PropertySpec.builder(queuePropertyName, queueTypeName)
                .initializer("%T()", responseQueueClassName)
                .addAnnotation(JvmStatic::class.asClassName())
                .also { companionBuilder.addProperty(it.build()) }

        FunSpec.builder(functionName)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("request", requestClassName)
                .addParameter("responseObserver", observerTypeName)
                .addStatement(
                        "handleUnaryCall(responseObserver, %N, %T.getDefaultInstance())",
                        queuePropertyName,
                        responseClassName
                )
                .also {
                    classBuilder.addFunction(it.build())
                }
    }

    private suspend fun buildResponseQueueOverloads(protoFile: ProtoFile){
        val filename = "${protoFile.javaOuterClassname}ResponseQueueOverloads"
        val fileSpecBuilder = FileSpec.builder(protoFile.outputPackage(),filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        protoFile.types()
                .asSequence()
                .filterNot { it is EnumType }
                .map{ it.type() }
                .forEach { protoType ->

                    val typeClassName = protoType.toClassName(protoFile)

                    val queueTypeName = ParameterizedTypeName.get(responseQueueClassName, typeClassName)

                    val builderLambdaTypeName = LambdaTypeName.get(
                            receiver = typeClassName.nestedClass("Builder"),
                            returnType = UNIT)

                    val methodBodyTemplate = "return %T.newBuilder().apply(block).build().let{ this.%NMessage(it) }"


                    /** Add Message w/ Builder Lambda */
                    FunSpec.builder("addMessage")
                            .addModifiers(KModifier.INLINE)
                            .receiver(queueTypeName)
                            .addParameter("block", builderLambdaTypeName)
                            .addStatement(methodBodyTemplate,typeClassName,"add")
                            .returns(BOOLEAN)
                            .addAnnotation(AnnotationSpec
                                .builder(JvmName::class.asClassName())
                                .addMember("%S", "add${protoType.simpleName()}")
                                .build())
                            .also {
                                fileSpecBuilder.addFunction(it.build())
                            }

                    /** Push Message w/ Builder Lambda */
                    FunSpec.builder("pushMessage")
                            .addModifiers(KModifier.INLINE)
                            .receiver(queueTypeName)
                            .addParameter("block", builderLambdaTypeName)
                            .addStatement(methodBodyTemplate,typeClassName,"push")
                            .returns(UNIT)
                            .addAnnotation(AnnotationSpec
                                    .builder(JvmName::class.asClassName())
                                    .addMember("%S", "push${protoType.simpleName()}")
                                    .build())
                            .also {
                                fileSpecBuilder.addFunction(it.build())
                            }
                }

        fileSpecBuilder.build()
                .takeIf { it.members.any { it is FunSpec } }
                ?.let { fileSpec ->
                    val result = GeneratorResult(fileSpec, outputDir)
                    resultChannel.send(result)
                }
    }

    companion object {
        val responseQueueClassName = ClassName("com.github.marcoferrer.krotoplus.test","ResponseQueue")
    }
}