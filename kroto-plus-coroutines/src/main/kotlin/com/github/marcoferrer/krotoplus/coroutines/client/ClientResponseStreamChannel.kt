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

package com.github.marcoferrer.krotoplus.coroutines.client

import com.github.marcoferrer.krotoplus.coroutines.call.FlowControlledInboundStreamObserver
import com.github.marcoferrer.krotoplus.coroutines.call.applyInboundFlowControl
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


internal class ClientResponseStreamChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    override val inboundChannel: Channel<RespT> = Channel()
) : ClientResponseObserver<ReqT, RespT>,
    FlowControlledInboundStreamObserver<RespT>,
    ReceiveChannel<RespT> by inboundChannel,
    CoroutineScope {

    override val isInboundCompleted: AtomicBoolean = AtomicBoolean()

    override val transientInboundMessageCount: AtomicInteger = AtomicInteger()

    override lateinit var callStreamObserver: ClientCallStreamObserver<ReqT>

    private var aborted: Boolean = false

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        callStreamObserver = requestStream.apply {
            applyInboundFlowControl(inboundChannel,transientInboundMessageCount)
        }

        inboundChannel.invokeOnClose {
            if(!isInboundCompleted.get() && !aborted){
                callStreamObserver.cancel("Call has been cancelled", it)
            }
        }
    }

    override fun onNext(value: RespT): Unit = onNextWithBackPressure(value)

    override fun onError(t: Throwable) {
        aborted = true
        inboundChannel.close(t)
    }
}