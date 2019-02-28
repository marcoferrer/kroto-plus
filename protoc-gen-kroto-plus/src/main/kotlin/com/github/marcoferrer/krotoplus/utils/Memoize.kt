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

class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x) { f(x) }
    }
}

class MemoizeExt1<in T, out R>(val f: T.() -> R){
    private val values = mutableMapOf<T, R>()
    operator fun T.invoke(): R {
        return values.getOrPut(this) { this.f() }
    }
}


fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)

fun <T, R> (T.() -> R).memoizeExt(): MemoizeExt1<T,R> = MemoizeExt1(this)