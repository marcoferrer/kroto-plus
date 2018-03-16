package com.github.mferrer.krotoplus.generators

import com.github.mferrer.krotoplus.generators.FileSpecProducer.Companion.AutoGenerationDisclaimer
import com.github.mferrer.krotoplus.schema.*
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.joinChildren
import kotlinx.coroutines.experimental.launch

class MockServiceGenerator(
        override val schema: Schema, override val fileSpecChannel: Channel<FileSpec>
) : SchemaConsumer, FileSpecProducer {

    override fun consume() = launch {
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

        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage(), mockClassNameString)
                .addComment(AutoGenerationDisclaimer)
                .addStaticImport("com.github.mferrer.krotoplus.test", "handleUnaryCall")

        val classBuilder = TypeSpec.classBuilder(mockClassNameString).apply {
            superclass(ClassName(enclosingServiceClassName.canonicalName, "${name}ImplBase"))
        }

        val companionBuilder = TypeSpec.companionObjectBuilder()

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
                    fileSpecChannel.send(fileSpecBuilder.addType(typeSpec).build())
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
        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage(),filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())

        protoFile.types()
                .asSequence()
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

        //TODO Check for empty file before emitting fileSpec
        fileSpecChannel.send(fileSpecBuilder.build())
    }

    companion object {
        val responseQueueClassName = ClassName("com.github.mferrer.krotoplus.test","ResponseQueue")
    }
}