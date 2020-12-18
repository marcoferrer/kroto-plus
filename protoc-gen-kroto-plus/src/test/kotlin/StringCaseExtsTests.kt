package com.github.marcoferrer.krotoplus.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringCaseExtsTests {
    @Test
    fun `toUpperCamelCase capitalizes first character`() {
        assertEquals("SomethingAwesome", "somethingAwesome".toUpperCamelCase())
        assertEquals("Test", "test".toUpperCamelCase())
    }

    @Test
    fun `toUpperCamelCase uppercases character after underscore`() {
        assertEquals("ThisIsAnAwesomeText", "this_is_an_awesome_text".toUpperCamelCase())
        assertEquals("SomeExample", "some_example".toUpperCamelCase())
    }

    @Test
    fun `toUpperCamelCase uppercases character after number`() {
        assertEquals("SomeMethod4Me", "some_method4me".toUpperCamelCase())
        assertEquals("SomeB2BCase", "some_b2b_case".toUpperCamelCase())
        assertEquals("Fun4Devs", "fun4devs".toUpperCamelCase())
    }
}