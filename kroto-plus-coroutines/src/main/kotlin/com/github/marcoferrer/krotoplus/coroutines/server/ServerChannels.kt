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

import com.github.marcoferrer.krotoplus.coroutines.call.FlowControlledInboundStreamObserver
import io.grpc.stub.ServerCallStreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

internal class ServerRequestStreamChannel<ReqT>(
    override val coroutineContext: CoroutineContext,
    override val inboundChannel: Channel<ReqT> = Channel(capacity = 0),
    override val transientInboundMessageCount: AtomicInteger = AtomicInteger(),
    override val callStreamObserver: ServerCallStreamObserver<*>,
    private val onErrorHandler: ((Throwable) -> Unit)? = null
) : ReceiveChannel<ReqT> by inboundChannel,
    FlowControlledInboundStreamObserver<ReqT>,
    CoroutineScope {

    override val isInboundCompleted: AtomicBoolean = AtomicBoolean()

    override fun onNext(value: ReqT) = onNextWithBackPressure(value)

    override fun onError(t: Throwable) {
        inboundChannel.close(t)
        onErrorHandler?.invoke(t)
    }
}