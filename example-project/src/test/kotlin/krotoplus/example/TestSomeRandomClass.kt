package krotoplus.example

import com.github.marcoferrer.krotoplus.test.MockServiceHelper
import com.github.marcoferrer.krotoplus.test.addServices
import io.grpc.testing.GrpcServerRule
import jojo.bizarre.adventure.MockJojoServices
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.character.addCharacter
import jojo.bizarre.adventure.character.addMessage
import jojo.bizarre.adventure.stand.MockStandService
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.addAttacks
import jojo.bizarre.adventure.stand.addMessage
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class TestSomeRandomClass {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun bindMockServices() {
        grpcServerRule?.serviceRegistry?.addServices(MockJojoServices)
    }

    @BeforeTest
    fun clearResponseQueues() = MockJojoServices
        .forEach { (it as? MockServiceHelper)?.clearQueues() }

    @Test
    fun `Test Finding Strongest Attack`() = runBlocking {

        MockCharacterService.getAllCharactersUnaryResponseQueue.apply {
            addMessage {
                addCharacter(characters["Jotaro Kujo"])
                addCharacter {
                    name = "Dio Brando"
                    affiliation = CharacterProto.Character.Affiliation.EVIL
                }
            }
        }

        MockStandService.getStandByCharacterResponseQueue.apply {

            addMessage(stands.getValue("Star Platinum"))

            addMessage {
                name = "The World"
                addAttacks(attacks["ZA WARUDO"])

                addAttacks {
                    name = "THE WORLD OVER HEAVEN"
                    damage = 900
                    range = StandProto.Attack.Range.CLOSE
                }
            }
        }

        val someRandomClass = SomeRandomClass(grpcServerRule.channel)

        someRandomClass.findStrongestAttackUnary().let { attack ->
            assertEquals("THE WORLD OVER HEAVEN", attack.name)
            assertEquals(900, attack.damage)
        }
    }

}