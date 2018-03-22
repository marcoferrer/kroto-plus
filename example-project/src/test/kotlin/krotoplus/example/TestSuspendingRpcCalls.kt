package krotoplus.example

import com.github.mferrer.krotoplus.coroutines.InboundStreamChannel
import com.github.mferrer.krotoplus.coroutines.RpcBidiChannel
import com.github.mferrer.krotoplus.coroutines.bidiCallChannel
import com.github.mferrer.krotoplus.coroutines.suspendingUnaryCallObserver
import com.github.mferrer.krotoplus.test.ServiceBindingServerRule
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.stand.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestSuspendingRpcCalls{

    @[Rule JvmField]
    var grpcServerRule = ServiceBindingServerRule(MockCharacterService())
            .directExecutor()!!

    @Test fun `Test Suspending Unary Rpc Calls`() = runBlocking {

        grpcServerRule.serviceRegistry.addService(MockStandService())

        MockStandService.apply {
            getStandByCharacterNameResponseQueue
                    .addMessage(stands["The World"]!!)

            getStandByCharacterResponseQueue
                    .addMessage(stands["Star Platinum"]!!)
        }

        val standServiceStub = StandServiceGrpc.newStub(grpcServerRule.channel)

        val deferredCallResponse = async {
            standServiceStub.getStandByCharacterName { name = "Dio Brando" }
        }

        val suspendingCallResponse = standServiceStub.getStandByCharacter(characters["Jotaro Kujo"]!!)

        assertEquals("""name: "Star Platinum"
        |attacks {
        |  name: "ZA WARUDO"
        |  range: MEDIUM
        |}
        |attacks {
        |  name: "ORA ORA ORA"
        |  damage: 110
        |  range: CLOSE
        |}
        |power_level: 500
        |speed: 550
        |""".trimMargin(), suspendingCallResponse.toString())

        assertEquals("""name: "The World"
        |attacks {
        |  name: "ZA WARUDO"
        |  range: MEDIUM
        |}
        |attacks {
        |  name: "MUDA MUDA MUDA"
        |  damage: 100
        |  range: CLOSE
        |}
        |power_level: 490
        |speed: 550
        |""".trimMargin(),deferredCallResponse.await().toString())
    }

    @Test fun testSuspendingUnaryCall() = runBlocking {
        grpcServerRule.serviceRegistry.addService(TestStandService())

        val stub = StandServiceGrpc.newStub(grpcServerRule.channel)

        val request = StandServiceProtoBuilders.GetStandByNameRequest { name = "Star Platinum" }

        val stand: StandProto.Stand = stub.suspendingUnaryCallObserver { observer ->
            getStandByName(request,observer)
        }

        assertEquals("""name: "Star Platinum"
        |attacks {
        |  name: "ZA WARUDO"
        |  range: MEDIUM
        |}
        |attacks {
        |  name: "ORA ORA ORA"
        |  damage: 110
        |  range: CLOSE
        |}
        |power_level: 500
        |speed: 550
        |""".trimMargin(), stand.toString())
    }

    @Test fun `Test Response Stream Observer Channel`() = runBlocking {
        grpcServerRule.serviceRegistry.addService(TestStandService())

        val stub = StandServiceGrpc.newStub(grpcServerRule.channel)

        val respChannel = stub.getAllStandsStream()

        for((_,expected) in stands){
            assertEquals(expected.toString(), respChannel.receive().toString())
        }

        assertNull(respChannel.receiveOrNull(),"Response quantity was greater than expected")
    }

    @Test fun `Test BiDirectional Rpc Channel`() = runBlocking {
        grpcServerRule.serviceRegistry.addService(TestStandService())

        val stub = StandServiceGrpc.newStub(grpcServerRule.channel)

        val rpcChannel = stub.getStandsForCharacters()

        rpcChannel.send(characters["Dio Brando"]!!)
        stands["The World"].toString().let {
            assertEquals(it,rpcChannel.receive().toString())
            assertEquals(it,rpcChannel.receive().toString())
            assertEquals(it,rpcChannel.receive().toString())
        }

        rpcChannel.send(characters["Jotaro Kujo"]!!)
        stands["Star Platinum"].toString().let {
            assertEquals(it,rpcChannel.receive().toString())
            assertEquals(it,rpcChannel.receive().toString())
            assertEquals(it,rpcChannel.receive().toString())
        }

        rpcChannel.close()

        assertNull(rpcChannel.receiveOrNull(),"Response quantity was greater than expected")
    }

}


class TestStandService : StandServiceGrpc.StandServiceImplBase() {

    fun getStandByName(name:String): StandProto.Stand? =
            when(name){
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

        return object : StreamObserver<CharacterProto.Character>{
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
        for((_,stand) in stands)
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

fun StandServiceGrpc.StandServiceStub.getAllStandsStream(): InboundStreamChannel<StandProto.Stand> =
        InboundStreamChannel<StandProto.Stand>().also { observer ->
            getAllStandsStream(Empty.getDefaultInstance(),observer)
        }

fun StandServiceGrpc.StandServiceStub.getStandsForCharacters(): RpcBidiChannel<CharacterProto.Character,StandProto.Stand> =
        this.bidiCallChannel { responseObserver ->
            getStandsForCharacters(responseObserver)
        }

suspend fun StandServiceGrpc.StandServiceStub.getStandByCharacter(request: CharacterProto.Character): StandProto.Stand =
        this.suspendingUnaryCallObserver { observer ->
            getStandByCharacter(request,observer)
        }
