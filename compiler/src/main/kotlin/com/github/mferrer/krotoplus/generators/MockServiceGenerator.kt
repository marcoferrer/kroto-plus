package com.github.mferrer.krotoplus.generators

import com.github.mferrer.krotoplus.Manifest
import com.github.mferrer.krotoplus.generators.FileSpecProducer.Companion.AutoGenerationDisclaimer
import com.github.mferrer.krotoplus.schema.ServiceWrapper
import com.squareup.kotlinpoet.*
import com.squareup.wire.schema.Schema
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import javax.annotation.Generated

class MockServiceGenerator(
        override val schema: Schema, override val fileSpecChannel: Channel<FileSpec>
) : SchemaConsumer, FileSpecProducer {

    override fun consume() = launch {
        schema.protoFiles()
                .asSequence()
                .filter { it.javaPackage() != "com.google.protobuf" }
                .forEach { protoFile ->

                    for (service in protoFile.services())
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

        for (method in methodDefinitions)
            method.buildFunSpec(classBuilder,companionBuilder)

        companionBuilder.build()
                .takeIf { it.propertySpecs.isNotEmpty() }
                ?.let { classBuilder.companionObject(it) }

        fileSpecBuilder.addType(classBuilder
                .addAnnotation(AnnotationSpec.builder(Generated::class.asClassName())
                        .addMember("value = [%S]", "by ${Manifest.implTitle} (version ${Manifest.implVersion})")
                        .addMember("comments = %S", "Source: ${protoFile.location().path()}")
                        .build())
                .build())

        fileSpecChannel.send(fileSpecBuilder.build())
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
                .also { classBuilder.addFunction(it.build()) }
    }

    companion object {
        val responseQueueClassName = ClassName("com.github.mferrer.krotoplus.test","ResponseQueue")
    }
}