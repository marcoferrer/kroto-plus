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

@file:UseExperimental(ExperimentalCoroutinesApi::class,ObsoleteCoroutinesApi::class)

package com.github.marcoferrer.krotoplus.generators

import com.github.marcoferrer.krotoplus.coroutines.launchProducerJob
import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.sayHello
import io.grpc.examples.helloworld.sayHelloClientStreaming
import io.grpc.examples.helloworld.sayHelloServerStreaming
import io.grpc.examples.helloworld.sayHelloStreaming
import io.grpc.examples.helloworld.send
import io.grpc.stub.StreamObserver
import io.grpc.testing.GrpcServerRule
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrpcStubExtsGeneratorTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun setupService(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){

            override val initialContext: CoroutineContext
                get() = Dispatchers.Unconfined

            override suspend fun sayHelloClientStreaming(requestChannel: ReceiveChannel<HelloRequest>): HelloReply {
                return HelloReply {
                    message = requestChannel.toList().joinToString(separator = "|"){ it.name }
                }
            }

            override suspend fun sayHelloStreaming(
                requestChannel: ReceiveChannel<HelloRequest>,
                responseChannel: SendChannel<HelloReply>
            ) {
                requestChannel.consumeEach { request ->
                    repeat(3) {
                        responseChannel.send { message = request.name }
                    }
                }
            }
        })
    }

    @Test
    fun `Client streaming coroutine exts are generated`() = runBlocking {
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
            .withCoroutineContext()

        val (requestChannel, response) = stub.sayHelloClientStreaming()

        launchProducerJob(requestChannel){
            repeat(3){
                send { name = "name $it" }
            }
        }
        assertEquals("name 0|name 1|name 2",response.await().message)
    }

    @Test
    fun `Bidi streaming coroutine exts are generated`() {
        runBlocking {

            val stub = GreeterGrpc.newStub(grpcServerRule.channel)
                .withCoroutineContext()

            val (requestChannel, responseChannel) = stub.sayHelloStreaming()

            launchProducerJob(requestChannel) {
                repeat(3) {
                    send { name = "name $it" }
                }
            }

            val results = responseChannel.toList()
            assertEquals(9, results.size)

            val expected = "name 0|name 0|name 0" +
                    "|name 1|name 1|name 1" +
                    "|name 2|name 2|name 2"
            assertEquals(
                expected,
                results.joinToString(separator = "|") { it.message }
            )
        }
    }
}

