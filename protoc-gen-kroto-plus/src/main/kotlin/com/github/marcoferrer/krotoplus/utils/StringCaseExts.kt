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

import com.google.common.base.CaseFormat

/**
 * This is the Kotlin implementation of the C++ UnderscoresToCamelCase function that is used by the protobuf java
 * compiler in the java_helpers.cc file.
 * See https://github.com/protocolbuffers/protobuf/blob/master/src/google/protobuf/compiler/java/java_helpers.cc#L168-L203
 * The exact same logic was ported to ensure the generated names match the generated Java names.
 */
val upperCamelCase = { it: String ->
    StringBuilder().apply {
        var capNextLetter = true
        it.forEach { c ->
            when (c) {
                in 'a'..'z' -> {
                    if (capNextLetter) {
                        append(c.toUpperCase())
                    } else {
                        append(c)
                    }
                    capNextLetter = false
                }
                in 'A'..'Z' -> {
                    append(c)
                    capNextLetter = false
                }
                in '0'..'9' -> {
                    append(c)
                    capNextLetter = true
                }
                else -> {
                    capNextLetter = true
                }
            }
        }
    }.toString()
}.memoize()

fun String.toUpperCamelCase(): String = upperCamelCase(this)

private val upperSnakeCase = { it: String ->
    val valueCamel = it.toUpperCamelCase()
    CaseFormat.UPPER_CAMEL
        .converterTo(CaseFormat.UPPER_UNDERSCORE)
        .convert(valueCamel) ?: error("Failed to convert '${valueCamel}' to case 'UPPER_UNDERSCORE'")
}.memoize()

fun String.toUpperSnakeCase(): String = upperSnakeCase(this)
