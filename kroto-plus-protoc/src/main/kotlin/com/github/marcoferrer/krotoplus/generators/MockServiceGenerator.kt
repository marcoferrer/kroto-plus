package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.Manifest.name
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.schema.Service
import com.github.marcoferrer.krotoplus.schema.getGeneratedAnnotationSpec
import com.github.marcoferrer.krotoplus.schema.javaOuterClassname
import com.github.marcoferrer.krotoplus.schema.javaPackage
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*

class MockServiceGenerator(override val context: Generator.Context) : Generator {

    override val key = "mock-services"

    private val allResponseDescriptors by lazy {
        context.schema.services.flatMap {
            it.methodDefinitions.map { it.responseType.descriptorProto }
        }
    }

    override fun invoke(responseBuilder: PluginProtos.CodeGeneratorResponse.Builder) {

        context.schema.services.forEach { service ->

            buildResponseQueueOverloads(service.protoFile)?.let{
                responseBuilder.addFile( it.toResponseFileProto() )
            }

            service.buildFileSpec()?.let{
                responseBuilder.addFile( it.toResponseFileProto() )
            }
        }
    }

    private fun Service.buildFileSpec(): FileSpec? {

        val mockClassNameString = "Mock$name"

        val classBuilder = TypeSpec.classBuilder(mockClassNameString).apply {
            superclass(ClassName(enclosingServiceClassName.canonicalName, "${name}ImplBase"))
        }

        val companionBuilder = TypeSpec.companionObjectBuilder()

        //TODO Add Support for mocking streaming calls
        methodDefinitions.asSequence()
                .filterNot { it.method.clientStreaming || it.method.serverStreaming }
                .forEach { method ->
                    classBuilder.addFunction( method.toFunSpec(companionBuilder) )
                }

        companionBuilder.build()
                .takeIf { it.propertySpecs.isNotEmpty() }
                ?.let { classBuilder.companionObject(it) }

        return classBuilder
                .addAnnotation(protoFile.getGeneratedAnnotationSpec()).build()
                .takeIf { it.funSpecs.isNotEmpty() }
                ?.let { typeSpec ->

                    FileSpec.builder(protoFile.javaPackage, mockClassNameString)
                            .addComment(AutoGenerationDisclaimer)
                            .addStaticImport(
                                    "com.github.marcoferrer.krotoplus.test",
                                    "handleUnaryCall"
                            )
                            .addType(typeSpec)
                            .build()
                }
    }


    private fun Service.Method.toFunSpec(companionBuilder: TypeSpec.Builder): FunSpec {

        val queuePropertyName = "${functionName}ResponseQueue"
        val queueTypeName = ParameterizedTypeName.get(responseQueueClassName, responseClassName)
        val observerTypeName = ParameterizedTypeName.get(ClassName("io.grpc.stub", "StreamObserver"), responseClassName)

        PropertySpec.builder(queuePropertyName, queueTypeName)
                .initializer("%T()", responseQueueClassName)
                .addAnnotation(kotlin.jvm.JvmStatic::class.asClassName())
                .also { companionBuilder.addProperty(it.build()) }

        return FunSpec.builder(functionName)
                .addModifiers(com.squareup.kotlinpoet.KModifier.OVERRIDE)
                .addParameter("request", requestClassName)
                .addParameter("responseObserver", observerTypeName)
                .addStatement(
                        "handleUnaryCall(responseObserver, %N, %T.getDefaultInstance())",
                        queuePropertyName,
                        responseClassName
                )
                .build()
    }

    private fun buildResponseQueueOverloads(protoFile: DescriptorProtos.FileDescriptorProto): FileSpec? {

        val filename = "${protoFile.javaOuterClassname}ResponseQueueOverloads"
        val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage,filename)
                .addComment(AutoGenerationDisclaimer)
                .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S","-$filename")
                        .build())


        protoFile.messageTypeList
                .asSequence()
                .filter { it in allResponseDescriptors }
                .map {
                    context.schema.typesByDescriptor[it]
                            ?: throw IllegalStateException("$name was not found in schema type map.")
                }
                .forEach { protoType ->

                    val typeClassName = protoType.className
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
                                    .addMember("%S", "add${protoType.name}")
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
                                    .addMember("%S", "push${protoType.name}")
                                    .build())
                            .also {
                                fileSpecBuilder.addFunction(it.build())
                            }
                }

        return fileSpecBuilder.build()
                .takeIf { it.members.any { it is FunSpec } }
    }

    companion object {
        val responseQueueClassName = ClassName("com.github.marcoferrer.krotoplus.test","ResponseQueue")
    }
}