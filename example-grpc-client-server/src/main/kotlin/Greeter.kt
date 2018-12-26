import io.grpc.Status
import io.grpc.examples.helloworld.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlin.coroutines.CoroutineContext


class Service : CoroutineScope {

    private val adapter = Adapter()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    suspend fun computeValue(){

    }

    inner class Adapter {

        fun adaptedComputeValue(){
            launch {
                println("AdapterLaunch " + coroutineContext[Job])
                computeValue()
            }
        }
    }
}

//interface ServiceScope : CoroutineScope

//
//public suspend fun <R> ServiceScope.launchRpc(block: suspend CoroutineScope.() -> Unit): RpcJob =
//    suspendCoroutineUninterceptedOrReturn { uCont ->
//
//        val delegateJob = CoroutineScope(uCont.context)
//            .launch(block = block)
//
//        RpcJobImpl(delegateJob)
//    }
//
//interface RpcJob : Job
//
//internal class RpcJobImpl(private val jobDelegate: Job) : RpcJob, Job by jobDelegate

suspend fun <R> CompletableDeferred<R>.respondWith(block: suspend CoroutineScope.()->R) {
    coroutineScope {
        try {
            complete(block())
        }catch (e: Throwable){
            completeExceptionally(e)
        }
    }
}

class Greeter : GreeterCoroutineGrpc.GreeterImplBase() {

    private val validNameRegex = Regex("[^0-9]*")

//    override
    suspend fun sayHello222(request: HelloRequest, completableResponse: CompletableDeferred<HelloReply>) =
        coroutineScope {

            println("sayhello ${coroutineContext[Job]}")
            if (request.name.matches(validNameRegex)) {
                completableResponse
                    .complete { message = "Hello there, ${request.name}!" }
            } else {
                completableResponse
                    .completeExceptionally(Status.INVALID_ARGUMENT.asRuntimeException())
            }
            delay(1000L)
            println("sayhello fin ${coroutineContext[Job]}")

        }

    override suspend fun sayHello(request: HelloRequest, completableResponse: CompletableDeferred<HelloReply>) =
        completableResponse.respondWith {
            println(coroutineContext[Job]?.get(Job) .toString() +"jobbbbbb")
            if (request.name.matches(validNameRegex)) {
                    HelloWorldProtoBuilders.HelloReply {
                    message = "Hello there, ${request.name}!"
                }
            } else {
                throw Status.INVALID_ARGUMENT.asException()
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