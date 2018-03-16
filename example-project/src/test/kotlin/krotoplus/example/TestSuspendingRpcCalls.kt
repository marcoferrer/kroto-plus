package krotoplus.example

import com.github.mferrer.krotoplus.test.ServiceBindingServerRule
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.stand.MockStandService
import jojo.bizarre.adventure.stand.StandServiceGrpc
import jojo.bizarre.adventure.stand.getStandByCharacter
import jojo.bizarre.adventure.stand.getStandByCharacterName
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class TestSuspendingRpcCalls{

    @[Rule JvmField]
    var grpcServerRule = ServiceBindingServerRule(MockStandService(), MockCharacterService())
            .directExecutor()!!

    @Test fun `Test Suspending Unary Rpc Calls`() = runBlocking {

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



}