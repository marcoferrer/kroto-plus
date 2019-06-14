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

package com.github.marcoferrer.krotoplus.generators

import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.HelloWorldProtoDslBuilder
import io.grpc.examples.helloworld.orDefault
import org.junit.Test
import test.message.*
import test.message.multi.MappedMessage
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ProtoBuildersGeneratorTests {

    @Test
    fun `Test DSL marker insertion`(){
        assert(HelloRequest.newBuilder() is HelloWorldProtoDslBuilder)
        assert(HelloReply.newBuilder() is HelloWorldProtoDslBuilder)
        assert(
            TestMessages.L1Message2.L2Nested2.L3Nested2.L4Nested1.L5Nested1
            .newBuilder() is TestMessagesDslBuilder
        )
    }

    @Test
    fun `Test nested message builder generation`(){
        val value = "value"
        assertEquals(value, L1Message2 { field = value }.field)
        assertEquals(value, L1Message2.L2Nested2.L3Nested2.L4Nested1.L5Nested1 { field = value }.field)
    }


    @Test
    fun `Test nested field builder generation`(){
        val value = "value"
        val message = L1Message2.L2Nested2{
            field = value
            nestedMessage {
                field = value
                anotherNestedMessage {
                    field = value
                }
            }
        }
        assertEquals(value, message.field)
        assertEquals(value, message.nestedMessage.field)
        assertEquals(value, message.nestedMessage.anotherNestedMessage.field)
    }

    @Test
    fun `Test message orDefault ext generation`(){
        val message: HelloReply? = null
        assertEquals(HelloReply.getDefaultInstance(), message.orDefault())
    }

    @Test
    fun `Test message copy ext generation`(){

        val message = L1Message2.L2Nested2{
            field = "field1"
            nestedMessage {
                field = "field2"
            }
        }
        val copyValue = message.copy { field = "replaced_field" }

        assertEquals("replaced_field", copyValue.field)
        assertNotEquals(message.field, copyValue.field)
        assertEquals(message.nestedMessage.field, copyValue.nestedMessage.field)
    }

    @Test
    fun `Test message plus operator ext generation`(){

        val message1 = L1Message2.L2Nested2{
            field = "field1"
        }

        val message2 = L1Message2.L2Nested2{
            nestedMessage {
                field = "field2"
            }
        }
        val result = message1 + message2

        assertEquals(message1.field, result.field)
        assertNotEquals(message1.nestedMessage, result.nestedMessage)
        assertEquals(message2.nestedMessage.field, result.nestedMessage.field)
    }

    @Test
    fun `Test message map omission`() {
        val key = "test"
        val value = "value"

        val message = MappedMessage {
            putTheField(key, value)
        }

        assertEquals(message.theFieldMap[key], value)
    }
}