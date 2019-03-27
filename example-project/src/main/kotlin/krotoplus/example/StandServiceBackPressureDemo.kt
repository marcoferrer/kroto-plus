package krotoplus.example

import io.grpc.Status
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.stand.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.CoroutineContext

class StandServiceBackPressureDemo : StandServiceCoroutineGrpc.StandServiceImplBase() {

    private val myThreadLocal = ThreadLocal.withInitial { "value" }.asContextElement()

    override val initialContext: CoroutineContext
        get() = Dispatchers.Unconfined + myThreadLocal

    override suspend fun getStandByName(
        request: StandServiceProto.GetStandByNameRequest
    ): StandProto.Stand = coroutineScope {
        val asyncAttack = async {
            Attack {
                name = "Life Shot"
                damage = 120
                range = StandProto.Attack.Range.CLOSE
            }
        }

        if (request.name == "Gold Experience") {
            Stand {
                name = "Gold Experience"
                powerLevel = 575
                speed = 500
                addAttacks(asyncAttack.await())
            }
        } else {
            throw Status.NOT_FOUND.asException()
        }
    }

    @ExperimentalCoroutinesApi
    override suspend fun getStandsForCharacters(
        requestChannel: ReceiveChannel<CharacterProto.Character>,
        responseChannel: SendChannel<StandProto.Stand>
    ) {
        val requestIter = requestChannel.iterator()

        while (requestIter.hasNext()) {

            val requestValues = listOf(
                requestIter.next(),
                requestIter.next(),
                requestIter.next()
            )

            responseChannel.send {
                name = requestValues.joinToString()
            }
        }

        println("Server Finished")
    }

}