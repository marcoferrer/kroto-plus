import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.benchmarks.proto.BenchmarkServiceGrpc
import io.grpc.benchmarks.proto.Messages

class BenchMarkService : BenchmarkServiceGrpc.BenchmarkServiceImplBase(){

    private val BIDI_RESPONSE_BYTES = 100
    private val BIDI_RESPONSE = Messages.SimpleResponse
        .newBuilder()
        .setPayload(
            Messages.Payload.newBuilder()
                .setBody(ByteString.copyFrom(ByteArray(BIDI_RESPONSE_BYTES))).build()
        )
        .build()
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