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

import com.github.marcoferrer.krotoplus.config.FileFilter
import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.generators.GeneratorContext
import com.google.protobuf.compiler.PluginProtos
import kotlin.test.Test
import kotlin.test.assertFalse

class GeneratorTests {


    @Test
    fun `isFileToGenerate falls back to codegen request when file filter is not provided`(){

        val filePath = "src/test/a.proto"
        val mockContext = GeneratorContext(
            PluginProtos.CodeGeneratorRequest.newBuilder()
                .addFileToGenerate(filePath)
                .build()
        )

        val generator = object : Generator {
            override val context: GeneratorContext = mockContext
            override fun invoke(): PluginProtos.CodeGeneratorResponse { TODO() }
        }

        assert(generator.isFileToGenerate(filePath, FileFilter.getDefaultInstance()))
    }

    @Test
    fun `isFileToGenerate ignores codegen request when file filter is provided`(){

        val filePath = "src/test/a.proto"
        val mockContext = GeneratorContext(
            PluginProtos.CodeGeneratorRequest.newBuilder()
                .addFileToGenerate(filePath)
                .build()
        )

        val filter = FileFilter.newBuilder().addIncludePath("").build()

        val generator = object : Generator {
            override val context: GeneratorContext = mockContext
            override fun invoke(): PluginProtos.CodeGeneratorResponse { TODO() }
        }

        assertFalse(generator.isFileToGenerate(filePath, filter))
    }

}