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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * A Channel representing a Bi-Directional rpc client call. Elements sent to this channel
 * with be sent to the server as request messages. Coroutine suspension is used as a mechanism to manage manual
 * flow control and apply back pressure to the production of request messages and consumption of response messages.
 *
 * The server will continue to request messages from this channel until its consumption becomes suspended. This will
 * cause invocations of `send` at the client to suspend until the server signals that it is ready to consume more
 * messages, or the call becomes cancelled.
 *
 * The client will consume responses from the server via this channel. For every message consumed, the client
 * will signal to the server that it is ready to receive another message. This channel will buffer at most one message
 * before signaling to the server that it is not ready to receive any additional messages. This will cause the
 * server implementation to suspend on attempts to send.
 *
 * Example:
 * ```
 * // We attach the current coroutine context so that cancellations
 * // can clean up resources and notify the server.
 * val (requestChannel, responseChannel) = stub.withCoroutineContext().sayHelloStreaming()
 *
 * launch {
 *     repeat(5){
 *         requestChannel.send { name = "person #$it" }
 *     }
 *     requestChannel.close()
 * }
 *
 * responseChannel.consumeEach {
 *     println("Bidi Response: $it")
 * }
 *
 * ```
 *
 * This interface implements operators for `component1()` and `component2()` as a convenience for splitting request
 * and response handling into separate channels.
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


internal class ClientResponseObserverChannel<ReqT, RespT>(
    override val coroutineContext: CoroutineContext,
    private val responseChannelDelegate: Channel<RespT> = Channel(capacity = 1)
) : ClientResponseObserver<ReqT, RespT>,
    FlowControlledObserver,
    ReceiveChannel<RespT> by responseChannelDelegate,
    CoroutineScope {

    private val isMessagePreloaded = AtomicBoolean()

    private lateinit var requestStream: ClientCallStreamObserver<ReqT>

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        this.requestStream = requestStream.apply {
            enableManualFlowControl(responseChannelDelegate,isMessagePreloaded)
        }
    }

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