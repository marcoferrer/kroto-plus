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

import com.github.marcoferrer.krotoplus.coroutines.utils.assertFails
import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.matchStatus
import io.grpc.ClientCall
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.junit.Test
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


class MethodDescriptorExtTests {

    @Test
    fun `Test CoroutineName from MethodDescriptor`(){
        val methodName = "test_method_name"
        val descriptor = mockk<MethodDescriptor<Unit,Unit>>().apply {
            every { fullMethodName } returns methodName
        }
        val coroutineName = descriptor.getCoroutineName()
        assertEquals(methodName, coroutineName.name)
    }
}

class BindToClientCancellationTests {

    @Test
    fun `Observer invoking cancellation handler cancels coroutine scope`() {

        val cancellationHandler = slot<Runnable>()
        val serverCallStreamObserver = mockk<ServerCallStreamObserver<*>>().apply {
            every { setOnCancelHandler(capture(cancellationHandler)) } just Runs
        }

        assertFails<CancellationException> {
            runBlocking {
                bindToClientCancellation(serverCallStreamObserver)
                launch {
                    cancellationHandler.captured.run()
                    launch {
                        fail("Child job was executed, Scope has not been cancelled")
                    }
                    yield()
                    fail("Job continued after suspension, Scope has not been cancelled")
                }
            }
        }
    }
}

class BindScopeCancellationToCallTests {

    @Test
    fun `Test call is cancelled by unhandled exception in scope`(){

        val errorMessage = "scope_cancelled"
        val call = mockk<ClientCall<*,*>>().apply {
            every { cancel(any(), any()) } just Runs
        }

        assertFails<IllegalStateException>(errorMessage) {
            runBlocking {
                launch {
                    bindScopeCancellationToCall(call)
                    launch { error(errorMessage) }
                    yield()
                    launch { fail("Child job was executed, Scope has not been cancelled") }
                    yield()
                    fail("Job continued after suspension, Scope has not been cancelled")
                }

            }
        }

        verify(exactly = 1) { call.cancel(errorMessage,any<IllegalStateException>()) }
    }

    @Test
    fun `Test call is cancelled by normal scope cancellation`(){

        val call = mockk<ClientCall<*,*>>().apply {
            every { cancel(any(), any()) } just Runs
        }

        runBlocking {
            launch {
                bindScopeCancellationToCall(call)
                launch { Unit }
                yield()
                cancel()
                launch { fail("Child job was executed, Scope has not been cancelled") }
                yield()
                fail("Job continued after suspension, Scope has not been cancelled")
            }
        }

        verify(exactly = 1) { call.cancel("Job was cancelled",any<CancellationException>()) }
    }

    @Test
    fun `Test call is not cancelled by normal completion`(){

        val call = mockk<ClientCall<*,*>>()

        runBlocking {
            launch {
                bindScopeCancellationToCall(call)
                launch { Unit }
            }
        }

        verify(exactly = 0) { call.cancel(any(),any()) }
    }

    @Test
    fun `Test binding fails if scope has no job`(){

        val call = mockk<ClientCall<*,*>>()

        assertFails<IllegalStateException>{
            GlobalScope.bindScopeCancellationToCall(call)
        }
    }
}