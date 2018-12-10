package krotoplus.example

import io.grpc.Status
import jojo.bizarre.adventure.stand.Attack
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandServiceCoroutineGrpc
import jojo.bizarre.adventure.stand.StandServiceProto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async

class StandService : StandServiceCoroutineGrpc.StandServiceCoroutineImplBase(){

    override suspend fun getStandByName(
        request: StandServiceProto.GetStandByNameRequest,
        deferredResponse: CompletableDeferred<StandProto.Stand>
    ) {
        val asyncAttack = async {
            Attack {
                name = "Life Shot"
                damage = 120
                range = StandProto.Attack.Range.CLOSE
            }
        }

        if(request.name == "Gold Experience"){
            deferredResponse.complete {
                name = "Gold Experience"
                powerLevel = 575
                speed = 500
                addAttacks(asyncAttack.await())
            }
        }else{
            deferredResponse.completeExceptionally(Status.NOT_FOUND.asException())
        }
    }



}