package com.github.marcoferrer.krotoplus

interface KrotoMessage : com.google.protobuf.Message {

    val companion: KrotoMessage.Companion<*,*>

    interface Builder<M : KrotoMessage> : com.google.protobuf.Message.Builder {

        override fun build(): M
    }

    interface Companion<M : KrotoMessage, B : Builder<M>> {

        val defaultInstance: M

        fun newBuilder(): B
    }

}

fun <M, B> KrotoMessage.Companion<M, B>.build(block: B.() -> Unit): M
        where M : KrotoMessage, B : KrotoMessage.Builder<M> {

    return this.newBuilder().apply(block).run { build() }
}
