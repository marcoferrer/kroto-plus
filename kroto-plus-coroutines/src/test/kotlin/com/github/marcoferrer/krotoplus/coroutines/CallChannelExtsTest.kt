package com.github.marcoferrer.krotoplus.coroutines

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.mockk.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class NewSendChannelFromObserverTests {

    @Test
    fun `Test channel send to observer success`() = runBlocking {

        val observer = mockk<StreamObserver<Int>>().apply {
            every { onNext(allAny()) } just Runs
            every { onCompleted() } just Runs
        }

        GlobalScope.newSendChannelFromObserver(observer).apply {
            repeat(3){ send(it) }
            close()
        }

        verify(exactly = 3) { observer.onNext(allAny()) }
    }

    @Test
    fun `Test channel close with error`() = runBlocking {

        val statusException = Status.INVALID_ARGUMENT.asException()
        val observer = mockk<StreamObserver<String>>().apply {
            every { onNext(allAny()) } just Runs
            every { onError(statusException) } just Runs
        }

        GlobalScope.newSendChannelFromObserver(observer).apply {
            send("")
            close(statusException)
        }

        verify(exactly = 1) { observer.onNext(allAny()) }
        verify(exactly = 1) { observer.onError(statusException) }
    }

    @Test
    fun `Test channel close when observer onNext error `() = runBlocking {

        val statusException = Status.UNKNOWN.asException()
        val observer = mockk<StreamObserver<String>>().apply {
            every { onNext(allAny()) } throws statusException
            every { onError(statusException) } just Runs
        }

        GlobalScope.newSendChannelFromObserver(observer).apply {

            val send1Result = runCatching { send("") }
            assertTrue(send1Result.isSuccess, "Error during observer.onNext should not fail channel.send")
            assertTrue(isClosedForSend,"Channel should be closed after onNext error")

            val send2Result = runCatching { send("") }
            assertTrue(send2Result.isFailure, "Expecting error after sending a value to failed channel")
            assertEquals(statusException,send2Result.exceptionOrNull())
        }

        verify(exactly = 1) { observer.onNext(allAny()) }
        verify(exactly = 1) { observer.onError(statusException) }
        verify(inverse = true) { observer.onCompleted() }
    }
}