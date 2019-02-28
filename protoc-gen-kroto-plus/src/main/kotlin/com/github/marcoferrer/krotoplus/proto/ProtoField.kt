/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.marcoferrer.krotoplus.proto

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName


fun DescriptorProtos.FieldDescriptorProto.getFieldClassName(schema: Schema): ClassName =
    when (type!!) {
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
            (schema.protoTypes[typeName] as ProtoMessage).className
        DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM ->
            (schema.protoTypes[typeName] as ProtoEnum).className
    }