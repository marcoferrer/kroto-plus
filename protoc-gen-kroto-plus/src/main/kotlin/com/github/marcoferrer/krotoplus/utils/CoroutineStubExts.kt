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

package com.github.marcoferrer.krotoplus.utils

import com.google.protobuf.DescriptorProtos
import com.squareup.kotlinpoet.*

val ClassName.requestParamSpec: ParameterSpec
    inline get() = ParameterSpec
        .builder("request", this)
        .defaultValue("%T.getDefaultInstance()", this)
        .build()

val ClassName.requestValueBuilderCodeBlock: CodeBlock
    inline get() = messageBuilderValueCodeBlock(
        this,
        "request",
        "block"
    )

fun messageBuilderValueCodeBlock(
    messageClassName: ClassName,
    valueVarName: String,
    builderLambdaVarName: String
): CodeBlock=
    CodeBlock.builder()
        .addStatement(
            "val %N = %T.newBuilder()",
            valueVarName,
            messageClassName
        )
        .indent()
        .addStatement(".apply(%N)", builderLambdaVarName)
        .addStatement(".build()")
        .unindent()
        .build()

fun ClassName.requestValueMethodSigCodeBlock(fields: List<DescriptorProtos.FieldDescriptorProto>): CodeBlock {

    val codeBuilder = CodeBlock.builder()
        .addStatement("val request = %T.newBuilder()", this)
        .indent()
        .apply {
            for(field in fields){
                val fieldName = upperCamelCase(field.name)

                addStatement(".set${fieldName}(${fieldName.decapitalize()})")
            }
        }

    return codeBuilder.addStatement(".build()").unindent().build()
}

val ClassName.requestValueDefaultCodeBlock: CodeBlock
    inline get() = CodeBlock.of("val request = %T.getDefaultInstance()\n", this)

val ClassName.builderLambdaTypeName: LambdaTypeName
    inline get() = LambdaTypeName.get(
        receiver = nestedClass("Builder"),
        returnType = UNIT
    )