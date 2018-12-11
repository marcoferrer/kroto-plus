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
import kotlin.coroutines.CoroutineContext


object CommonClassNames{

    val bindableService: ClassName = BindableService::class.asClassName()
    val coroutineScope: ClassName = CoroutineScope::class.asClassName()
    val coroutineContext: ClassName = CoroutineContext::class.asClassName()
    val receiveChannel: ClassName = ReceiveChannel::class.asClassName()
    val dispatchers: ClassName = Dispatchers::class.asClassName()
    val completableDeferred: ClassName = CompletableDeferred::class.asClassName()
    val streamObserver: ClassName = ClassName("io.grpc.stub", "StreamObserver")
    val serverCalls = ClassName(krotoCoroutineLib,"ServerCalls")
    val launch: ClassName = ClassName(kotlinxCoroutines,"launch")
    val grpcContextElement: ClassName = ClassName(krotoCoroutineLib,"GrpcContextElement")
    val obsoleteCoroutinesApi: ClassName = ClassName(kotlinxCoroutines, "ObsoleteCoroutinesApi")
    val experimentalCoroutinesApi: ClassName = ClassName(kotlinxCoroutines, "ExperimentalCoroutinesApi")

    object ServerCalls {

        val serverCallUnary = ClassName(krotoCoroutineLib,"serverCallUnary")
        val serverCallClientStreaming = ClassName(krotoCoroutineLib,"serverCallClientStreaming")
        val serverCallUnimplementedUnary = ClassName(krotoCoroutineLib,"serverCallUnimplementedUnary")

    }
}

object CommonPackages {

    const val krotoCoroutineLib = "com.github.marcoferrer.krotoplus.coroutines"
    const val kotlinxCoroutines = "kotlinx.coroutines"
}