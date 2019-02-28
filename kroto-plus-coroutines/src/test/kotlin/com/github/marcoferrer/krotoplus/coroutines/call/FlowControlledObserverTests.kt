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

import io.grpc.stub.CallStreamObserver
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean


class NextValueWithBackPressureTests {

    private val mockObserver = object: FlowControlledObserver {}

    @Test
    fun `Test channel is closed`() {

        val observer = mockk<CallStreamObserver<String>>()
        val isMessagePreloaded = mockk<AtomicBoolean>()
        val mockScope = mockk<CoroutineScope>()
        val channel = mockk<Channel<Int>>().apply {
            every { isClosedForSend } returns true
        }

        with(mockObserver){
            mockScope.nextValueWithBackPressure(1,channel, observer, isMessagePreloaded)
        }

        verify { isMessagePreloaded wasNot Called }
        verify(atLeast = 1) { channel.isClosedForSend }
        verify(inverse = true) { channel.offer(any()) }
        coVerify(inverse = true) { channel.send(any()) }
    }

    @Test
    fun `Test channel accepts value`() {

        val observer = mockk<CallStreamObserver<String>>().apply {
            every { request(1) } just Runs
        }
        val mockScope = mockk<CoroutineScope>()
        val isMessagePreloaded = mockk<AtomicBoolean>()
        val channel = mockk<Channel<Int>>().apply {
            every { isClosedForSend } returns false
            every { offer(1) } returns true
        }

        with(mockObserver){
            mockScope.nextValueWithBackPressure(1,channel, observer, isMessagePreloaded)
        }

        verify { isMessagePreloaded wasNot Called }
        verify(atLeast = 1) { channel.isClosedForSend }
        verify(exactly = 1) { channel.offer(any()) }
        verify(exactly = 1) { observer.request(1) }
        coVerify(inverse = true) { channel.send(any()) }
    }

    @Test
    fun `Test channel buffer is full and preload value`() {

        val observer = mockk<CallStreamObserver<String>>().apply {
            every { request(1) } just Runs
        }
        val isMessagePreloaded = mockk<AtomicBoolean>().apply {
            every { set(allAny()) } just Runs
        }
        val channel = spyk(Channel<Int>(capacity = 1))

        runBlocking {
            channel.offer(0)
            assert(channel.isFull){ "Target channel will not cause preload" }
            with(mockObserver) {
                nextValueWithBackPressure(1, channel, observer, isMessagePreloaded)
            }
            channel.receive()
        }

        verifyOrder {
            isMessagePreloaded.set(true)
            isMessagePreloaded.set(false)
        }
        verify { channel.offer(any()) }
        verify(atLeast = 1) { channel.isClosedForSend }
        verify(exactly = 1) { observer.request(1) }
        coVerify(exactly = 1) { channel.send(any()) }
    }
}


