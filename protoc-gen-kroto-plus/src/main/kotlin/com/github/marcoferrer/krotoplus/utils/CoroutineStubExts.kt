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

import com.squareup.kotlinpoet.*

val ClassName.requestParamSpec: ParameterSpec
    inline get() = ParameterSpec
        .builder("request", this)
        .defaultValue("%T.getDefaultInstance()", this)
        .build()

val ClassName.requestValueBuilderCodeBlock: CodeBlock
    inline get() = CodeBlock.of("val request = %T.newBuilder().apply(block).build()\n", this)

val ClassName.requestValueDefaultCodeBlock: CodeBlock
    inline get() = CodeBlock.of("val request = %T.getDefaultInstance()\n", this)

val ClassName.builderLambdaTypeName: LambdaTypeName
    inline get() = LambdaTypeName.get(
        receiver = nestedClass("Builder"),
        returnType = UNIT
    )