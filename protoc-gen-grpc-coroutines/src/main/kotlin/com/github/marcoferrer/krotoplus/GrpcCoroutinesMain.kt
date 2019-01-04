@file:JvmName("GrpcCoroutinesMain")

package com.github.marcoferrer.krotoplus

import com.github.marcoferrer.krotoplus.config.CompilerConfig
import com.github.marcoferrer.krotoplus.config.GrpcCoroutinesGenOptions
import com.github.marcoferrer.krotoplus.generators.GrpcCoroutinesGenerator
import com.github.marcoferrer.krotoplus.generators.initializeContext

fun main(args: Array<String>) {

    initializeContext(
        CompilerConfig.newBuilder()
            .addGrpcCoroutines(GrpcCoroutinesGenOptions.getDefaultInstance())
            .build()
    )

    GrpcCoroutinesGenerator().writeTo(System.out)

    System.out.flush()
}