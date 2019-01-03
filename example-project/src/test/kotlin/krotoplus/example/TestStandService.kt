package krotoplus.example

import com.github.marcoferrer.krotoplus.coroutines.*
import io.grpc.*
import io.grpc.testing.GrpcServerRule
import jojo.bizarre.adventure.stand.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEachIndexed


class TestStandService {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun bindService() {
        grpcServerRule.serviceRegistry?.addService(StandService())
    }

    @Test
    fun `Test Unary Service Response`() = runBlocking {
        val standStub = StandServiceGrpc.newStub(grpcServerRule.channel)

        standStub.getStandByName { name = "Gold Experience" }.let { response ->

            assertEquals("Gold Experience", response.name)
            assertEquals(575, response.powerLevel)
            assertEquals(500, response.speed)
            response.attacksList.first().let { attack ->
                assertEquals("Life Shot", attack.name)
                assertEquals(120, attack.damage)
                assertEquals(StandProto.Attack.Range.CLOSE, attack.range)
            }
        }

        try {
            standStub.getStandByName { name = "Silver Chariot" }
            fail("Exception was expected with status code: ${Status.NOT_FOUND.code}")
        } catch (e: Throwable) {
            val exception = e as? StatusRuntimeException
            assertNotNull(exception)
            assertEquals(Status.NOT_FOUND.code, exception.status.code)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `Test Bidi Service Call`() {
        runBlocking {

            val standStub = StandServiceCoroutineGrpc
                .newStub(grpcServerRule.channel!!)
                .withCoroutineContext()

            val (requestChannel, responseChannel) = standStub.getStandsForCharacters()

            launchProducerJob(requestChannel) {
                repeat(300) {
                    val value = it + 1
                    send { name = "test $value" }
                    println("-> Client Sent '$value'")
                    delay(2L)
                }
            }

            launch(Dispatchers.Default) {
                var responseQty = 0

                responseChannel.consumeEachIndexed { (index, response) ->
                    responseQty = index
                    println("<- Resp#: $index, Client Received '${response.toString().trim()}' ")
                }

                assertEquals(99, responseQty)
            }
        }
    }

}