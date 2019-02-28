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