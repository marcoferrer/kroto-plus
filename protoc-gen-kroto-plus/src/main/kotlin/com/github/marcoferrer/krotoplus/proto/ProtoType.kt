package com.github.marcoferrer.krotoplus.proto

import com.github.marcoferrer.krotoplus.config.InsertionPoint
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.common.base.CaseFormat
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.ClassName

sealed class ProtoType(
    val name: String,
    val protoFile: ProtoFile,
    val parentProtoType: ProtoMessage?
) : Schema.DescriptorWrapper {

    val enclosingTypeName: String? = parentProtoType?.let { parent ->
        parent.enclosingTypeName?.let { "$it." }.orEmpty() +
                parent.name
    }

    val outerClassname: String? = protoFile.javaOuterClassname.takeUnless {
        protoFile.javaMultipleFiles || it.isEmpty()
    }

    val javaPackage: String? = protoFile.javaPackage.takeIf { it.isNotEmpty() }

    val protoPackage: String? = protoFile.protoPackage.takeIf { it.isNotEmpty() }

    val canonicalProtoName: String =
        protoPackage?.let { "$it." }.orEmpty() +
                enclosingTypeName?.let { "$it." }.orEmpty() +
                name

    val canonicalJavaName: String =
        javaPackage?.let { "$it." }.orEmpty() +
                outerClassname?.let { "$it." }.orEmpty() +
                enclosingTypeName?.let { "$it." }.orEmpty() +
                name

    val parentClassName: ClassName? = parentProtoType?.className
        ?: outerClassname?.let { ClassName(javaPackage.orEmpty(), it) }

    val className: ClassName = parentClassName?.nestedClass(name)
        ?: ClassName(javaPackage.orEmpty(), name)

    val outputFilePath: String = parentProtoType?.outputFilePath
        ?: javaPackage.orEmpty().let { pkg ->
            val basePath = pkg.replace(".", "/")
            val fileName = if (protoFile.javaMultipleFiles) name else outerClassname

            if (basePath.isNotEmpty())
                "$basePath/$fileName.java" else "$fileName.java"
        }
}


class ProtoMessage(
    override val descriptorProto: DescriptorProtos.DescriptorProto,
    protoFile: ProtoFile,
    parentProtoType: ProtoMessage? = null
) : ProtoType(
    descriptorProto.name,
    protoFile,
    parentProtoType
) {

    val isMapEntry get() = descriptorProto.options.mapEntry

    val builderClassName = className.nestedClass("Builder")

    val nestedMessageTypes = descriptorProto.nestedTypeList.map { nestedDescriptor ->
        ProtoMessage(
            nestedDescriptor,
            protoFile,
            parentProtoType = this@ProtoMessage
        )
    }

    val nestedEnumTypes = descriptorProto.enumTypeList.map { nestedDescriptor ->
        ProtoEnum(
            nestedDescriptor,
            protoFile,
            parentProtoType = this@ProtoMessage
        )
    }
}

class ProtoEnum(
    override val descriptorProto: DescriptorProtos.EnumDescriptorProto,
    protoFile: ProtoFile,
    parentProtoType: ProtoMessage? = null
) : ProtoType(
    descriptorProto.name,
    protoFile,
    parentProtoType
)

fun Schema.DescriptorWrapper.supportsInsertionPoint(point: InsertionPoint): Boolean =
    when (this) {
        is ProtoMessage -> (
                point == InsertionPoint.INTERFACE_EXTENDS ||
                        point == InsertionPoint.MESSAGE_IMPLEMENTS ||
                        point == InsertionPoint.BUILDER_IMPLEMENTS ||
                        point == InsertionPoint.BUILDER_SCOPE ||
                        point == InsertionPoint.CLASS_SCOPE) && !this.isMapEntry

        is ProtoEnum -> point == InsertionPoint.ENUM_SCOPE
        is ProtoFile -> point == InsertionPoint.OUTER_CLASS_SCOPE
        else -> false
    }

fun <T : ProtoType> Sequence<T>.flattenProtoTypes(): Sequence<ProtoType> =
    flatMap {
        sequenceOf(it) + if (it is ProtoMessage)
            it.nestedMessageTypes.asSequence().flattenProtoTypes() +
                    it.nestedEnumTypes.asSequence().flattenProtoTypes()
        else
            emptySequence()
    }

