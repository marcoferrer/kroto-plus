package com.github.marcoferrer.krotoplus.generators

import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.HelloWorldProtoDslBuilder
import org.junit.Test
import test.message.TestMessages
import test.message.TestMessagesDslBuilder

class ProtoBuildersGeneratorTests {

    @Test
    fun `Test DSL Markers Insertion`(){
        assert(HelloRequest.newBuilder() is HelloWorldProtoDslBuilder)
        assert(HelloReply.newBuilder() is HelloWorldProtoDslBuilder)
        assert(
            TestMessages.L1Message2.L2Nested2.L3Nested2.L4Nested1.L5Nested1
            .newBuilder() is TestMessagesDslBuilder
        )
    }

}