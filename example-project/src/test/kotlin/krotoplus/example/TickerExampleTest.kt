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

package krotoplus.example

import com.github.marcoferrer.krotoplus.coroutines.newGrpcStub
import io.grpc.testing.GrpcServerRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import test.TickerCoroutineGrpc
import kotlin.test.BeforeTest

class TickerExampleTest {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun bindService() {
        grpcServerRule.serviceRegistry?.addService(TickerExampleService())
    }

    fun `Test multiple client ticker subscriptions`(){
        val job = GlobalScope.launch {
            val tickerService = newGrpcStub(
                TickerCoroutineGrpc.TickerCoroutineStub,
                grpcServerRule.channel
            )

            // Register 5 individual client to subscribe to tick events from the server
            repeat(5){ clientNo ->
                launch {
                    val responseChannel = tickerService.listen()
                    responseChannel.consumeEach {
                        println("Client #$clientNo, Tick: ${it.timestamp}")
                    }
                }
            }
        }

        runBlocking { job.join() }
    }

}