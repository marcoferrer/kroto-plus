package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.config.MockServicesGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.*
import com.github.marcoferrer.krotoplus.utils.matches
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*
import io.grpc.BindableService

object MockServicesGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.mockServicesCount > 0

    private val responseQueueClassName =
            ClassName("com.github.marcoferrer.krotoplus.test", "ResponseQueue")

    private val responseQueueExts = mutableSetOf<ResponseQueueExtSpecs>()

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        val mockServiceListMap = mutableMapOf<Pair<String, String>, MutableList<String>>()
        context.schema.protoServices.forEach { service ->

            for (options in context.config.mockServicesList) {
                if (options.filter.matches(service.protoFile.name)) {
                    //TODO: Need clean up from v0.1.0
                    service.buildFileSpec(options)?.let { fileSpecBuilder ->

                        val fileSpec = fileSpecBuilder.buildResponseQueueOverloads(service).build()

                        if (options.generateServiceList) {
                            val key = options.serviceListPackage to options.serviceListName
                            mockServiceListMap.getOrPut(key) { mutableListOf() }
                                    .add(getMockServiceInstanceTemplate(fileSpec))
                        }

                        responseBuilder.addFile(fileSpec.toResponseFileProto())
                    }
                }
            }
        }

        responseQueueExts.groupBy { it.protoType.protoFile }
                .forEach{ protoFile, extsList ->
                    val fileSpecBuilder = FileSpec.builder(protoFile.javaPackage,protoFile.javaOuterClassname+"RespQueueExts")
                    for((_, addFunSpec, pushFunSpec) in extsList){
                        fileSpecBuilder.addFunction(addFunSpec)
                        fileSpecBuilder.addFunction(pushFunSpec)
                    }
                    responseBuilder.addFile(fileSpecBuilder.build().toResponseFileProto())
                }

        return responseBuilder
                .addAllFile(buildMockServiceList(mockServiceListMap))
                .build()
    }

    private fun buildMockServiceList(mockServiceListMap: Map<Pair<String, String>, List<String>>)
            : List<PluginProtos.CodeGeneratorResponse.File> {

        val builderMap = mutableMapOf<String, FileSpec.Builder>()

        mockServiceListMap.forEach { (outputPackage, propertyName), mockServiceList ->

            val initializerTemplate = mockServiceList
                    .joinToString(separator = ",\n", prefix = "listOf(\n", postfix = "\n)")

            val name = propertyName.takeIf { it.isNotEmpty() } ?: "MockServiceList"

            val propSpec = PropertySpec.builder(name, ParameterizedTypeName
                    .get(List::class.asClassName(), BindableService::class.asClassName()))
                    .initializer(initializerTemplate)
                    .build()

            builderMap.getOrPut(outputPackage) {
                FileSpec.builder(outputPackage, "MockServices")
            }.addProperty(propSpec)
        }

        return builderMap.values.map { it.build().toResponseFileProto() }
    }

    private fun getMockServiceInstanceTemplate(fileSpec: FileSpec): String = fileSpec.members
            .filterIsInstance<TypeSpec>()
            .first {
                it.name.orEmpty().startsWith("Mock") &&
                        (it.kind == TypeSpec.Kind.CLASS || it.kind == TypeSpec.Kind.OBJECT)
            }
            .let {
                val filePackage = fileSpec.packageName
                        .takeIf { it.isNotEmpty() }
                        ?.let { "$it." }
                        .orEmpty()

                filePackage + it.name + if (it.kind == TypeSpec.Kind.CLASS) "()" else ""
            }


    private fun ProtoService.buildFileSpec(options: MockServicesGenOptions): FileSpec.Builder? {

        val mockClassNameString = "Mock$name"
        val superClassName = ClassName(enclosingServiceClassName.canonicalName, "${name}ImplBase")

        val classBuilder = if (options.implementAsObject)
            TypeSpec.objectBuilder(mockClassNameString).superclass(superClassName) else
            TypeSpec.classBuilder(mockClassNameString).superclass(superClassName).addModifiers(KModifier.OPEN)

        val objectBuilder = if (options.implementAsObject)
            classBuilder else TypeSpec.companionObjectBuilder()

        objectBuilder.addSuperinterface(ClassName(
                "com.github.marcoferrer.krotoplus.test",
                "MockServiceHelper"
        ))

        //TODO Add Support for mocking streaming calls
        val propToFunSpecsMap = methodDefinitions.asSequence()
                .filterNot { it.descriptorProto.clientStreaming || it.descriptorProto.serverStreaming }
                .associate { method ->
                    val methodTypes = method.getMethodTypes()
                    propSpecFrom(methodTypes) to method.toFunSpec(methodTypes, isOpen = !options.implementAsObject)
                }

        classBuilder.addFunctions(propToFunSpecsMap.values)
        objectBuilder.apply {
            implementMockServiceHelper(propToFunSpecsMap.keys)
            addProperties(propToFunSpecsMap.keys)
        }

        if (!options.implementAsObject) {
            objectBuilder
                    .build()
                    .takeIf { it.propertySpecs.isNotEmpty() }
                    ?.let { classBuilder.companionObject(it) }
        }

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
                }
    }

    private data class MethodTypes(
            val queuePropertyName: String,
            val queueTypeName: ParameterizedTypeName,
            val observerTypeName: ParameterizedTypeName
    )

    private fun ProtoMethod.getMethodTypes() =
            MethodTypes(
                    queuePropertyName = "${functionName}ResponseQueue",
                    queueTypeName = ParameterizedTypeName.get(responseQueueClassName, responseClassName),
                    observerTypeName = ParameterizedTypeName.get(ClassName("io.grpc.stub", "StreamObserver"), responseClassName)
            )

    private fun propSpecFrom(methodTypes: MethodTypes): PropertySpec {
        val (queuePropertyName, queueTypeName, _) = methodTypes

        return PropertySpec.builder(queuePropertyName, queueTypeName)
                .initializer("%T()", responseQueueClassName)
                .addAnnotation(kotlin.jvm.JvmStatic::class.asClassName())
                .build()
    }

    private fun ProtoMethod.toFunSpec(methodTypes: MethodTypes, isOpen: Boolean): FunSpec {

        val (queuePropertyName, _, observerTypeName) = methodTypes

        return FunSpec.builder(functionName)
                .addModifiers(KModifier.OVERRIDE)
                .apply { if(isOpen) addModifiers(KModifier.OPEN) }
                .addParameter("request", requestClassName )
                .addParameter("responseObserver", observerTypeName )
                .addStatement(
                        "handleUnaryCall(responseObserver, %N, %T.getDefaultInstance())",
                        queuePropertyName,
                        responseClassName
                )
                .build()
    }

    fun TypeSpec.Builder.implementMockServiceHelper(propSpecList: Set<PropertySpec>) {
        if (propSpecList.isNotEmpty())
            addFunction(FunSpec.builder("clearQueues")
                    .addModifiers(KModifier.OVERRIDE)
                    .apply {
                        for (propSpec in propSpecList)
                            addStatement("${propSpec.name}.clear()")
                    }
                    .build())
    }

    private fun FileSpec.Builder.buildResponseQueueOverloads(service: ProtoService): FileSpec.Builder {

        service.methodDefinitions
                .asSequence().map { it.responseType }.distinct().filterIsInstance<ProtoMessage>()
                .forEach { protoType ->

                    val queueClassName = ParameterizedTypeName.get(responseQueueClassName, protoType.className)
                    val builderLambdaTypeName = LambdaTypeName.get(
                            receiver = protoType.builderClassName,
                            returnType = UNIT)

                    val methodBodyTemplate = "return %T.newBuilder().apply(block).build().let{ this.%NMessage(it) }"

                    /** Add Message w/ Builder Lambda */
                    val addFunSpec = FunSpec.builder("addMessage")
                            .addModifiers(KModifier.INLINE)
                            .receiver(queueClassName)
                            .addParameter("block", builderLambdaTypeName)
                            .addStatement(methodBodyTemplate, protoType.className, "add")
                            .returns(BOOLEAN)
                            .addAnnotation(AnnotationSpec
                                    .builder(JvmName::class.asClassName())
                                    .addMember("%S", "add${protoType.name}")
                                    .build())
                            .build()

                    /** Push Message w/ Builder Lambda */
                    val pushFunSpec = FunSpec.builder("pushMessage")
                            .addModifiers(KModifier.INLINE)
                            .receiver(queueClassName)
                            .addParameter("block", builderLambdaTypeName)
                            .addStatement(methodBodyTemplate, protoType.className, "push")
                            .returns(UNIT)
                            .addAnnotation(AnnotationSpec
                                    .builder(JvmName::class.asClassName())
                                    .addMember("%S", "push${protoType.name}")
                                    .build())
                            .build()

                    responseQueueExts.add(ResponseQueueExtSpecs(protoType,addFunSpec,pushFunSpec))
                }

        return this@buildResponseQueueOverloads
    }

    data class ResponseQueueExtSpecs(val protoType: ProtoType, val addFunSpec: FunSpec, val pushFunSPec: FunSpec)
}