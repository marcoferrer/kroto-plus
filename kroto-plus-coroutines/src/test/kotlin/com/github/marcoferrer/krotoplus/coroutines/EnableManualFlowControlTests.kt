package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.stub.CallStreamObserver
import io.mockk.*
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean


class EnableManualFlowControlTests {

    @Test
    fun `Test observer not ready`(){

        val onReadyHandler = slot<Runnable>()
        val targetChannel = mockk<Channel<Unit>>()
        val observer = mockk<CallStreamObserver<Unit>>().apply {
            every { isReady } returns false
            every { setOnReadyHandler(capture(onReadyHandler)) } just Runs
            every { disableAutoInboundFlowControl() } just Runs
        }

        observer.enableManualFlowControl(targetChannel, AtomicBoolean())

        onReadyHandler.captured.run()

        verify(exactly = 1) { observer.isReady }
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
        verify(inverse = true) { observer.request(any()) }
    }

    @Test
    fun `Test channel is full`(){

        val onReadyHandler = slot<Runnable>()
        val targetChannel = mockk<Channel<Unit>>().apply {
            every { isFull } returns true
        }
        val observer = mockk<CallStreamObserver<Unit>>().apply {
            every { isReady } returns true
            every { setOnReadyHandler(capture(onReadyHandler)) } just Runs
            every { disableAutoInboundFlowControl() } just Runs
        }

        observer.enableManualFlowControl(targetChannel, AtomicBoolean())

        onReadyHandler.captured.run()

        verify(exactly = 1) { observer.isReady }
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
        verify(inverse = true) { observer.request(any()) }

        verify(exactly = 1) { targetChannel.isFull }
        verify(inverse = true) { targetChannel.isClosedForSend }
    }


    @Test
    fun `Test channel is closed for send`(){

        val onReadyHandler = slot<Runnable>()
        val isMessagePreloaded = mockk<AtomicBoolean>()
        val targetChannel = mockk<Channel<Unit>>().apply {
            every { isFull } returns false
            every { isClosedForSend } returns true
        }
        val observer = mockk<CallStreamObserver<Unit>>().apply {
            every { isReady } returns true
            every { setOnReadyHandler(capture(onReadyHandler)) } just Runs
            every { disableAutoInboundFlowControl() } just Runs
        }

        observer.enableManualFlowControl(targetChannel,isMessagePreloaded)

        onReadyHandler.captured.run()

        verify(exactly = 1) { observer.isReady }
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
        verify(inverse = true) { observer.request(any()) }

        verify(exactly = 1) { targetChannel.isFull }
        verify(exactly = 1) { targetChannel.isClosedForSend }

        verify(inverse = true) { isMessagePreloaded.compareAndSet(any(),any()) }
    }


    @Test
    fun `Test message is preloaded for target channel`(){

        val onReadyHandler = slot<Runnable>()
        val isMessagePreloaded = mockk<AtomicBoolean>().apply {
            every { compareAndSet(false,true) } returns false
        }
        val targetChannel = mockk<Channel<Unit>>().apply {
            every { isFull } returns false
            every { isClosedForSend } returns false
        }
        val observer = mockk<CallStreamObserver<Unit>>().apply {
            every { isReady } returns true
            every { setOnReadyHandler(capture(onReadyHandler)) } just Runs
            every { disableAutoInboundFlowControl() } just Runs
        }

        observer.enableManualFlowControl(targetChannel,isMessagePreloaded)

        onReadyHandler.captured.run()

        verify(exactly = 1) { observer.isReady }
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
        verify(inverse = true) { observer.request(any()) }

        verify(exactly = 1) { targetChannel.isFull }
        verify(exactly = 1) { targetChannel.isClosedForSend }

        verify(exactly = 1) { isMessagePreloaded.compareAndSet(false,true) }
    }


    @Test
    fun `Test ready to request new message from observer`(){

        val onReadyHandler = slot<Runnable>()
        val isMessagePreloaded = mockk<AtomicBoolean>().apply {
            every { compareAndSet(false,true) } returns true
        }
        val targetChannel = mockk<Channel<Unit>>().apply {
            every { isFull } returns false
            every { isClosedForSend } returns false
        }
        val observer = mockk<CallStreamObserver<Unit>>().apply {
            every { isReady } returns true
            every { setOnReadyHandler(capture(onReadyHandler)) } just Runs
            every { disableAutoInboundFlowControl() } just Runs
            every { request(1) } just Runs
        }

        observer.enableManualFlowControl(targetChannel,isMessagePreloaded)

        onReadyHandler.captured.run()

        verify(exactly = 1) { observer.isReady }
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
        verify(exactly = 1) { observer.request(1) }

        verify(exactly = 1) { targetChannel.isFull }
        verify(exactly = 1) { targetChannel.isClosedForSend }

        verify(exactly = 1) { isMessagePreloaded.compareAndSet(false,true) }
    }
}