class GrpcStubServerStreamingExtsGeneratorTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun setupService(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHelloServerStreaming(
                request: HelloRequest,
                responseChannel: SendChannel<HelloReply>
            ) {
                repeat(3){
                    responseChannel.send { message = request.name + "-$it" }
                }
            }
        })
    }

    @Test
    fun `Async stub exts for default arg are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHelloServerStreaming(responseObserver)
        while(!responseObserver.isCompleted.get()){}
        repeat(3){ index ->
            verify(exactly = 1) { responseObserver.onNext(match { it.message == "-$index" }) }
        }
    }

    @Test
    fun `Async stub exts for lambda builders are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHelloServerStreaming(responseObserver){ name = "test" }
        while(!responseObserver.isCompleted.get()){}
        repeat(3){ index ->
            verify(exactly = 1) { responseObserver.onNext(match { it.message == "test-$index" }) }
        }
    }

    @Test
    fun `Async stub exts for method signatures are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHelloServerStreaming("test", responseObserver)
        while(!responseObserver.isCompleted.get()){}
        repeat(3){ index ->
            verify(exactly = 1) { responseObserver.onNext(match { it.message == "test-$index" }) }
        }
    }

    @Test
    fun `Blocking stub default arg exts are generated`() {
        val stub = GreeterGrpc.newBlockingStub(grpcServerRule.channel)

        val result = stub.sayHelloServerStreaming().asSequence().toList()
        assertEquals(3, result.size)
        result.withIndex().forEach { (index, reply) ->
            assertEquals("-$index",reply.message)
        }
    }

    @Test
    fun `Blocking stub lambda builder exts are generated`() {
        val stub = GreeterGrpc.newBlockingStub(grpcServerRule.channel)

        val result = stub.sayHelloServerStreaming { name = "with-arg" }.asSequence().toList()
        assertEquals(3, result.size)
        result.withIndex().forEach { (index, reply) ->
            assertEquals("with-arg-$index",reply.message)
        }
    }

    @Test
    fun `Blocking stub method signature exts are generated`() {
        val stub = GreeterGrpc.newBlockingStub(grpcServerRule.channel)

        val result = stub.sayHelloServerStreaming(name = "with-arg").asSequence().toList()
        assertEquals(3, result.size)
        result.withIndex().forEach { (index, reply) ->
            assertEquals("with-arg-$index",reply.message)
        }
    }

    @Test
    fun `Async stub coroutine exts are generated`() = runBlocking {
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
            .withCoroutineContext()

        // Default Value Ext
        val response1 = stub.sayHelloServerStreaming()
        repeat(3){
            assertEquals("-$it",response1.receive().message)
        }
        assertNull(response1.receiveOrNull())
        assert(response1.isClosedForReceive)

        // Single Message Parameter Ext
        val response2 = stub.sayHelloServerStreaming(HelloRequest { name = "with-arg" })
        repeat(3){
            assertEquals("with-arg-$it",response2.receive().message)
        }
        assertNull(response2.receiveOrNull())
        assert(response2.isClosedForReceive)
    }

    @Test
    fun `Async stub coroutine lambda builder exts are generated`() = runBlocking {
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
            .withCoroutineContext()

        // Message Builder Ext
        val response = stub.sayHelloServerStreaming { name = "with-block" }
        repeat(3){
            assertEquals("with-block-$it",response.receive().message)
        }
        assertNull(response.receiveOrNull())
        assert(response.isClosedForReceive)
    }

    @Test
    fun `Async stub coroutine method signature exts are generated`() = runBlocking {
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
            .withCoroutineContext()

        // Method signature Ext
        val response = stub.sayHelloServerStreaming(name = "with-block")
        repeat(3){
            assertEquals("with-block-$it",response.receive().message)
        }
        assertNull(response.receiveOrNull())
        assert(response.isClosedForReceive)
    }
}

class GrpcStubUnaryExtsGeneratorTests {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun setupService(){
        grpcServerRule.serviceRegistry.addService(object : GreeterCoroutineGrpc.GreeterImplBase(){
            override val initialContext: CoroutineContext = Dispatchers.Unconfined
            override suspend fun sayHello(request: HelloRequest): HelloReply {
                return HelloReply { message = "result-${request.name}" }
            }
        })
    }

    @Test
    fun `Async stub exts for default arg are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHello(responseObserver)
        while(!responseObserver.isCompleted.get()){}
        verify(exactly = 1) { responseObserver.onNext(match { it.message == "result-" }) }
    }

    @Test
    fun `Async stub exts for lambda builders are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHello(responseObserver){ name = "test" }
        while(!responseObserver.isCompleted.get()){}
        verify(exactly = 1) { responseObserver.onNext(match { it.message == "result-test" }) }
    }

    @Test
    fun `Async stub exts for method signatures are generated`() {
        val responseObserver = spyk(TestObserver())
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)

        stub.sayHello("test", responseObserver)
        while(!responseObserver.isCompleted.get()){}
        verify(exactly = 1) { responseObserver.onNext(match { it.message == "result-test" }) }
    }

    @Test
    fun `Blocking stub exts are generated`(){
        val stub = GreeterGrpc.newBlockingStub(grpcServerRule.channel)
        assertEquals("result-",stub.sayHello(HelloRequest.getDefaultInstance()).message)
        assertEquals("result-",stub.sayHello().message)
        assertEquals("result-test",stub.sayHello { name = "test" }.message)
        assertEquals("result-test",stub.sayHello(name = "test").message)
    }

    @Test
    fun `Future stub exts are generated`(){
        val stub = GreeterGrpc.newFutureStub(grpcServerRule.channel)
        assertEquals("result-",stub.sayHello(HelloRequest.getDefaultInstance()).get().message)
        assertEquals("result-",stub.sayHello().get().message)
        assertEquals("result-test",stub.sayHello { name = "test" }.get().message)
        assertEquals("result-test",stub.sayHello(name = "test").get().message)
    }

    @Test
    fun `Async stub coroutine exts are generated`() = runBlocking {
        val stub = GreeterGrpc.newStub(grpcServerRule.channel)
        assertEquals("result-",stub.sayHello(HelloRequest.getDefaultInstance()).message)
        assertEquals("result-",stub.sayHello().message)
        assertEquals("result-test",stub.sayHello { name = "test" }.message)
        assertEquals("result-test",stub.sayHello(name = "test").message)
    }
}

class TestObserver : StreamObserver<HelloReply> {
    val isCompleted = AtomicBoolean()
    override fun onNext(value: HelloReply?) {}
    override fun onError(t: Throwable?) { isCompleted.set(true) }
    override fun onCompleted() { isCompleted.set(true) }
}