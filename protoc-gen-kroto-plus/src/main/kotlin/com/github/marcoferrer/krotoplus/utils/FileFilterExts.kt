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

data class RegexFilter(val include: List<Regex>, val exclude: List<Regex>) {

    fun isEmpty() = include.isEmpty() && exclude.isEmpty()

    fun matches(value: String) = when{
        include.isNotEmpty() -> include.any { it.matches(value) } && exclude.none { it.matches(value) }
        exclude.isNotEmpty() -> exclude.none { it.matches(value) }
        else -> true
    }
}

fun globPatternToRegexString(globPattern: String): String = globPattern
    .replace(".", "\\.")
    .replace("?", ".")
    .replace("*", ".*")

private val filterRegexListCache = mutableMapOf<FileFilter, RegexFilter>()

fun FileFilter.getRegexFilter(): RegexFilter =
    filterRegexListCache.getOrPut(this) {
        RegexFilter(
            include = includePathList.map { Regex("^(${globPatternToRegexString(it)})$") },
            exclude = excludePathList.map { Regex("^(${globPatternToRegexString(it)})$") }
        )
    }

fun FileFilter.matches(path: String): Boolean = getRegexFilter().matches(path)
