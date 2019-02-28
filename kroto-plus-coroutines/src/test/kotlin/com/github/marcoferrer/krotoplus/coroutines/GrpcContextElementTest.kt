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

import io.grpc.Context
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class GrpcContextElementTest {

    data class Person(val name: String)

    @Test
    fun `Test Grpc Context Element Attach`() = runBlocking<Unit> {

        val KEY_PERSON = Context.key<Person>("person")
        val bill = Person("Bill")
        val ctx = Context.current().withValue(KEY_PERSON, bill)
        ctx.attach()

        GlobalScope.launch {
            // Yield so that we can make sure we suspend at least once
            yield()
            assertNull(KEY_PERSON.get())
        }

        val grpcContextElement = ctx.asContextElement()

        launch(grpcContextElement) {
            delay(1L)

            val expectedGrpcContext =
                (coroutineContext[GrpcContextElement] as GrpcContextElement)
                    .context

            assertEquals(bill, KEY_PERSON.get())
            assertEquals(bill, KEY_PERSON.get(expectedGrpcContext))

            GlobalScope.launch {
                assertNotEquals(expectedGrpcContext, Context.current())
            }

            launch {
                assertEquals(expectedGrpcContext, Context.current())
            }
        }
    }
}