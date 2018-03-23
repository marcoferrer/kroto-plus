package krotoplus.example

import io.grpc.ManagedChannel
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.CharacterServiceGrpc
import jojo.bizarre.adventure.character.getAllCharactersStream
import jojo.bizarre.adventure.character.getAllCharactersUnary
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandServiceGrpc
import jojo.bizarre.adventure.stand.StandServiceProto
import jojo.bizarre.adventure.stand.getStandByCharacter
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch


class SomeRandomClass(val managedChannel: ManagedChannel){

    suspend fun findStrongestAttack(): StandProto.Attack {

        val standService = StandServiceGrpc.newStub(managedChannel)
        val characterService = CharacterServiceGrpc.newStub(managedChannel)

        val deferredStands = characterService.getAllCharactersStream()
                .map { character ->
                    async { standService.getStandByCharacter(character) }
                }
                .toList()

        val strongestAttack = deferredStands
                .flatMap { it.await().attacksList }
                .maxBy { it.damage }

        return strongestAttack ?: StandProto.Attack.getDefaultInstance()
    }

    suspend fun findStrongestAttackUnary(): StandProto.Attack {

        val standService = StandServiceGrpc.newStub(managedChannel)
        val characterService = CharacterServiceGrpc.newStub(managedChannel)

        val deferredStands = characterService.getAllCharactersUnary()
                .characterList
                .map { character ->
                    async { standService.getStandByCharacter(character) }
                }

        val strongestAttack = deferredStands
                .flatMap { it.await().attacksList }
                .maxBy { it.damage }

        return strongestAttack ?: StandProto.Attack.getDefaultInstance()
    }

}

