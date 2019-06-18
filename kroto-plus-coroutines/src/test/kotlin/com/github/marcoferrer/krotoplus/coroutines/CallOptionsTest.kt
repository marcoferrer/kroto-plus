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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CallOptionsTest {

    @Test
    fun `Coroutine context call option defaults to EmptyCoroutineContext`(){
        assertEquals(EmptyCoroutineContext, CallOptions.DEFAULT.getOption(CALL_OPTION_COROUTINE_CONTEXT))
    }

    @Test
    fun `Stub coroutineContext is populated via call option value`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)
        assertEquals(EmptyCoroutineContext, stub.context)
    }

    @Test
    fun `Attaching coroutineContext to stub explicitly`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)
        assertEquals(
            Dispatchers.Default,
            stub.withCoroutineContext(Dispatchers.Default).coroutineContext
        )

        assertEquals(EmptyCoroutineContext, stub.context)
        assertEquals(
            Dispatchers.Default,
            stub.withCoroutineContext(Dispatchers.Default).context
        )
    }

    @Test
    fun `Attaching coroutineContext to stub implicitly`(){
        val channel = mockk<Channel>()
        val stub = TestStub(channel)
        assertEquals(EmptyCoroutineContext, stub.coroutineContext)
        assertEquals(EmptyCoroutineContext, stub.context)

        runBlocking {
            val newStub = stub.withCoroutineContext()

            assertEquals(coroutineContext, newStub.coroutineContext)
            assertEquals(coroutineContext, newStub.context)

            assertNotEquals(stub.coroutineContext, newStub.coroutineContext)
            assertNotEquals(stub.context, newStub.context)
        }
    }

    @Test
    fun `Merging scope context with stub context implicitly`(){
        val channel = mockk<Channel>()

        val coroutineName = CoroutineName("testing")
        val stub = TestStub(channel)
            .withCoroutineContext(coroutineName)

        runBlocking(Dispatchers.IO) {
            val newStub = stub.plusCoroutineContext()

            assertEquals(coroutineName.name,newStub.context[CoroutineName]?.name)
            assertEquals(Dispatchers.IO,newStub.context[ContinuationInterceptor])
            assertNotEquals(stub.context, newStub.context)
        }
    }

    @Test
    fun `Merging context with stub context explicitly`(){
        val channel = mockk<Channel>()

        val coroutineName = CoroutineName("testing")
        val stub = TestStub(channel)
            .withCoroutineContext(coroutineName)

        val newStub = stub.plusCoroutineContext(Dispatchers.IO)

        assertEquals(coroutineName.name,newStub.context[CoroutineName]?.name)
        assertEquals(Dispatchers.IO,newStub.context[ContinuationInterceptor])
        assertNotEquals(stub.context, newStub.context)
    }

    @Test
    fun `Attaching coroutineContext to call options explicitly`(){
        val callOptions = CallOptions.DEFAULT.withCoroutineContext(Dispatchers.Default)
        assertEquals(
            Dispatchers.Default,
            callOptions.getOption(CALL_OPTION_COROUTINE_CONTEXT)
        )
    }

    @Test
    fun `Attaching coroutineContext to call options implicitly`() = runBlocking {
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