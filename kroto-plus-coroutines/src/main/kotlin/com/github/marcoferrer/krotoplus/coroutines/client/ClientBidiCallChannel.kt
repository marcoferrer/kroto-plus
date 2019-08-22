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
import com.github.marcoferrer.krotoplus.coroutines.call.MessageHandler
import com.github.marcoferrer.krotoplus.coroutines.call.applyOutboundFlowControl
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
public interface ClientBidiCallChannel<ReqT, RespT> : SendChannel<ReqT>, ReceiveChannel<RespT> {

    public val requestChannel: SendChannel<ReqT>

    public val responseChannel: ReceiveChannel<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): ReceiveChannel<RespT> = responseChannel
}

internal class ClientBidiCallChannelImpl<ReqT,RespT>(
    override val coroutineContext: CoroutineContext,
    override val inboundChannel: Channel<RespT> = Channel(),
    private val outboundChannel: Channel<ReqT> = Channel()
) : FlowControlledInboundStreamObserver<RespT>,
    ClientResponseObserver<ReqT, RespT>,
    ClientBidiCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by outboundChannel,
    ReceiveChannel<RespT> by inboundChannel
{
    override val requestChannel: SendChannel<ReqT>
        get() = outboundChannel

    override val responseChannel: ReceiveChannel<RespT>
        get() = inboundChannel

    override val isInboundCompleted = AtomicBoolean()

    override val transientInboundMessageCount: AtomicInteger = AtomicInteger()

    override lateinit var callStreamObserver: ClientCallStreamObserver<ReqT>

    private lateinit var outboundMessageHandler: SendChannel<MessageHandler>

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        callStreamObserver = requestStream.apply { disableAutoInboundFlowControl() }
        outboundMessageHandler = applyOutboundFlowControl(requestStream,outboundChannel)

        inboundChannel.invokeOnClose {
            // If the client prematurely closes the response channel
            // we need to propagate this as a cancellation to the underlying call
            if(!outboundChannel.isClosedForSend){
                callStreamObserver.cancel("Call has been cancelled", it)
            }
        }
    }

    override fun onNext(value: RespT): Unit = onNextWithBackPressure(value)

    override fun onError(t: Throwable) {
        outboundChannel.close(t)
        outboundChannel.cancel(CancellationException(t.message,t))
        inboundChannel.close(t)
        outboundMessageHandler.close(t)
    }

    override fun onCompleted() {
        super.onCompleted()
        if (isChannelReadyForClose) {
            outboundMessageHandler.close()
        }
    }
}

