package krotoplus.example

import com.github.mferrer.krotoplus.test.ServiceBindingServerRule
import io.grpc.ManagedChannel
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.CharacterProtoBuilders
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.character.addMessage
import jojo.bizarre.adventure.stand.MockStandService
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandProtoBuilders
import jojo.bizarre.adventure.stand.addMessage
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class TestSomeRandomClass{

    @[Rule JvmField]
    var grpcServerRule = ServiceBindingServerRule(MockStandService(), MockCharacterService())
            .directExecutor()!!

    @Test fun `Test Finding Strongest Attack`() = runBlocking {

        MockCharacterService.getAllCharactersUnaryResponseQueue.apply {
            addMessage{
                addCharacter(characters["Jotaro Kujo"])
                addCharacter(CharacterProtoBuilders.Character {
                    name = "Dio Brando"
                    affiliation = CharacterProto.Character.Affiliation.EVIL
                })
            }
        }

        MockStandService.getStandByCharacterResponseQueue.apply {

            addMessage(stands["Star Platinum"]!!)

            addMessage {
                name = "The World"
                addAttacks(attacks["ZA WARUDO"])
                addAttacks(StandProtoBuilders.Attack {
                    name = "THE WORLD OVER HEAVEN"
                    damage = 900
                    range = StandProto.Attack.Range.CLOSE
                })
            }
        }

        val someRandomClass = SomeRandomClass(grpcServerRule.channel)

        someRandomClass.findStrongestAttackUnary().let{ attack ->
            assertEquals("THE WORLD OVER HEAVEN",attack.name)
            assertEquals(900,attack.damage)
        }
    }

}