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

package com.github.marcoferrer.krotoplus.coroutines.utils

import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlin.test.assertEquals
import kotlin.test.fail

inline fun assertFailsWithStatus2(
    status: Status,
    message: String? = null,
    block: () -> Unit
){
    try{
        block()
        fail("Block did not fail")
    }catch (e: Throwable){
        println("assertFailsWithStatus(${e.javaClass}, message: ${e.message})")
        assertEquals(StatusRuntimeException::class.java.canonicalName, e.javaClass.canonicalName)
        require(e is StatusRuntimeException)
        message?.let { assertEquals(it,e.message) }
        assertEquals(status.code, e.status.code)
    }
}

inline fun assertFailsWithStatus(
    status: Status,
    message: String? = null,
    block: () -> Unit
){
    try{
        block()
        fail("Block did not fail")
    }catch (e: StatusRuntimeException){
//   TODO: Fix this in separate PR
//    }catch (e: Throwable){
//        assertEquals(StatusRuntimeException::class.java.canonicalName, e.javaClass.canonicalName)
//        require(e is StatusRuntimeException)
//        println("assertFailsWithStatus(${e.javaClass}, message: ${e.message})")
        message?.let { assertEquals(it,e.message) }
        assertEquals(status.code, e.status.code)
    }
}

//Default `assertFailsWith` isn't inline and doesnt support coroutines
inline fun <reified T : Throwable> assertFails(message: String? = null, block: ()-> Unit){
    try {
        block()
        fail("Block did not fail")
    } catch (e: Throwable) {
        message?.let { assertEquals(it,e.message) }
        when {
            e is AssertionError || e.cause is AssertionError -> throw e
            else -> assert(e is T){
                "Expecting ${T::class} exception: $e"
            }
        }
    }
}