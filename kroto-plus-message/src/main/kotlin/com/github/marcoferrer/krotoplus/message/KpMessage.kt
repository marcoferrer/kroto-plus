package com.github.marcoferrer.krotoplus.message

import com.google.protobuf.Empty
import com.google.protobuf.EmptyProto
import com.google.protobuf.Message
import java.util.concurrent.ConcurrentHashMap

//
//interface KpM : com.google.protobuf.Message {
//
//
//}
//
//interface KpMComp<out T> where T: KpM, T: KpBuildable<KpMBuilder<T>> {
//
//    val defaultInstance: T
//
//    fun newBuilder() = defaultInstance.toBuilder()
//
//}
//
//interface KpBuildable<out T: KpMBuilder<KpM>>{
//
//    fun toBuilder(): T
//}
//interface KpMBuilder<out T : KpM> : com.google.protobuf.Message.Builder{
//    override fun build(): T
//}



interface KpMessage<out M,out B> : com.google.protobuf.Message
        where M : KpMessage<M,B>, B :KpBuilder<M>, B : com.google.protobuf.Message.Builder {

    val companion: KpCompanion<M,B>

}

interface KpBuilder<out M> : com.google.protobuf.Message.Builder
        where M : KpMessage<M,KpBuilder<M>> {

    override fun build(): M

}

interface KpCompanion<out M, out B>
        where M : KpMessage<M,B>, B: KpBuilder<M> {

    val defaultInstance: M

    fun newBuilder(): B

    @Suppress("UNCHECKED_CAST")
    companion object Registry {

        private val registeredCompanions = ConcurrentHashMap<Class<*>, KpCompanion<*,*>>()

        operator fun <M,B,C> get(clazz:Class<M>): C
                where M : KpMessage<M, B>, B : KpBuilder<M>, C : KpCompanion<M, B> =
                registeredCompanions.getOrPut(clazz){ getDefaultInstance(clazz).companion } as C

        fun <M, B, C> initializeCompanion(clazz: Class<M>, kpCompanion: C): C
                where M : KpMessage<M, B>, B : KpBuilder<M>, C : KpCompanion<M, B> =
                registeredCompanions.getOrPut(clazz) { kpCompanion } as C

        private fun <M : KpMessage<*,*>> getDefaultInstance(clazz:Class<M>): M =
                clazz.getMethod("getDefaultInstance").invoke(null) as M
    }
}

/**
 * Companion Extensions
 */

fun <M, B> KpCompanion<M, B>.build(block: B.() -> Unit): M
        where B : KpBuilder<M>,M : KpMessage<M,B> {

    return newBuilder().apply(block).build()
}




