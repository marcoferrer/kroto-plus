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

import com.google.common.util.concurrent.UncaughtExceptionHandlers
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.Dispatchers

import java.util.concurrent.*
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory
import java.util.concurrent.atomic.AtomicInteger

// TODO: add args to enabled direct executor
fun main(){

//    ForkJoinPool.commonPool()
//    Dispatchers.Default
//    val fjpExecutor = ForkJoinPool(
//        4 /* parallelism*/, object : ForkJoinWorkerThreadFactory {
//
//        private val num = AtomicInteger()
//        override fun newThread(pool: ForkJoinPool): ForkJoinWorkerThread {
//            val thread = defaultForkJoinWorkerThreadFactory.newThread(pool)
//            thread.isDaemon = true
//            thread.name = "grpc-server-app-" + "-" + num.getAndIncrement()
//            return thread
//        }
//    }, UncaughtExceptionHandlers.systemExit(), true /* async */)

    val server = NettyServerBuilder
        .forPort(8000)
        .addService(BenchMarkService())
//        .executor(ForkJoinPool.commonPool())
        .directExecutor()
        .build()
        .apply { start() }

    server.awaitTermination()
}
