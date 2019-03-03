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
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import kotlin.test.assertEquals
import kotlin.test.fail

inline fun assertCancellationError(block: ()-> Unit){
    try {
        block()
        fail("Block did not fail")
    } catch (e: Throwable) {
        when {
            e is AssertionError || e.cause is AssertionError -> throw e
            else -> assert(e is CancellationException){
                "Expecting cancellation exception: $e"
            }
        }
    }
}

inline fun assertFailsWithStatusCode(
    code: Status.Code,
    message: String? = null,
    block: () -> Unit
){
    try{
        block()
    }catch (e: Throwable){
        message?.let { assertEquals(it,e.message) }
        when(e){
            is StatusRuntimeException -> assertEquals(code, e.status.code)
            else -> throw e
        }
    }
}