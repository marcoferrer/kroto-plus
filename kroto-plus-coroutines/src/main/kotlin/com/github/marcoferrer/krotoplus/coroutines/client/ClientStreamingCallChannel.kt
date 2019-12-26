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

import com.github.marcoferrer.krotoplus.coroutines.call.MessageHandler
import com.github.marcoferrer.krotoplus.coroutines.call.applyOutboundFlowControl
import com.github.marcoferrer.krotoplus.coroutines.call.attachOutboundChannelCompletionHandler
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext

/**
 *
 */
public interface ClientStreamingCallChannel<ReqT, RespT> : SendChannel<ReqT> {

    public val requestChannel: SendChannel<ReqT>

    public val response: Deferred<RespT>

    public operator fun component1(): SendChannel<ReqT> = requestChannel

    public operator fun component2(): Deferred<RespT> = response
}


internal class ClientStreamingCallChannelImpl<ReqT,RespT>(

    override val coroutineContext: CoroutineContext,

    private val outboundChannel: Channel<ReqT> = Channel(),

    private val completableResponse: CompletableDeferred<RespT> = CompletableDeferred(parent = coroutineContext[Job])

) : ClientResponseObserver<ReqT, RespT>,
    ClientStreamingCallChannel<ReqT, RespT>,
    SendChannel<ReqT> by outboundChannel,
    CoroutineScope {

    override val requestChannel: SendChannel<ReqT>
        get() = outboundChannel

    override val response: Deferred<RespT>
        get() = completableResponse

    private lateinit var callStreamObserver: ClientCallStreamObserver<ReqT>

    private lateinit var outboundMessageHandler: SendChannel<MessageHandler>

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        callStreamObserver = requestStream
        outboundMessageHandler = applyOutboundFlowControl(requestStream, outboundChannel)

        attachOutboundChannelCompletionHandler(
            callStreamObserver, outboundChannel,
            onSuccess = { outboundMessageHandler.close() },
            onError = { error -> completableResponse.completeExceptionally(error) }
        )
        completableResponse.invokeOnCompletion {
            // If the client prematurely cancels the response
            // we need to propagate this as a cancellation to the underlying call
            if(!outboundChannel.isClosedForSend && coroutineContext[Job]?.isCancelled == false){
                callStreamObserver.cancel("Client has cancelled call", it)
            }
        }
    }

    override fun onNext(value: RespT) {
        completableResponse.complete(value)
    }

    override fun onError(t: Throwable) {
        outboundChannel.close(t)
        outboundChannel.cancel(CancellationException(t.message,t))
        completableResponse.completeExceptionally(t)
        outboundMessageHandler.close(t)
    }

    override fun onCompleted() {
        require(completableResponse.isCompleted){
            "Stream was completed before onNext was called"
        }
    }

}