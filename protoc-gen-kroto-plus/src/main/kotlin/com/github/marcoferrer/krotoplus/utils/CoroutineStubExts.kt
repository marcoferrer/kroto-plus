package com.github.marcoferrer.krotoplus.utils

import com.squareup.kotlinpoet.*

val ClassName.requestParamSpec: ParameterSpec
    inline get() = ParameterSpec
        .builder("request", this)
        .defaultValue("%T.getDefaultInstance()", this)
        .build()

val ClassName.requestValueBuilderCodeBlock: CodeBlock
    inline get() = CodeBlock.of("val request = %T.newBuilder().apply(block).build()\n", this)

val ClassName.builderLambdaTypeName: LambdaTypeName
    inline get() = LambdaTypeName.get(
        receiver = nestedClass("Builder"),
        returnType = UNIT
    )