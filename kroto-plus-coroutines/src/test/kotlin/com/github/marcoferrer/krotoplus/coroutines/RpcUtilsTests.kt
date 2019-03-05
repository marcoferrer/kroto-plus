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

import com.github.marcoferrer.krotoplus.coroutines.utils.assertCancellationError
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.take
import kotlinx.coroutines.channels.toList
import org.junit.Test
import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LaunchProducerJobTests {

    @Test
    fun `Job produces values to channel and channel closes with no error`() = runBlocking{

        val expectedSize = 5
        val channel = spyk(Channel<Unit>())
        val job = launchProducerJob(channel){
            repeat(expectedSize) {
                send(Unit)
            }
        }

        val result = async { channel.toList() }
        assertEquals(expectedSize,result.await().size)
        assert(channel.isClosedForSend){ "Channel is closed for send" }
        assert(job.isCompleted){ "Producer Job is completed" }
        verify(atLeast = 1) { channel.close(null) }
    }

    @Test
    fun `Channel is closed on normal scope cancellation`(){
        val channel = spyk(Channel<Unit>())
        assertCancellationError {
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    launchProducerJob(channel) {
                        repeat(5) {
                            send(Unit)
                        }
                    }
                }
                cancel()
            }
        }

        assert(channel.isClosedForSend){ "Channel should be closed for send" }
        verify(atLeast = 1) { channel.close(any<CancellationException>()) }
    }

    @Test
    fun `Channel is closed on exceptional scope cancellation`(){

        val channel = spyk(Channel<Unit>())
        assertFailsWith(IllegalStateException::class,"cancel"){
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    launchProducerJob(channel) {
                        repeat(5) {
                            send(Unit)
                        }
                    }
                }
                launch{
                    error("cancel")
                }
                cancel()
            }
        }

        assert(channel.isClosedForSend){ "Channel should be closed for send" }
        verify(atLeast = 1) { channel.close(any<CancellationException>()) }
    }
}