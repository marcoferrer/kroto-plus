import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.proto.ProtoEnum
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.utils.addFunctions
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.*

object VarArgExtensionGenerator : Generator {

    val basePackage = "com.my.output.package"
    val outputFilename = "VarArgBuilderExts"

    override fun invoke(): PluginProtos.CodeGeneratorResponse {

        val statementTemplate = "return this.%N(values.toList())"

        val funSpecs = context.schema.protoTypes.values.asSequence()
            .filterIsInstance<ProtoMessage>()
            .associate {
                it to it.descriptorProto.fieldList.filter { field -> field.label == LABEL_REPEATED }
            }
            .filterValues { it.isNotEmpty() }
            .map { (protoMessage, repeatedFields) ->

                repeatedFields.map { field ->
                    val fieldNameCamelCase = camelCaseFieldName(field.name)

                    FunSpec.builder("addAll$fieldNameCamelCase")
                        .addStatement(statementTemplate, "addAll$fieldNameCamelCase")
                        .receiver(protoMessage.builderClassName)
                        .addParameter("values", field.javaClassName, KModifier.VARARG)
                        .build()
                }
            }
            .flatten()

        return PluginProtos.CodeGeneratorResponse.newBuilder()
            .addFile(FileSpec
                .builder(basePackage, outputFilename)
                .addFunctions(funSpecs).build()
                .toResponseFileProto()
            )
            .build()
    }

    private val DescriptorProtos.FieldDescriptorProto.javaClassName: ClassName
        get() = when (this@javaClassName.type!!) {
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> Long::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> Int::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> Double::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> Float::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> Boolean::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> String::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> TODO()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> ByteString::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE ->
                (context.schema.protoTypes[this@javaClassName.typeName] as ProtoMessage).className
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM ->
                (context.schema.protoTypes[this@javaClassName.typeName] as ProtoEnum).className
        }

    val camelCaseFieldName = { it: String ->
        // We cant use CaseFormat.UPPER_CAMEL since
        // protoc is lenient with malformed field names
        if (it.contains("_"))
            it.split("_").joinToString(separator = "") { it.capitalize() } else
            it.capitalize()

    }.memoize()
}

