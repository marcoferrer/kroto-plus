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

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.coroutines.*
import kotlin.test.assertFailsWith

class SuspendingUnaryObserverTests {

    @Test
    fun `Observer completion resumes continuation only once`(){

        mockkStatic("kotlin.coroutines.ContinuationKt")
        val expectedResult = "result"

        val cont = mockk<Continuation<String>>().apply {
            every { resume(expectedResult) } just Runs
        }

        SuspendingUnaryObserver(cont).apply {
            onNext(expectedResult)
            onNext("unexpected")
            onCompleted()
        }

        verify(exactly = 1) { cont.resume(any()) }
    }


    @Test
    fun `Observer error resumes continuation exceptionally only once`(){
        // Ran into issues mock continuations and invoking resume with exception
        // Exceptional cases need to be tested with a real continuation

        val expectedError = IllegalStateException("expected")
        lateinit var observer: SuspendingUnaryObserver<String>

        assertFailsWith(expectedError.javaClass.kotlin, expectedError.message){
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    suspendCancellableCoroutine<String> { cont ->
                        observer = spyk(SuspendingUnaryObserver(cont), recordPrivateCalls = true)
                    }
                }
                launch {
                    observer.onError(expectedError)
                    observer.onError(IllegalStateException("unexpected"))
                }
            }
        }
    }

}