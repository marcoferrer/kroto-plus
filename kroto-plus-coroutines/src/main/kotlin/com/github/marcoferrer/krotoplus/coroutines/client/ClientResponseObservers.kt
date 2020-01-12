/*
 *  Copyright 2019 Kroto+ Contributors
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

import com.github.marcoferrer.krotoplus.coroutines.call.CallReadyObserver
import com.github.marcoferrer.krotoplus.coroutines.call.completeSafely
import com.github.marcoferrer.krotoplus.coroutines.call.newCallReadyObserver
import io.grpc.Status
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

internal abstract class StatefulClientResponseObserver<ReqT, RespT> : ClientResponseObserver<ReqT, RespT> {

    protected val isAborted = AtomicBoolean()

    protected val isCompleted = AtomicBoolean()

    val isActive: Boolean
        get() = !(isAborted.get() || isCompleted.get())

    lateinit var callStreamObserver: ClientCallStreamObserver<ReqT>
        protected set

}

internal class ServerStreamingResponseObserver<ReqT, RespT>: StatefulClientResponseObserver<ReqT, RespT>() {

    lateinit var responseProducerScope: ProducerScope<RespT>

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        require(::responseProducerScope.isInitialized){ "Producer scope was not initialized" }
        callStreamObserver = requestStream.apply { disableAutoInboundFlowControl() }
    }

    fun beforeCallCancellation(message: String?, cause: Throwable?){
        if(!isAborted.getAndSet(true)) {
            val cancellationStatus = Status.CANCELLED
                .withDescription(message)
                .withCause(cause)
                .asRuntimeException()

            responseProducerScope.close(CancellationException(message, cancellationStatus))
        }
    }

    override fun onNext(value: RespT) {
        responseProducerScope.offer(value)
    }

    override fun onError(t: Throwable) {
        isAborted.set(true)
        responseProducerScope.close(t)
        responseProducerScope.cancel(CancellationException(t.message,t))
    }

    override fun onCompleted() {
        isCompleted.set(true)
        responseProducerScope.close()
    }
}

internal class ClientStreamingResponseObserver<ReqT, RespT>(
    private val rpcScope: CoroutineScope,
    private val requestChannel: Channel<ReqT>,
    private val response: CompletableDeferred<RespT>
) : StatefulClientResponseObserver<ReqT, RespT>() {

    private lateinit var readyObserver: CallReadyObserver

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        callStreamObserver = requestStream.apply { disableAutoInboundFlowControl() }
        readyObserver = callStreamObserver.newCallReadyObserver()
    }

    suspend fun awaitReady(){
        readyObserver.awaitReady()
    }

    override fun onNext(value: RespT) {
        response.complete(value)
    }

    override fun onError(t: Throwable) {
        isAborted.set(true)
        response.completeExceptionally(t)
        requestChannel.close(t)
        rpcScope.cancel(CancellationException(t.message, t))
        readyObserver.cancel(t)
    }

    override fun onCompleted() {
        isCompleted.set(true)
        require(response.isCompleted) {
            "Stream was completed before onNext was called"
        }
    }
}

internal class BidiStreamingResponseObserver<ReqT, RespT>(
    private val rpcScope: CoroutineScope
): StatefulClientResponseObserver<ReqT, RespT>() {

    private lateinit var readyObserver: CallReadyObserver

    private val inboundChannel: Channel<RespT> = Channel(Channel.UNLIMITED)

    lateinit var requestChannel: SendChannel<ReqT>
        private set

    lateinit var responseChannel: ReceiveChannel<RespT>
        private set

    override fun beforeStart(requestStream: ClientCallStreamObserver<ReqT>) {
        callStreamObserver = requestStream.apply { disableAutoInboundFlowControl() }
        readyObserver = callStreamObserver.newCallReadyObserver()

        requestChannel = rpcScope.actor(
            capacity = Channel.RENDEZVOUS,
            context = Dispatchers.Unconfined
        ) {

            var error: Throwable? = null
            try {
                // We use an iterator to prevent prematurely
                // consuming a message from the channel if the
                // call is not ready for one. This keeps our
                // in-memory buffer from being increased by 1
                val iter = this.channel.iterator()
                while(readyObserver.isReady() && iter.hasNext()){
                    callStreamObserver.onNext(iter.next())
                }
            } catch (e: Throwable) {
                error = e
            } finally {
                if(isActive) {
                    callStreamObserver.completeSafely(error, convertError = false)
                }
            }
        }

        responseChannel = flow<RespT> {
            try {
                emitAll(inboundChannel)
            }catch (e: Throwable){
                if(coroutineContext[Job]!!.isCancelled && isActive){
                    val status = Status.CANCELLED
                        .withDescription(MESSAGE_CLIENT_CANCELLED_CALL)
                        .withCause(e)
                        .asRuntimeException()
                    callStreamObserver.cancel(MESSAGE_CLIENT_CANCELLED_CALL, status)
                    requestChannel.close(status)
                }
                throw e
            }
        }.onEach { if (isActive) callStreamObserver.request(1) }
        // We use buffer RENDEZVOUS on the outer flow so that our
        // `onEach` operator is only invoked each time a message is
        // collected instead of each time a message is received from
        // from the underlying call.
        .buffer(Channel.RENDEZVOUS)
        .produceIn(rpcScope)
    }

    fun beforeCallCancellation(message: String?, cause: Throwable?){
        if(!isAborted.getAndSet(true)) {
            val cancelWith = if(cause is CancellationException) cause else {
                Status.CANCELLED
                    .withDescription(message)
                    .withCause(cause)
                    .asRuntimeException()
            }
            inboundChannel.close(cancelWith)
        }
    }

    override fun onNext(value: RespT) {
        inboundChannel.offer(value)
    }

    override fun onError(t: Throwable) {
        isAborted.set(true)
        inboundChannel.close(t)
        requestChannel.close(t)
        readyObserver.cancel(t)
    }

    override fun onCompleted() {
        isCompleted.set(true)
        inboundChannel.close()
    }
}

internal fun <ReqT, RespT> BidiStreamingResponseObserver<ReqT, RespT>.asClientBidiCallChannel()
        : ClientBidiCallChannel<ReqT, RespT> =
    object : ClientBidiCallChannel<ReqT, RespT>,
        SendChannel<ReqT> by requestChannel,
        ReceiveChannel<RespT> by responseChannel {
        override val requestChannel: SendChannel<ReqT>
            get() = this@asClientBidiCallChannel.requestChannel
        override val responseChannel: ReceiveChannel<RespT>
            get() = this@asClientBidiCallChannel.responseChannel
    }
