package com.github.marcoferrer.krotoplus.schema

import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.ClassName

fun ProtoType.associateTo(map: MutableMap<String, ProtoType>){

    map[cannonicalProtoName] = this

    if(this is ProtoMessage){
        nestedMessageTypes.forEach { it.associateTo(map) }
        nestedEnumTypes.forEach { it.associateTo(map) }
    }
}

sealed class ProtoType{

    abstract val name: String

    abstract val parentProtoType: ProtoMessage?

    abstract val fileDescriptor: DescriptorProtos.FileDescriptorProto

    val typeName: String = buildTypeName()

    val outerClassname: String = buildOuterClassname()

    val javaPackage: String? = buildJavaPackage()

    val protoPackage: String? = buildProtoPackage()

    val cannonicalProtoName = protoPackage
            ?.let { "$it.$typeName" } ?: typeName

    val cannonicalJavaName = javaPackage
            ?.let { "$it.$typeName" } ?: typeName

    val className = if(outerClassname.isEmpty())
        ClassName(javaPackage.orEmpty(), typeName) else
        ClassName(javaPackage.orEmpty(), outerClassname, typeName)

    val outputFilePath = javaPackage.orEmpty().let { pkg ->

        val basePath = pkg.replace(".","/")

        val fileName = if(fileDescriptor.options.javaMultipleFiles) name else outerClassname

        if(basePath.isNotEmpty())
            "$basePath/$fileName.java" else "$fileName.java"
    }

    /**
     * The java class name
     *
     * If type is nested then prepend the enclosing class name.
     *
     * ENCLOSING CLASS NAME IS NOT THE SAME AS THE JAVA OUTER CLASS NAME
     */
    private fun buildTypeName(): String =
            parentProtoType?.typeName?.let{ "$it.$name" } ?: name

    /**
     * Outer class name
     */
    private fun buildOuterClassname(): String =
            if(fileDescriptor.options.javaMultipleFiles)
                fileDescriptor.javaOuterClassname else ""

    private fun buildJavaPackage(): String? =
            fileDescriptor.javaPackage.takeIf { it.isNotEmpty() }

    private fun buildProtoPackage(): String? =
            fileDescriptor.protoPackage.takeIf { it.isNotEmpty() }
}


data class ProtoMessage(
        val descriptorProto: DescriptorProtos.DescriptorProto,
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
        val descriptorProto: DescriptorProtos.EnumDescriptorProto,
        override val fileDescriptor: DescriptorProtos.FileDescriptorProto,
        override val parentProtoType: ProtoMessage? = null
): ProtoType(){

    override val name: String
        get() = descriptorProto.name
}