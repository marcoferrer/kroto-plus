package krotoplus.example

import io.grpc.ManagedChannel
import jojo.bizarre.adventure.character.CharacterServiceGrpc
import jojo.bizarre.adventure.character.getAllCharactersStream
import jojo.bizarre.adventure.character.getAllCharactersUnary
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandServiceGrpc
import jojo.bizarre.adventure.stand.getStandByCharacter
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.coroutineScope


class SomeRandomClass(val managedChannel: ManagedChannel){

    suspend fun findStrongestAttack(): StandProto.Attack = coroutineScope {

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

        strongestAttack ?: StandProto.Attack.getDefaultInstance()
    }

    suspend fun findStrongestAttackUnary(): StandProto.Attack = coroutineScope {

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

        strongestAttack ?: StandProto.Attack.getDefaultInstance()
    }

}

