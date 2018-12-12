package com.github.marcoferrer.krotoplus.utils

class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x) { f(x) }
    }
}

class MemoizeExt1<in T, out R>(val f: T.() -> R){
    private val values = mutableMapOf<T, R>()
    operator fun T.invoke(): R {
        return values.getOrPut(this) { this.f() }
    }
}


fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)

fun <T, R> (T.() -> R).memoizeExt(): MemoizeExt1<T,R> = MemoizeExt1(this)