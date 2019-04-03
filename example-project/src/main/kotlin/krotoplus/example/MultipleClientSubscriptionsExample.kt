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

import com.google.protobuf.Empty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.toChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import test.Tick
import test.TickerCoroutineGrpc


class TickerExampleService : TickerCoroutineGrpc.TickerImplBase(){

    // Ticker is a producer that broadcast messages to all subscribed
    // clients every 10s.
    private val ticker = GlobalScope.broadcast<Tick> {
        while (isActive){
            send { timestamp = System.currentTimeMillis() }
            delay(10_000)
        }
    }

    override suspend fun listen(request: Empty, responseChannel: SendChannel<Tick>) {
        val subscription = ticker.openSubscription()

        // Forward incoming messages from the broadcast subscription to the client
        subscription.toChannel(responseChannel)
    }
}