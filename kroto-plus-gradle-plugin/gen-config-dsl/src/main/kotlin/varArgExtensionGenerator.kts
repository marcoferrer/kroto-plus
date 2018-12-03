import com.github.marcoferrer.krotoplus.config.CompilerConfig
import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.generators.ProtoBuildersGenerator
import com.github.marcoferrer.krotoplus.proto.ProtoEnum
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.utils.addFile
import com.github.marcoferrer.krotoplus.utils.addFunctions
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*

object VarArgExtensionGenerator : Generator {

    private val gradleActionClassName = ClassName("org.gradle.api", "Action")

    override val isEnabled: Boolean
        get() = true

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val configMessage = context.schema.protoTypes.values
            .find { it.name == "CompilerConfig" } as ProtoMessage

        val funSpecs = configMessage.descriptorProto.fieldList.asSequence()
            .filter { it.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE }
            .map {
                it to requireNotNull(context.schema.protoTypes[it.typeName] as? ProtoMessage) {
                    "${it.typeName} was not found in schema type map."
                }
            }
            .filterNot { it.second.isMapEntry }
            .map { (fieldDescriptorProto, protoMessageForField) ->

                val fieldNameCamelCase = camelCaseFieldName(fieldDescriptorProto.name)
                val statementTemplate = "builder.add%N(%T.newBuilder().also{ block.execute(it) }.build())"

                val funSpecBuilder = FunSpec.builder(fieldNameCamelCase.decapitalize())
                    .addStatement(statementTemplate, fieldNameCamelCase, protoMessageForField.className)

                funSpecBuilder
                    .addParameter(
                        "block", ParameterizedTypeName
                            .get(gradleActionClassName, protoMessageForField.builderClassName)

                    )
                    .returns(UNIT)
                    .build()
            }.toList()

        val fileSpec = FileSpec.builder(configMessage.javaPackage.orEmpty(), "CompilerConfigDsl")
            .addType(
                TypeSpec.classBuilder("KrotoPlusConfigurator")
                    .addProperty(
                        PropertySpec.builder("builder", configMessage.builderClassName)
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("%T.newBuilder()", configMessage.className)
                            .build()
                    )
                    .addFunctions(funSpecs)
                    .build()
            )

//        val typeSpec =

        return PluginProtos.CodeGeneratorResponse.newBuilder()
            .addFile(fileSpec.build().toResponseFileProto())
            .build()
    }

    val camelCaseFieldName = { it: String ->
        // We cant use CaseFormat.UPPER_CAMEL since
        // protoc is lenient with malformed field names
        if (it.contains("_"))
            it.split("_").joinToString(separator = "") { it.capitalize() } else
            it.capitalize()

    }.memoize()
}

