package krotoplus.example

import com.github.marcoferrer.krotoplus.coroutines.suspendingUnaryCallObserver
import io.grpc.ManagedChannel
import io.grpc.testing.GrpcServerRule
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.stand.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestSuspendingRpcCalls{

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun bindMockServices() {
        grpcServerRule?.serviceRegistry?.apply {
            addService(DummyStandService())
            addService(MockCharacterService)
        }
    }

    val channel: ManagedChannel
        get() = grpcServerRule.channel

    @Test fun `Test Suspending Unary Rpc Calls`() = runBlocking {

        val standServiceStub = StandServiceGrpc.newStub(channel)

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

    @Test fun `Test Suspending Unary Call`() = runBlocking {

        val stub = StandServiceGrpc.newStub(channel)

        val request = GetStandByNameRequest { name = "Star Platinum" }

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

        val stub = StandServiceGrpc.newStub(channel)

        val respChannel = stub.getAllStandsStream()

        for((_,expected) in stands){
            assertEquals(expected.toString(), respChannel.receive().toString())
        }

        assertNull(respChannel.receiveOrNull(),"Response quantity was greater than expected")
    }

    @Test fun `Test Bidirectional Rpc Channel`() = runBlocking {

        val stub = StandServiceGrpc.newStub(grpcServerRule.channel)

        val rpcChannel = stub.getStandsForCharacters()

        //Our dummy service is sending three responses for each request it receives

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



