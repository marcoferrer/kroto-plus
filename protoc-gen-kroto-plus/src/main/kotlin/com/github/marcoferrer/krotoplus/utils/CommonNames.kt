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

package com.github.marcoferrer.krotoplus.utils

import com.github.marcoferrer.krotoplus.utils.CommonPackages.kotlinxCoroutines
import com.github.marcoferrer.krotoplus.utils.CommonPackages.krotoCoroutineLib
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import io.grpc.BindableService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


object CommonClassNames{

    val bindableService: ClassName = BindableService::class.asClassName()
    val coroutineScope: ClassName = CoroutineScope::class.asClassName()
    val coroutineContext: ClassName = CoroutineContext::class.asClassName()
    val emptyCoroutineContext: ClassName = EmptyCoroutineContext::class.asClassName()
    val receiveChannel: ClassName = ReceiveChannel::class.asClassName()
    val sendChannel: ClassName = SendChannel::class.asClassName()
    val dispatchers: ClassName = Dispatchers::class.asClassName()
    val completableDeferred: ClassName = CompletableDeferred::class.asClassName()

    val launch = ClassName(kotlinxCoroutines,"launch")
    val obsoleteCoroutinesApi = ClassName(kotlinxCoroutines, "ObsoleteCoroutinesApi")
    val experimentalCoroutinesApi = ClassName(kotlinxCoroutines, "ExperimentalCoroutinesApi")


    val grpcChannel = ClassName("io.grpc","Channel")
    val grpcCallOptions = ClassName("io.grpc","CallOptions")
    val grpcServerServiceDefinition = ClassName("io.grpc", "ServerServiceDefinition")
    val grpcAbstractStub = ClassName("io.grpc.stub", "AbstractStub")
    val grpcStubRpcMethod = ClassName("io.grpc.stub.annotations","RpcMethod")

    val streamObserver: ClassName = ClassName("io.grpc.stub", "StreamObserver")

    val experimentalKrotoPlusCoroutinesApi = ClassName(krotoCoroutineLib, "ExperimentalKrotoPlusCoroutinesApi")
    val coroutineService = ClassName("$krotoCoroutineLib.server", "CoroutineService")

    val listenableFuture = ClassName("com.google.common.util.concurrent", "ListenableFuture")
    val grpcContextElement = ClassName(krotoCoroutineLib,"GrpcContextElement")
    val suspendingUnaryCallObserver: ClassName = ClassName(krotoCoroutineLib,"suspendingUnaryCallObserver")
    val bidiCallChannel: ClassName = ClassName(krotoCoroutineLib,"bidiCallChannel")
    val clientBidiCallChannel: ClassName = ClassName(krotoCoroutineLib,"ClientBidiCallChannel")
    val inboundStreamChannel: ClassName = ClassName(krotoCoroutineLib,"InboundStreamChannel")

    object ClientCalls {
        val clientCallUnary = ClassName("$krotoCoroutineLib.client", "clientCallUnary")
        val clientCallServerStreaming = ClassName("$krotoCoroutineLib.client", "clientCallServerStreaming")
        val clientCallBidiStreaming = ClassName("$krotoCoroutineLib.client", "clientCallBidiStreaming")
        val clientCallClientStreaming = ClassName("$krotoCoroutineLib.client", "clientCallClientStreaming")
    }

    object ClientChannels {
        val clientBidiCallChannel = ClassName("$krotoCoroutineLib.client", "ClientBidiCallChannel")
        val clientStreamingCallChannel = ClassName("$krotoCoroutineLib.client", "ClientStreamingCallChannel")
    }

    object ServerCalls {

        val serverCallUnary = ClassName("$krotoCoroutineLib.server","serverCallUnary")
        val serverCallClientStreaming = ClassName("$krotoCoroutineLib.server","serverCallClientStreaming")
        val serverCallServerStreaming = ClassName("$krotoCoroutineLib.server","serverCallServerStreaming")
        val serverCallBidiStreaming = ClassName("$krotoCoroutineLib.server","serverCallBidiStreaming")
        val serverCallUnimplementedUnary = ClassName("$krotoCoroutineLib.server","serverCallUnimplementedUnary")
        val serverCallUnimplementedStream = ClassName("$krotoCoroutineLib.server","serverCallUnimplementedStream")
    }
}

object CommonPackages {

    const val krotoCoroutineLib = "com.github.marcoferrer.krotoplus.coroutines"
    const val kotlinxCoroutines = "kotlinx.coroutines"
}