package com.github.marcoferrer.krotoplus.schema

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.GeneratedMessageV3
import com.squareup.kotlinpoet.ClassName

fun ProtoType.registerByName(map: MutableMap<String, ProtoType>){

    map[".$cannonicalProtoName"] = this

    if(this is ProtoMessage){
        for(nestedMessage in nestedMessageTypes)
            nestedMessage.registerByName(map)

        for(nestedEnum in nestedEnumTypes)
            nestedEnum.registerByName(map)
    }
}

fun <T : ProtoType> Sequence<T>.flattenProtoTypes(): Sequence<ProtoType> =
        flatMap {
            sequenceOf(it) + if(it is ProtoMessage)
                it.nestedMessageTypes.asSequence().flattenProtoTypes() +
                it.nestedEnumTypes.asSequence().flattenProtoTypes()
            else
                emptySequence()
        }


sealed class ProtoType{

    abstract val name: String

    abstract val descriptorProto: GeneratedMessageV3

    abstract val parentProtoType: ProtoMessage?

    abstract val fileDescriptor: DescriptorProtos.FileDescriptorProto

    val typeName: String by lazy {
        parentProtoType?.typeName?.let{ "$it.$name" } ?: name
    }

    val outerClassname: String by lazy {
        if(fileDescriptor.options.javaMultipleFiles)
            "" else fileDescriptor.javaOuterClassname
    }

    val javaPackage: String? by lazy {
        fileDescriptor.javaPackage.takeIf { it.isNotEmpty() }
    }

    val protoPackage: String? by lazy {
        fileDescriptor.protoPackage.takeIf { it.isNotEmpty() }
    }

    val cannonicalProtoName by lazy {
        protoPackage?.let { "$it.$typeName" } ?: typeName
    }

    val cannonicalJavaName by lazy {
        javaPackage?.let { "$it.$typeName" } ?: typeName
    }

    val className by lazy {
        if (outerClassname.isEmpty())
            ClassName(javaPackage.orEmpty(), typeName) else
            ClassName(javaPackage.orEmpty(), outerClassname, typeName)
    }

    val outputFilePath by lazy {
        javaPackage.orEmpty().let { pkg ->

            val basePath = pkg.replace(".", "/")

            val fileName = if (fileDescriptor.options.javaMultipleFiles) name else outerClassname

            if (basePath.isNotEmpty())
                "$basePath/$fileName.java" else "$fileName.java"
        }
    }
}


data class ProtoMessage(
        override val descriptorProto: DescriptorProtos.DescriptorProto,
        override val fileDescriptor: DescriptorProtos.FileDescriptorProto,
        override val parentProtoType: ProtoMessage? = null
) : ProtoType() {

    override val name: String
        get() = descriptorProto.name

    val nestedMessageTypes = descriptorProto.nestedTypeList.map { nestedDescriptor ->
        ProtoMessage(
                nestedDescriptor,
                fileDescriptor,
                parentProtoType = this@ProtoMessage
        )
    }

    val nestedEnumTypes = descriptorProto.enumTypeList.map { nestedDescriptor ->
        ProtoEnum(
                nestedDescriptor,
                fileDescriptor,
                parentProtoType = this@ProtoMessage
        )
    }
}

data class ProtoEnum(
        override val descriptorProto: DescriptorProtos.EnumDescriptorProto,
        override val fileDescriptor: DescriptorProtos.FileDescriptorProto,
        override val parentProtoType: ProtoMessage? = null
): ProtoType(){

    override val name: String
        get() = descriptorProto.name
}