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

import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import io.mockk.*
import kotlinx.coroutines.*
import org.junit.Test
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*


class NewSendChannelFromObserverTests {

    @Test
    fun `Test channel send to observer success`() = runBlocking {

        val observer = mockk<StreamObserver<Int>>().apply {
            every { onNext(allAny()) } just Runs
            every { onCompleted() } just Runs
        }

        GlobalScope.newSendChannelFromObserver(observer).apply {
            repeat(3) { send(it) }
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
            assertTrue(isClosedForSend, "Channel should be closed after onNext error")

            val send2Result = runCatching { send("") }
            assertTrue(send2Result.isFailure, "Expecting error after sending a value to failed channel")
            assertEquals(statusException, send2Result.exceptionOrNull())
        }

        verify(exactly = 1) { observer.onNext(allAny()) }
        verify(exactly = 1) { observer.onError(statusException) }
        verify(inverse = true) { observer.onCompleted() }
    }
}

class NewManagedServerResponseChannelTests {

    lateinit var observer: ServerCallStreamObserver<Unit>

    @BeforeTest
    fun setup(){
        observer = mockk<ServerCallStreamObserver<Unit>>().apply {
            every { disableAutoInboundFlowControl() } just Runs
            every { setOnReadyHandler(any()) } just Runs
        }
    }

    @Test
    fun `Test manual flow control is enabled`()= runBlocking {
        newManagedServerResponseChannel<Unit,Unit>(observer,AtomicBoolean())
        verify(exactly = 1) { observer.disableAutoInboundFlowControl() }
        verify(exactly = 1) { observer.setOnReadyHandler(any()) }
    }

    @Test
    fun `Test channel propagates values to observer onNext`() = runBlocking {
        observer.apply {
            every { onNext(Unit) } just Runs
            every { onCompleted() } just Runs
        }

        with(newManagedServerResponseChannel<Unit,Unit>(observer,AtomicBoolean())){
            repeat(3){
                send(Unit)
            }
            close()
        }

        verify(exactly = 3) { observer.onNext(Unit) }
        verify(exactly = 1) { observer.onCompleted() }
    }

    @Test
    fun `Test channel propagates errors to observer onError`(){

        observer.apply {

            every {
                val matcher = match<StatusRuntimeException> {
                    it.status.code == Status.UNKNOWN.code
                }
                onError(matcher)
            } just Runs

            every { onNext(Unit) } just Runs
            every { onCompleted() } just Runs
        }

        val error = IllegalArgumentException("error")
        assertFailsWith(IllegalArgumentException::class, error.message){
            runBlocking {
                newManagedServerResponseChannel<Unit, Unit>(observer, AtomicBoolean()).apply {
                    send(Unit)
                    close(error)
                }
            }
        }

        verify(exactly = 1) { observer.onNext(Unit) }
        verify(exactly = 1) { observer.onError(any()) }
        verify(exactly = 0) { observer.onCompleted() }
    }

}

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

class HandleUnaryRpcBlockTests {

    @Test
    fun `Test block completed successfully`(){
        val observer = mockk<StreamObserver<Unit>>().apply {
            every { onNext(Unit) } just Runs
            every { onCompleted() } just Runs
        }

        observer.handleUnaryRpc { Unit }

        verify(exactly = 1) { observer.onNext(Unit) }
        verify(exactly = 1) { observer.onCompleted() }
    }


    @Test
    fun `Test block completed exceptionally`(){
        val observer = mockk<StreamObserver<Unit>>().apply {
            every {
                val matcher = match<StatusRuntimeException> {
                    it.status.code == Status.UNKNOWN.code
                }
                onError(matcher)
            } just Runs
        }

        observer.handleUnaryRpc { error("failed") }

        verify(exactly = 1) { observer.onError(any()) }
    }
}

class BindToClientCancellationTests {

    @Test
    fun `Observer invoking cancellation handler cancels coroutine scope`() {

        val cancellationHandler = slot<Runnable>()
        val serverCallStreamObserver = mockk<ServerCallStreamObserver<*>>().apply {
            every { setOnCancelHandler(capture(cancellationHandler)) } just Runs
        }

        try {

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
            fail("Job did not fail")
        } catch (e: Throwable) {
            when (e) {
                is AssertionError -> throw e
                else -> assertEquals(
                    "kotlinx.coroutines.JobCancellationException",
                    e.javaClass.name
                )
            }
        }
    }
}