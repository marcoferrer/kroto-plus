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

package com.github.marcoferrer.krotoplus.coroutines.call

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CompletableDeferredExtsTests {

    @Test
    fun `Deferred completed successfully on next value`(){
        val value = "test_value"
        val result = spyk(CompletableDeferred<String>())

        with(result.toStreamObserver()){
            onNext(value)
            onCompleted()
        }

        runBlocking {
            assertEquals(value,result.await())
        }

        verify(exactly = 1) { result.complete(value) }
    }

    @Test
    fun `Deferred completed exceptionally on next value`(){
        val errorMessage = "error_message"
        val error = IllegalArgumentException(errorMessage)
        val result = spyk(CompletableDeferred<String>())

        with(result.toStreamObserver()){
            onError(error)
        }

        val throwable = runBlocking {
            result.runCatching { await() }.exceptionOrNull()
        }

        assertNotNull(throwable)
        assertEquals(errorMessage,throwable.message)
        assert(throwable is IllegalArgumentException)

        verify(exactly = 0) { result.complete(any()) }
        verify(exactly = 1) { result.completeExceptionally(any()) }
    }

    @Test
    fun `Deferred result doesn't change on repeated completion`(){
        val value = "test_value"
        val result = spyk(CompletableDeferred<String>())

        with(result.toStreamObserver()){
            onNext(value)
            onNext("extra_value")
            onCompleted()
        }

        runBlocking {
            assertEquals(value,result.await())
        }
    }

    @Test
    fun `Deferred result doesn't change on excessive exceptional completion`(){
        val value = "test_value"
        val result = CompletableDeferred<String>()
        val errorMessage = "error_message"
        val error = IllegalArgumentException(errorMessage)

        with(result.toStreamObserver()){
            onNext(value)
            onCompleted()
            onError(error)
        }

        runBlocking {
            assertEquals(value,result.await())
        }
    }

    @Test
    fun `Deferred exception doesn't change on excessive exceptional completion`(){
        val errorMessage = "error_message"
        val error = IllegalArgumentException(errorMessage)
        val result = CompletableDeferred<String>()


        with(result.toStreamObserver()){
            onError(error)
            onError(IndexOutOfBoundsException())
        }

        val throwable = runBlocking {
            result.runCatching { await() }.exceptionOrNull()
        }

        assertNotNull(throwable)
        assertEquals(errorMessage,throwable.message)
        assert(throwable is IllegalArgumentException)
    }
}