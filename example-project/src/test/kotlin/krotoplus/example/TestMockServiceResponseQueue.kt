package krotoplus.example

import com.github.marcoferrer.krotoplus.message.*
import com.github.marcoferrer.krotoplus.test.QueueMessage
import com.github.marcoferrer.krotoplus.test.ResponseQueue
import com.github.marcoferrer.krotoplus.test.ServiceBindingServerRule
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jojo.bizarre.adventure.character.MockCharacterService
import jojo.bizarre.adventure.stand.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TestMockServiceResponseQueue {

    inline fun <reified M,B> ResponseQueue<M>.addMessage2(block:B.()->Unit): Boolean
        where M : KpMessage<M, B>, B : KpBuilder<M> {

        val builder = KpCompanion.Registry[M::class.java].newBuilder()

        return builder.apply(block).build().let{ add(QueueMessage(it)) }
    }

    @[Rule JvmField]
    var grpcServerRule = ServiceBindingServerRule(MockStandService(),MockCharacterService())
            .directExecutor()!!

    val metadataTestKey = Metadata.Key.of("test_header", Metadata.ASCII_STRING_MARSHALLER)

    @Test fun `Test Unary Response Queue`(){

        MockStandService.getStandByNameResponseQueue.apply {

            //Queue up a valid response message
            addMessage2 {
                name = "Star Platinum"
                powerLevel = 500
                speed = 550
                addAttacks(StandProtoBuilders.Attack {
                    name = "ORA ORA ORA"
                    damage = 100
                    range = StandProto.Attack.Range.CLOSE
                })
            }


            //Queue up an error
            addError(Status.INVALID_ARGUMENT)

            //Queue up a valid response message
            addMessage2 {
                name = "The World"
                powerLevel = 490
                speed = 550
                addAttacks(StandProtoBuilders.Attack {
                    name = "ZA WARUDO"
                    damage = 0
                    range = StandProto.Attack.Range.MEDIUM
                })
            }

            //Queue up another error but this time with metadata
            addError(Status.PERMISSION_DENIED, Metadata().apply {
                put(metadataTestKey,"some_metadata_value")
            })
        }

        val standStub = StandServiceGrpc.newBlockingStub(grpcServerRule.channel)

        standStub.getStandByName { name = "Star Platinum" }.let{ response ->
            assertEquals("Star Platinum",response.name)
            assertEquals(500,response.powerLevel)
            assertEquals(550,response.speed)
            response.attacksList.first().let{ attack ->
                assertEquals("ORA ORA ORA",attack.name)
                assertEquals(100,attack.damage)
                assertEquals(StandProto.Attack.Range.CLOSE,attack.range)
            }
        }

        try{
            standStub.getStandByName { name = "The World" }
            fail("Exception was expected with status code: ${Status.INVALID_ARGUMENT.code}")
        }catch (e: StatusRuntimeException){
            assertEquals(Status.INVALID_ARGUMENT.code, e.status.code)
        }

        standStub.getStandByName { name = "The World" }.let{ response ->
            assertEquals("The World",response.name)
            assertEquals(490,response.powerLevel)
            assertEquals(550,response.speed)
            response.attacksList.first().let{ attack ->
                assertEquals("ZA WARUDO",attack.name)
                assertEquals(0,attack.damage)
                assertEquals(StandProto.Attack.Range.MEDIUM,attack.range)
            }
        }

        try{
            standStub.getStandByName { name = "The World" }
            fail("Exception was expected with status code: ${Status.INVALID_ARGUMENT.code}")
        }catch (e: StatusRuntimeException){
            assertEquals(Status.PERMISSION_DENIED.code, e.status.code)
            assertEquals("some_metadata_value",e.trailers.get(metadataTestKey))
        }

        //Test default instance fall back when the response queue is empty
        assertEquals(StandProto.Stand.getDefaultInstance(),standStub.getStandByName { name = "Anything" })
    }


}