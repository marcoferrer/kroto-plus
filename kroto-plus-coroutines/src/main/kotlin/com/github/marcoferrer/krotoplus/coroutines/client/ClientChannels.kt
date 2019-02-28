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

import com.github.marcoferrer.krotoplus.coroutines.call.FlowControlledObserver
import com.github.marcoferrer.krotoplus.coroutines.call.enableManualFlowControl
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 *
 */
public interface ClientBidiCallChannel<ReqT, RespT> : SendChannel<ReqT>, ReceiveChannel<RespT>{

    public val requestChannel: SendChannel<ReqT>

    public val responseChannel: ReceiveChannel<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): ReceiveChannel<RespT> = responseChannel
}

internal class ClientBidiCallChannelImpl<ReqT, RespT>(
    public override val requestChannel: SendChannel<ReqT>,
    public override val responseChannel: ReceiveChannel<RespT>
) : ClientBidiCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by requestChannel,
    ReceiveChannel<RespT> by responseChannel

/**
 *
 */
public interface ClientStreamingCallChannel<ReqT, RespT> : SendChannel<ReqT> {

    public val requestChannel: SendChannel<ReqT>

    public val response: Deferred<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): Deferred<RespT> = response
}

internal class ClientStreamingCallChannelImpl<ReqT, RespT>(
    public override val requestChannel: SendChannel<ReqT>,
    public override val response: Deferred<RespT>
) : ClientStreamingCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by requestChannel


/**
 *
 */
internal class ClientResponseObserverChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    private val responseChannelDelegate: Channel<RespT> = Channel(capacity = 1)
) : ClientResponseObserver<ReqT, RespT>,
    FlowControlledObserver,
    ReceiveChannel<RespT> by responseChannelDelegate,
    CoroutineScope {

    private val isMessagePreloaded = AtomicBoolean()

    private lateinit var requestStream: ClientCallStreamObserver<ReqT>

    @ExperimentalCoroutinesApi
    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        this.requestStream = requestStream.apply {
            enableManualFlowControl(responseChannelDelegate,isMessagePreloaded)
        }
    }

    @ExperimentalCoroutinesApi
    override fun onNext(value: RespT) = nextValueWithBackPressure(
        value = value,
        channel = responseChannelDelegate,
        callStreamObserver = requestStream,
        isMessagePreloaded = isMessagePreloaded
    )

    override fun onError(t: Throwable) {
        responseChannelDelegate.close(t)
    }

    override fun onCompleted() {
        responseChannelDelegate.close()
    }
}