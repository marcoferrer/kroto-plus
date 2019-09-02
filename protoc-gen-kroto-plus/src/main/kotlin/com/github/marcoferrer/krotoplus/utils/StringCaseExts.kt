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

val upperCamelCase = { it: String ->
    // We cant use CaseFormat.UPPER_CAMEL since
    // protoc is lenient with malformed field names
    if (it.contains("_"))
        it.split("_").joinToString(separator = "") { it.capitalize() } else
        it.capitalize()

}.memoize()

fun String.toUpperCamelCase(): String = upperCamelCase(this)

private val upperSnakeCase = { it: String ->

    val valueCamel = it.toUpperCamelCase()
    CaseFormat.UPPER_CAMEL
        .converterTo(CaseFormat.UPPER_UNDERSCORE)
        .convert(valueCamel) ?: error("Failed to convert '${valueCamel}' to case 'UPPER_UNDERSCORE'")
}.memoize()

fun String.toUpperSnakeCase(): String = upperSnakeCase(this)
