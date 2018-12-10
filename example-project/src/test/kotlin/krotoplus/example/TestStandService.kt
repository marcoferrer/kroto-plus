package krotoplus.example

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.testing.GrpcServerRule
import jojo.bizarre.adventure.stand.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.assertNotNull
import kotlin.test.fail

class TestStandService {

    @[Rule JvmField]
    var grpcServerRule = GrpcServerRule().directExecutor()

    @BeforeTest
    fun bindService() {
        grpcServerRule?.serviceRegistry?.addService(StandService())
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

        try{
            standStub.getStandByName { name = "Silver Chariot" }
            fail("Exception was expected with status code: ${Status.NOT_FOUND.code}")
        } catch (e: Throwable) {
            val exception = e as? StatusRuntimeException
            assertNotNull(exception)
            assertEquals(Status.NOT_FOUND.code, exception.status.code)
        }
    }

}