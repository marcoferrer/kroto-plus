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

package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.utils.assertFailsWithStatus
import com.github.marcoferrer.krotoplus.coroutines.utils.matchStatus
import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.Test


class ServerCallTests {

    @Test
    fun `Test server handler unimplemented unary`(){
        val methodDescriptor = GreeterGrpc.getSayHelloMethod()
        assertFailsWithStatus(
            Status.UNIMPLEMENTED,
            "UNIMPLEMENTED: Method ${methodDescriptor.fullMethodName} is unimplemented"
        ){
            serverCallUnimplementedUnary(methodDescriptor)
        }
    }

    @Test
    fun `Test server handler unimplemented streaming`(){
        val methodDescriptor = GreeterGrpc.getSayHelloMethod()
        val channel = spyk(Channel<String>())
        serverCallUnimplementedStream(methodDescriptor,channel)

        verify(exactly = 1) {
            channel.close(matchStatus(
                Status.UNIMPLEMENTED,
                "UNIMPLEMENTED: Method ${methodDescriptor.fullMethodName} is unimplemented"
            ))
        }
    }
}