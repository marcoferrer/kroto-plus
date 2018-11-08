package krotoplus.example

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandServiceGrpc
import jojo.bizarre.adventure.stand.StandServiceProto

class DummyStandService : StandServiceGrpc.StandServiceImplBase() {

    fun getStandByName(name: String): StandProto.Stand? =
        when (name) {
            "Jotaro Kujo" -> stands["Star Platinum"]
            "Dio Brando" -> stands["The World"]
            else -> null
        }


    override fun getStandByCharacterName(
        request: StandServiceProto.GetStandByCharacterNameRequest,
        responseObserver: StreamObserver<StandProto.Stand>
    ) {
        getStandByName(request.name)
            ?.let { responseObserver.onNext(it) }
            ?: run {
                responseObserver.onError(Status.NOT_FOUND.asException())
                return
            }

        responseObserver.onCompleted()
    }

    override fun getStandByCharacter(
        request: CharacterProto.Character,
        responseObserver: StreamObserver<StandProto.Stand>
    ) {
        getStandByName(request.name)
            ?.let { responseObserver.onNext(it) }
            ?: run {
                responseObserver.onError(Status.NOT_FOUND.asException())
                return
            }

        responseObserver.onCompleted()
    }

    override fun getStandsForCharacters(responseObserver: StreamObserver<StandProto.Stand>): StreamObserver<CharacterProto.Character> {

        return object : StreamObserver<CharacterProto.Character> {
            override fun onNext(value: CharacterProto.Character) {
                println("Client Sent: ${value.name}")
                getStandByName(value.name)?.let {
                    responseObserver.onNext(it)
                    responseObserver.onNext(it)
                    responseObserver.onNext(it)
                }
                    ?: run {
                        responseObserver.onError(Status.NOT_FOUND.asException())
                        return
                    }
            }

            override fun onError(t: Throwable?) {
                println("Client Sent Error: ${t?.message}")
            }

            override fun onCompleted() {
                println("Client Called onComplete")
                responseObserver.onCompleted()
            }
        }
    }

    override fun getAllStandsStream(request: Empty?, responseObserver: StreamObserver<StandProto.Stand>) {
        for ((_, stand) in stands)
            responseObserver.onNext(stand)

        responseObserver.onCompleted()
    }

    override fun getStandByName(
        request: StandServiceProto.GetStandByNameRequest,
        responseObserver: StreamObserver<StandProto.Stand>
    ) {
        stands[request.name]?.let {
            responseObserver.onNext(it)
            responseObserver.onCompleted()
        } ?: responseObserver.onError(Status.NOT_FOUND.asException())
    }
}