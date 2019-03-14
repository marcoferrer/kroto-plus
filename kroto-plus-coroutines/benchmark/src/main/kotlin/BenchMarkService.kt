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

import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.benchmarks.proto.BenchmarkServiceCoroutineGrpc
import io.grpc.benchmarks.proto.Messages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.*

@UseExperimental(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
class BenchMarkService : BenchmarkServiceCoroutineGrpc.BenchmarkServiceImplBase() {

    private val BIDI_RESPONSE_BYTES = 100
    private val BIDI_RESPONSE = Messages.SimpleResponse
        .newBuilder()
        .setPayload(
            Messages.Payload.newBuilder()
                .setBody(ByteString.copyFrom(ByteArray(BIDI_RESPONSE_BYTES))).build()
        )
        .build()

    override suspend fun unaryCall(request: Messages.SimpleRequest): Messages.SimpleResponse =
        makeResponse(request)

    override suspend fun streamingCall(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
        requestChannel.mapTo(responseChannel) { makeResponse(it) }
    }

    override suspend fun streamingFromClient(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>
    ): Messages.SimpleResponse {

        val lastSeen = requestChannel
            .toList()
            .lastOrNull()
            ?: throw Status.FAILED_PRECONDITION
                .withDescription("never received any requests")
                .asException()

        return makeResponse(lastSeen)
    }

    override suspend fun streamingFromServer(
        request: Messages.SimpleRequest,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
        val response = makeResponse(request)

        while (!responseChannel.isClosedForSend) {
            responseChannel.send(response)
        }
    }

    override suspend fun streamingBothWays(
        requestChannel: ReceiveChannel<Messages.SimpleRequest>,
        responseChannel: SendChannel<Messages.SimpleResponse>
    ) {
        while (!requestChannel.isClosedForReceive) {
            responseChannel.send(BIDI_RESPONSE)
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