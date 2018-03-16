package krotoplus.example

import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.CharacterServiceGrpc
import jojo.bizarre.adventure.stand.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

//
//fun main(args: Array<String>){
//    runBlocking {
//        val rule = GrpcServerRule
//        val channel = InProcessChannelBuilder.forName("example").directExecutor().build()
//
//        val starPlatinum = StandProtoBuilders.Stand {
//            name = "Star Platinum"
//        }
//
//        val standServiceStub = StandServiceGrpc.newStub(channel)
//        val characterServiceStub = CharacterServiceGrpc.newStub(channel)
//
//        launch {
//            val suspendingResponse = standServiceStub.getStandByCharacter {
//                name = "Jotaro Kujo"
//                affiliation = CharacterProto.Character.Affiliation.GOOD
//            }
//
//            val deferredResponse = async {
//                standServiceStub.getStandByCharacterName { name = "Dio Brando" }
//            }
//
//            println(deferredResponse.await().name) //Stand: The World
//        }.join()
//    }
//}