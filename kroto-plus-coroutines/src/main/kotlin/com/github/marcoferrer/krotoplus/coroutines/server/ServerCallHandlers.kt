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

package com.github.marcoferrer.krotoplus.coroutines.server

import com.github.marcoferrer.krotoplus.coroutines.call.applyInboundFlowControl
import com.github.marcoferrer.krotoplus.coroutines.call.applyOutboundFlowControl
import com.github.marcoferrer.krotoplus.coroutines.call.attachOutboundChannelCompletionHandler
import com.github.marcoferrer.krotoplus.coroutines.call.bindToClientCancellation
import com.github.marcoferrer.krotoplus.coroutines.call.completeSafely
import com.github.marcoferrer.krotoplus.coroutines.call.newCallReadyObserver
import com.github.marcoferrer.krotoplus.coroutines.call.newRpcScope
import com.github.marcoferrer.krotoplus.coroutines.call.toRpcException
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.ServerCalls
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

public fun <ReqT, RespT> ServiceScope.unaryServerCallHandler(
    methodHandler: UnaryMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> =
    UnaryServerCallHandler(this, methodHandler)

internal class UnaryServerCallHandler<ReqT, RespT> (
    private val serviceScope: ServiceScope,
    private val methodHandler: UnaryMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> {

    override fun startCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata
    ): ServerCall.Listener<ReqT> {
        val delegate = ServerCalls.asyncUnaryCall<ReqT, RespT> { request, responseObserver ->

                with(newRpcScope(serviceScope.initialContext, call.methodDescriptor)) rpcScope@ {
                bindToClientCancellation(responseObserver as ServerCallStreamObserver<*>)
                launch(start = CoroutineStart.ATOMIC) {
                    try{
                        responseObserver.onNext(methodHandler(request))
                        responseObserver.onCompleted()
                    }catch (e: Throwable){
                        responseObserver.completeSafely(e)
                    }
                }
            }
        }

        return delegate.startCall(call, headers)
    }

}

public fun <ReqT, RespT> ServiceScope.bidiStreamingServerCallHandler(
    methodHandler: BidiStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> =
    BidiStreamingServerCallHandler(this, methodHandler)

internal class BidiStreamingServerCallHandler<ReqT, RespT>(
    private val serviceScope: ServiceScope,
    private val methodHandler: BidiStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> {

    override fun startCall(call: ServerCall<ReqT, RespT>, headers: Metadata): ServerCall.Listener<ReqT> {

        val delegate = with(newRpcScope(serviceScope.initialContext, call.methodDescriptor)) rpcScope@{
            ServerCalls.asyncBidiStreamingCall<ReqT, RespT> { responseObserver ->

                val responseChannel = Channel<RespT>()
                val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
                    .apply { disableAutoInboundFlowControl() }

                bindToClientCancellation(serverCallObserver)
                val outboundMessageHandler = applyOutboundFlowControl(serverCallObserver,responseChannel)
                val requestChannel = ServerRequestStreamChannel<ReqT>(
                    coroutineContext = coroutineContext,
                    callStreamObserver = serverCallObserver,
                    onErrorHandler = {
                        // Call cancellation already cancels the coroutine scope
                        // and closes the response stream. So we dont need to
                        // do anything in this case.
                        if(!serverCallObserver.isCancelled) {
                            // In the event of a request error, we
                            // need to close the responseChannel before
                            // cancelling the rpcScope.
                            responseObserver.completeSafely(it)
                            responseChannel.close(it)
                            this@rpcScope.cancel()
                        }
                    }
                )

                attachOutboundChannelCompletionHandler(
                    serverCallObserver, responseChannel,
                    onSuccess = { outboundMessageHandler.close() }
                )

                launch(start = CoroutineStart.ATOMIC) {
                    serverCallObserver.request(1)
                    try {
                        methodHandler(requestChannel, responseChannel)
                        responseChannel.close()
                    } catch (e: Throwable) {
                        val rpcError = e.toRpcException()
                        serverCallObserver.completeSafely(rpcError)
                        responseChannel.close(rpcError)
                    } finally {
                        if (!requestChannel.isClosedForReceive) {
                            requestChannel.cancel()
                        }
                        outboundMessageHandler.close()
                    }
                }

                bindScopeCompletionToObserver(serverCallObserver)
                requestChannel
            }
        }

        return delegate.startCall(call, headers)
    }

}

public fun <ReqT, RespT> ServiceScope.clientStreamingServerCallHandler(
    methodHandler: ClientStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> =
    ClientStreamingServerCallHandler(this, methodHandler)

internal class ClientStreamingServerCallHandler<ReqT, RespT>(
    private val serviceScope: ServiceScope,
    private val methodHandler: ClientStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> {

    override fun startCall(call: ServerCall<ReqT, RespT>, headers: Metadata): ServerCall.Listener<ReqT> {

        val delegate = with(newRpcScope(serviceScope.initialContext, call.methodDescriptor)) rpcScope@ {
            ServerCalls.asyncClientStreamingCall<ReqT, RespT> { responseObserver ->

                val activeInboundJobCount = AtomicInteger()
                val inboundChannel = Channel<ReqT>()

                val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
                    .apply { applyInboundFlowControl(inboundChannel, activeInboundJobCount) }

                bindToClientCancellation(serverCallObserver)

                val requestChannel = ServerRequestStreamChannel(
                    coroutineContext = coroutineContext,
                    inboundChannel = inboundChannel,
                    transientInboundMessageCount = activeInboundJobCount,
                    callStreamObserver = serverCallObserver,
                    onErrorHandler = {
                        // Call cancellation already cancels the coroutine scope
                        // and closes the response stream. So we dont need to
                        // do anything in this case.
                        if(!serverCallObserver.isCancelled) {
                            this@rpcScope.cancel()
                            responseObserver.completeSafely(it)
                        }
                    }
                )

                launch(start = CoroutineStart.ATOMIC) {
                    try {
                        responseObserver.onNext(methodHandler(requestChannel))
                        responseObserver.onCompleted()
                    } catch (e: Throwable) {
                        responseObserver.completeSafely(e)
                    } finally {
                        if (!requestChannel.isClosedForReceive) {
                            requestChannel.cancel()
                        }
                    }
                }

                requestChannel
            }
        }

        return delegate.startCall(call, headers)
    }

}

public fun <ReqT, RespT> ServiceScope.serverStreamingServerCallHandler(
    methodHandler: ServerStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> =
    ServerStreamingServerCallHandler(this, methodHandler)


internal class ServerStreamingServerCallHandler<ReqT, RespT>(
    private val serviceScope: ServiceScope,
    private val methodHandler: ServerStreamingMethod<ReqT, RespT>
) : ServerCallHandler<ReqT, RespT> {

    override fun startCall(call: ServerCall<ReqT, RespT>, headers: Metadata): ServerCall.Listener<ReqT> {

        val delegate = ServerCalls.asyncServerStreamingCall<ReqT, RespT> { request, responseObserver ->

            val rpcScope = newRpcScope(serviceScope.initialContext, call.methodDescriptor)
            val cancellationHandler = DeferredCancellationHandler(rpcScope)

            val serverCallObserver = (responseObserver as ServerCallStreamObserver<RespT>)
                .apply { setOnCancelHandler(cancellationHandler) }

            val readyObserver = serverCallObserver.newCallReadyObserver()

            val responseFlow = callbackFlow<RespT> {
                cancellationHandler.onMethodHandlerStart()
                methodHandler(request, channel)
                channel.close()
            }.buffer(Channel.RENDEZVOUS).onEach { message ->
                if(readyObserver.isReady()){
                    serverCallObserver.onNext(message)
                }
            }

            rpcScope.launch(start = CoroutineStart.ATOMIC) {
                try {
                    responseFlow.collect()
                    serverCallObserver.onCompleted()
                }catch (e: Throwable){
                    serverCallObserver.completeSafely(e)
                }
            }
        }

        return delegate.startCall(call, headers)
    }

}