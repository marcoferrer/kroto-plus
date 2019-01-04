import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.benchmarks.proto.BenchmarkServiceCoroutineGrpc
import io.grpc.benchmarks.proto.Messages
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.coroutineScope

class BenchMarkService : BenchmarkServiceCoroutineGrpc.BenchmarkServiceImplBase(){

    private val BIDI_RESPONSE_BYTES = 100
    private val BIDI_RESPONSE = Messages.SimpleResponse
        .newBuilder()
        .setPayload(
            Messages.Payload.newBuilder()
                .setBody(ByteString.copyFrom(ByteArray(BIDI_RESPONSE_BYTES))).build()
        )
        .build()

    override suspend fun unaryCall(
        request: Messages.SimpleRequest
    ): Messages.SimpleResponse = coroutineScope {
        makeResponse(request)
    }

    override suspend fun streamingCall(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
       coroutineScope {
           requestChannel.mapTo(responseChannel){ makeResponse(it) }
       }
    }

    override suspend fun streamingFromClient(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>
    ): Messages.SimpleResponse = coroutineScope {

        val lastSeen = requestChannel
            .toList()
            .lastOrNull()
            ?: throw Status.FAILED_PRECONDITION
                .withDescription("never received any requests")
                .asException()

        makeResponse(lastSeen)
    }

    override suspend fun streamingFromServer(
        request: Messages.SimpleRequest,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
        coroutineScope {
            val response = makeResponse(request)

            while (!responseChannel.isClosedForSend) {
                responseChannel.send(response)
            }
        }
    }

    override suspend fun streamingBothWays(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
        coroutineScope {
            while(!requestChannel.isClosedForReceive){
                responseChannel.send(BIDI_RESPONSE)
            }
        }
    }
}

// Copied from
// https://github.com/grpc/grpc-java/blob/847eae8d37af91ca2d9d4ebfff4d0ed4cd3f78bf/benchmarks/src/main/java/io/grpc/benchmarks/Utils.java#L249
fun makeResponse(request: Messages.SimpleRequest): Messages.SimpleResponse {
    if (request.responseSize > 0) {
        if (Messages.PayloadType.COMPRESSABLE != request.responseType) {
            throw Status.INTERNAL.augmentDescription("Error creating payload.").asRuntimeException()
        }

        val body = ByteString.copyFrom(ByteArray(request.responseSize))
        val type = request.responseType

        val payload = Messages.Payload.newBuilder().setType(type).setBody(body).build()
        return Messages.SimpleResponse.newBuilder().setPayload(payload).build()
    }
    return Messages.SimpleResponse.getDefaultInstance()
}