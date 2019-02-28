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

package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.CallOptions
import io.grpc.Channel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CallOptionsTest {

    @Test
    fun `Test coroutine context call option defaults to EmptyCoroutineContext`(){
        assertEquals(EmptyCoroutineContext, CallOptions.DEFAULT.getOption(CALL_OPTION_COROUTINE_CONTEXT))
    }

    @Test
    fun `Test stub coroutineContext is populated via call option value`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)
    }

    @Test
    fun `Test attaching coroutineContext to stub explicitly`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)
        assertEquals(
            Dispatchers.Default,
            stub.withCoroutineContext(Dispatchers.Default).coroutineContext
        )
    }

    @Test
    fun `Test attaching coroutineContext to stub implicitly`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)

        runBlocking {
            val newStub = stub.withCoroutineContext()
            assertEquals(coroutineContext, newStub.coroutineContext)
            assertNotEquals(stub.coroutineContext, newStub.coroutineContext)
        }
    }

    @Test
    fun `Test attaching coroutineContext to call options explicitly`(){
        val callOptions = CallOptions.DEFAULT.withCoroutineContext(Dispatchers.Default)
        assertEquals(
            Dispatchers.Default,
            callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)
        )
    }

    @Test
    fun `Test attaching coroutineContext to call options implicitly`() = runBlocking {
        val callOptions = CallOptions.DEFAULT.withCoroutineContext()
        assertEquals(
            coroutineContext,
            callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)
        )
        assertNotEquals(
            CallOptions.DEFAULT.getOption(CALL_OPTION_COROUTINE_CONTEXT),
            callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)
        )
    }
}