package krotoplus.example

import com.github.marcoferrer.krotoplus.message.KpBuilder
import com.github.marcoferrer.krotoplus.message.KpCompanion
import com.github.marcoferrer.krotoplus.message.KpMessage
import jojo.bizarre.adventure.character.Character
import jojo.bizarre.adventure.character.CharacterProtoBuilders

interface MyCompanion<T> {

    @JvmDefault
    val myField: String
        get() = "value"

    @JvmDefault
    fun doSomething(){
        println("Doing something")
    }

}

fun test(){
    Character.newBuilder()
    Character {

    }

    "sdfsdfd".apply {  }
}

//abstract class AbstractCompanion
//    : KpCompanion<KpMessage, KpBuilder<KpMessage>> {
//
//    fun doSomethingElse(){
//        println("Do something else")
//    }
//}