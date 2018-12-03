package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.*
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asClassName
import java.lang.IllegalArgumentException


fun DescriptorProtos.FieldDescriptorProto.getFieldClassName(schema: Schema): ClassName =
    when (type!!) {
        TYPE_INT64,
        TYPE_FIXED64,
        TYPE_SFIXED64,
        TYPE_SINT64,
        TYPE_UINT64 -> Long::class.asClassName()
        TYPE_INT32,
        TYPE_UINT32,
        TYPE_SFIXED32,
        TYPE_SINT32,
        TYPE_FIXED32 -> Int::class.asClassName()
        TYPE_DOUBLE -> Double::class.asClassName()
        TYPE_FLOAT -> Float::class.asClassName()
        TYPE_BOOL -> Boolean::class.asClassName()
        TYPE_STRING -> String::class.asClassName()
        TYPE_BYTES -> ByteString::class.asClassName()

        TYPE_GROUP ->
            TODO("ClassName for field type 'GROUP' is not yet implemented")

        TYPE_MESSAGE -> (schema.protoTypes[typeName] as ProtoMessage).className
        TYPE_ENUM -> (schema.protoTypes[typeName] as ProtoEnum).className
    }

//need to check the actual message in schema
fun DescriptorProtos.FieldDescriptorProto.isMapField(schema:Schema): Boolean =
    type == TYPE_MESSAGE && getFieldProtoMessage(schema).isMapEntry

val DescriptorProtos.FieldDescriptorProto.isRepeated: Boolean
    get() = label == LABEL_REPEATED


fun DescriptorProtos.FieldDescriptorProto.getParamaterizeTypeName(schema: Schema): ParameterizedTypeName =
    when{
        isMapField(schema) -> {
            val message = getFieldProtoMessage(schema)
            val keyCn = message.descriptorProto.getField(0)
                .getFieldClassName(schema)

            val valueCn = message.descriptorProto.getField(1)
                .getFieldClassName(schema)

            ParameterizedTypeName.get(Map::class.asClassName(), keyCn, valueCn)
        }
        label == LABEL_REPEATED -> {

            ParameterizedTypeName.get(List::class.asClassName(), getFieldClassName(schema))
        }
        else -> throw IllegalArgumentException("Only 'REPEATED' or 'MapEntry' fields can be parameterized")
    }


fun DescriptorProtos.FieldDescriptorProto.getFieldProtoMessage(schema: Schema): ProtoMessage =
    requireNotNull(schema.protoTypes[typeName] as? ProtoMessage){
        "$typeName was not found in schema type map."
    }


fun DescriptorProtos.FieldDescriptorProto.getDefaultInitializer(schema: Schema): String =
    when {
        isMapField(schema) -> "emptyMap()"
        isRepeated -> "emptyList()"
        else -> when (type!!) {
            TYPE_INT64,
            TYPE_FIXED64,
            TYPE_SFIXED64,
            TYPE_SINT64,
            TYPE_UINT64,
            TYPE_INT32,
            TYPE_UINT32,
            TYPE_SFIXED32,
            TYPE_SINT32,
            TYPE_FIXED32,
            TYPE_DOUBLE,
            TYPE_FLOAT -> "0"
            TYPE_BOOL -> "false"
            TYPE_STRING -> "\"\""
            TYPE_BYTES,
            TYPE_GROUP ->
                TODO("DefaultInitializer for field type '${type.name}' is not yet implemented")

            TYPE_MESSAGE -> (schema.protoTypes[typeName] as ProtoMessage)
                .let { it.canonicalJavaName + ".defaultInstance" }


            TYPE_ENUM -> (schema.protoTypes[typeName] as ProtoEnum)
                .let { enum ->
                    "${enum.canonicalJavaName}.${enum.descriptorProto.valueList.first().name}"
                }
        }
    }