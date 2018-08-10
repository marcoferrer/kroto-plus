package krotoplus.example

import jojo.bizarre.adventure.character.Character
import jojo.bizarre.adventure.character.CharacterProto
import jojo.bizarre.adventure.character.stand
import jojo.bizarre.adventure.stand.Attack
import jojo.bizarre.adventure.stand.Stand
import jojo.bizarre.adventure.stand.StandProto
import jojo.bizarre.adventure.stand.addAttacks

val attacks: Map<String, StandProto.Attack> =
        listOf(
            Attack {
                name = "ZA WARUDO"
                damage = 0
                range = StandProto.Attack.Range.MEDIUM
            },
            Attack {
                name = "ORA ORA ORA"
                damage = 110
                range = StandProto.Attack.Range.CLOSE
            },
            Attack {
                name = "MUDA MUDA MUDA"
                damage = 100
                range = StandProto.Attack.Range.CLOSE
            },
            Attack {
                name = "EMERALD SPLASH"
                damage = 70
                range = StandProto.Attack.Range.MEDIUM
            }
        ).associateBy { it.name }


val stands: Map<String, StandProto.Stand> =
        listOf(
            Stand {
                name = "Star Platinum"
                powerLevel = 500
                speed = 550
                addAttacks(attacks["ZA WARUDO"])
                addAttacks(attacks["ORA ORA ORA"])
            },
            Stand {
                name = "The World"
                powerLevel = 490
                speed = 550
                addAttacks(attacks["ZA WARUDO"])
                addAttacks(attacks["MUDA MUDA MUDA"])
            }
        ).associateBy { it.name }


val characters: Map<String, CharacterProto.Character> =
        listOf(
            Character {
                name = "Jotaro Kujo"
                affiliation = CharacterProto.Character.Affiliation.GOOD
                stand = stands["Star Platinum"]
            },
            Character {
                name = "Dio Brando"
                affiliation = CharacterProto.Character.Affiliation.EVIL
                stand = stands["The World"]
            }
        ).associateBy { it.name }

val dioBrandoCharacter = Character {
    name = "Dio Brando"
    affiliation = CharacterProto.Character.Affiliation.EVIL
    stand {
        name = "The World"
        powerLevel = 490
        speed = 550
        addAttacks {
            name = "ZA WARUDO"
            damage = 0
            range = StandProto.Attack.Range.MEDIUM
        }
        addAttacks {
            name = "MUDA MUDA MUDA"
            damage = 100
            range = StandProto.Attack.Range.CLOSE
        }
    }
}