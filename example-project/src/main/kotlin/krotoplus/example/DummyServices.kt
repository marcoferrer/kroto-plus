package krotoplus.example

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandServiceCoroutineGrpc
import jojo.bizarre.adventure.stand.StandServiceProto
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope

class DummyStandService : StandServiceCoroutineGrpc.StandServiceImplBase() {

    fun getStandByName(name: String): StandProto.Stand? =
        when (name) {
            "Jotaro Kujo" -> stands["Star Platinum"]
            "Dio Brando" -> stands["The World"]
            else -> null
        }

    override suspend fun getStandByCharacterName(
        request: StandServiceProto.GetStandByCharacterNameRequest
    ): StandProto.Stand = coroutineScope {
        getStandByName(request.name)
            ?: throw Status.NOT_FOUND.asException()
    }

    override suspend fun getStandByCharacter(request: CharacterProto.Character): StandProto.Stand = coroutineScope {
        getStandByName(request.name) ?: throw Status.NOT_FOUND.asException()
    }

    override suspend fun getStandsForCharacters(
        requestChannel: ReceiveChannel<CharacterProto.Character>,
        responseChannel: SendChannel<StandProto.Stand>
    ) {
        coroutineScope {
            requestChannel.consumeEach { character ->

                getStandByName(character.name)
                    ?.let { stand ->
                        repeat(3) {
                            responseChannel.send(stand)
                        }
                    }
                    ?: throw Status.NOT_FOUND.asException()
            }
        }
    }

    override suspend fun getAllStandsStream(request: Empty, responseChannel: SendChannel<StandProto.Stand>) {
        coroutineScope {
            stands.values.forEach {
                responseChannel.send(it)
            }
        }
    }

    override suspend fun getStandByName(request: StandServiceProto.GetStandByNameRequest): StandProto.Stand =
        coroutineScope {
            stands[request.name] ?: throw Status.NOT_FOUND.asException()
        }

}