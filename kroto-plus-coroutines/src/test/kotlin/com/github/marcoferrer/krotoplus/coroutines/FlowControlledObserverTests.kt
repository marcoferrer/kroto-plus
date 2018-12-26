package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.CallStreamObserver
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean


class NextValueWithBackPressureTests {

    private val mockObserver = object: FlowControlledObserver{}

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


