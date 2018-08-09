package com.github.marcoferrer.krotoplus.utils

class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x) { f(x) }
    }
}

fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)