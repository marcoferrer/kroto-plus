import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList

class Greeter : GreeterCoroutineGrpc.GreeterImplBase() {

    private val validNameRegex = Regex("[^0-9]*")

    override suspend fun sayHello(request: HelloRequest, completableResponse: CompletableDeferred<HelloReply>) {

        if (request.name.matches(validNameRegex)) {
            completableResponse
                .complete { message = "Hello there, ${request.name}!" }
        } else {
            completableResponse
                .completeExceptionally(Status.INVALID_ARGUMENT.asRuntimeException())
        }
    }

    override suspend fun sayHelloStreaming(
        requestChannel: ReceiveChannel<HelloRequest>,
        responseChannel: SendChannel<HelloReply>
    ) {
        requestChannel.consumeEach { request ->

            responseChannel
                .send { message = "Hello there, ${request.name}!" }
        }

        responseChannel.close()
    }

    override suspend fun sayHelloClientStreaming(
        requestChannel: ReceiveChannel<HelloRequest>,
        completableResponse: CompletableDeferred<HelloReply>
    ) {
        completableResponse.complete {
            message = requestChannel.toList().joinToString()
        }
    }

    override suspend fun sayHelloServerStreaming(request: HelloRequest, responseChannel: SendChannel<HelloReply>) {
        for(char in request.name) {

            responseChannel.send {
                message = "Hello $char!"
            }
        }
        responseChannel.close()
    }
}