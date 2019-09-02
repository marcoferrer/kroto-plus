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

import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

fun PluginProtos.CodeGeneratorResponse.Builder.addFile(block: PluginProtos.CodeGeneratorResponse.File.Builder.() -> Unit)
        : PluginProtos.CodeGeneratorResponse.Builder {
    this.addFile(PluginProtos.CodeGeneratorResponse.File.newBuilder().apply(block).build())
    return this
}

fun PluginProtos.CodeGeneratorResponse.Builder.addFiles(files: Iterable<PluginProtos.CodeGeneratorResponse.File>)
        : PluginProtos.CodeGeneratorResponse.Builder {
    for (file in files) addFile(file)
    return this
}

fun FileSpec.Builder.addFunctions(funSpecs: Iterable<FunSpec>): FileSpec.Builder {
    for (funSpec in funSpecs) this.addFunction(funSpec)
    return this
}

fun FileSpec.Builder.addTypes(typeSpecs: Iterable<TypeSpec>): FileSpec.Builder {
    for (typeSpec in typeSpecs) this.addType(typeSpec)
    return this
}

fun <T> FunSpec.Builder.addForEach(list: List<T>, block: FunSpec.Builder.(T) -> Unit): FunSpec.Builder = apply {
    for(item in list){
        block(item)
    }
}

fun <T> TypeSpec.Builder.addForEach(list: List<T>, block: TypeSpec.Builder.(T) -> Unit): TypeSpec.Builder = apply {
    for(item in list){
        block(item)
    }
}