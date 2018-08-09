package krotoplus.example

import com.github.marcoferrer.krotoplus.message.KpMessage
import com.google.protobuf.DynamicMessage
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.CharacterProtoBuilders
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.StandProtoBuilders

val attacks: Map<String, StandProto.Attack> =
        listOf(
            StandProtoBuilders.Attack {
                name = "ZA WARUDO"
                damage = 0
                range = StandProto.Attack.Range.MEDIUM
            },
            StandProtoBuilders.Attack {
                name = "ORA ORA ORA"
                damage = 110
                range = StandProto.Attack.Range.CLOSE
            },
            StandProtoBuilders.Attack {
                name = "MUDA MUDA MUDA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            },
            StandProtoBuilders.Attack {
                name = "EMERALD SPLASH"
                damage = 70
                range = StandProto.Attack.Range.MEDIUM
            }
        ).associateBy { it.name }


val stands: Map<String, StandProto.Stand> =
        listOf(
            StandProtoBuilders.Stand {
                name = "Star Platinum"
                powerLevel = 500
                speed = 550
                addAttacks(attacks["ZA WARUDO"])
                addAttacks(attacks["ORA ORA ORA"])
            },
            StandProtoBuilders.Stand {
                name = "The World"
                powerLevel = 490
                speed = 550
                addAttacks(attacks["ZA WARUDO"])
                addAttacks(attacks["MUDA MUDA MUDA"])
            }
        ).associateBy { it.name }


val characters: Map<String, CharacterProto.Character> =
        listOf(
                Character{

                }
                CharacterProto.Character.build {

                }
            CharacterProtoBuilders.Character {
                name = "Jotaro Kujo"
                affiliation = CharacterProto.Character.Affiliation.GOOD
                stand = stands["Star Platinum"]
            },
            CharacterProtoBuilders.Character {
                name = "Dio Brando"
                affiliation = CharacterProto.Character.Affiliation.EVIL
                stand = stands["The World"]
            }
        ).associateBy { it.name }


public inline fun <T> T.apply(block: T.() -> Unit): T {
    block()

    jojo.bizarre.adventure.character.Character


    return this
}