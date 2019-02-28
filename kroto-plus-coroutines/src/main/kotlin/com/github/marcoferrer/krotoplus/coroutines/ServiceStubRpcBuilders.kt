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

package com.github.marcoferrer.krotoplus.coroutines

import com.github.marcoferrer.krotoplus.coroutines.call.newSendChannelFromObserver
import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannel
import com.github.marcoferrer.krotoplus.coroutines.client.ClientBidiCallChannelImpl
import io.grpc.stub.AbstractStub
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*

@Deprecated(
    message = "Deprecated in favor of back-pressure supporting APIs.",
    level = DeprecationLevel.WARNING
)
suspend inline fun <T : AbstractStub<T>, reified R> T.suspendingUnaryCallObserver(
    crossinline block: T.(StreamObserver<R>) -> Unit
): R = suspendCancellableCoroutine { cont: CancellableContinuation<R> ->
    block(SuspendingUnaryObserver(cont))
}

/**
 * Marked as [ObsoleteCoroutinesApi] due to usage of [CoroutineScope.actor]
 * Marked as [ExperimentalCoroutinesApi] due to usage of [Dispatchers.Unconfined]
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@ExperimentalKrotoPlusCoroutinesApi
@Deprecated(
    message = "Deprecated in favor of back-pressure supporting APIs.",
    level = DeprecationLevel.WARNING
)
fun <T : AbstractStub<T>, ReqT, RespT> T.bidiCallChannel(
    block: T.(StreamObserver<RespT>) -> StreamObserver<ReqT>
): ClientBidiCallChannel<ReqT, RespT> {

    val responseObserverChannel = InboundStreamChannel<RespT>()
    val requestObserver = block(responseObserverChannel)

    val requestObserverChannel = CoroutineScope(coroutineContext)
        .newSendChannelFromObserver(requestObserver)

    return ClientBidiCallChannelImpl(
        requestObserverChannel,
        responseObserverChannel
    )
}


