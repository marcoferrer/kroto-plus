import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.send
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach

val server = InProcessServerBuilder
    .forName("helloworld")
    .addService(Greeter())
    .directExecutor()
    .build()
    .start()

val channel = InProcessChannelBuilder
    .forName("helloworld")
    .directExecutor()
    .build()

suspend fun main(){
    coroutineScope {

        // Optional coroutineContext. Default is Dispatchers.Unconfined
        val stub = GreeterCoroutineGrpc
            .newStub(channel)
            .withCoroutineContext(Dispatchers.IO)

        performUnaryCall(stub)

        performBidiCall(stub)

        performClientStreamingCall(stub)

        performServerStreamingCall(stub)
    }

    server.shutdown()
}

suspend fun performUnaryCall(stub: GreeterCoroutineGrpc.GreeterCoroutineStub){

    val unaryResponse = stub.sayHello { name = "John" }

    println("Unary Response: ${unaryResponse.message}")
}

suspend fun performServerStreamingCall(stub: GreeterCoroutineGrpc.GreeterCoroutineStub){

    val responseChannel = stub.sayHelloServerStreaming { name = "John" }

    responseChannel.consumeEach {
        println("Server Streaming Response: ${it.message}")
    }
}

suspend fun CoroutineScope.performClientStreamingCall(stub: GreeterCoroutineGrpc.GreeterCoroutineStub){

    // Client Streaming RPC
    val (requestChannel, response) = stub.sayHelloClientStreaming()

    launch {
        repeat(5){
            requestChannel.send { name = "person #$it" }
        }
        requestChannel.close()
    }

    println("Client Streaming Response: ${response.await().toString().trim()}")
}

suspend fun CoroutineScope.performBidiCall(stub: GreeterCoroutineGrpc.GreeterCoroutineStub){

    val (requestChannel, responseChannel) = stub.sayHelloStreaming()

    launch {
        repeat(5){
            requestChannel.send { name = "person #$it" }
        }
        requestChannel.close()
    }

    launch {
        responseChannel.consumeEach {
            println("Bidi Response: ${it.message}")
        }
    }
}