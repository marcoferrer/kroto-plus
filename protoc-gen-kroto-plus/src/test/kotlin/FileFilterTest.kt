package com.github.marcoferrer.krotoplus.utils

import com.github.marcoferrer.krotoplus.config.FileFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class FileFilterTest {

    val testPaths = listOf(
        "google/protobuf",
        "test/dummy/a/1",
        "test/dummy/a/2",
        "test/dummy/b/1",
        "test/dummy/b/2",
        "test/dummy/c/1",
        "test/dummy/c/2"
    )

    @Test
    fun `include all paths`(){
        val fileFilter = FileFilter.getDefaultInstance()
        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(7, matches.size)
    }

    @Test
    fun `Include single path`(){
        val fileFilter = FileFilter.newBuilder().addIncludePath("test/dummy/a/*").build()
        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(2, matches.size)
        assertEquals("test/dummy/a/1",matches[0])
        assertEquals("test/dummy/a/2",matches[1])
    }

    @Test
    fun `Include multiple paths`(){
        val fileFilter = FileFilter.newBuilder()
            .addIncludePath("test/dummy/a/*")
            .addIncludePath("google/*")
            .build()
        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(3, matches.size)
        assertEquals("google/protobuf",matches[0])
        assertEquals("test/dummy/a/1",matches[1])
        assertEquals("test/dummy/a/2",matches[2])
    }

    @Test
    fun `Exclude single path`(){
        val fileFilter = FileFilter.newBuilder()
            .addExcludePath("google/*")
            .build()

        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(6, matches.size)
        assertEquals("test/dummy/a/1",matches[0])
        assertEquals("test/dummy/a/2",matches[1])
        assertEquals("test/dummy/b/1",matches[2])
        assertEquals("test/dummy/b/2",matches[3])
        assertEquals("test/dummy/c/1",matches[4])
        assertEquals("test/dummy/c/2",matches[5])
    }

    @Test
    fun `Exclude multiple paths`(){
        val fileFilter = FileFilter.newBuilder()
            .addExcludePath("google/*")
            .addExcludePath("test/dummy/b/*")
            .build()

        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(4, matches.size)
        assertEquals("test/dummy/a/1",matches[0])
        assertEquals("test/dummy/a/2",matches[1])
        assertEquals("test/dummy/c/1",matches[2])
        assertEquals("test/dummy/c/2",matches[3])
    }

    @Test
    fun `Include and exclude paths`(){
        val fileFilter = FileFilter.newBuilder()
            .addIncludePath("test/dummy/*")
            .addExcludePath("test/dummy/*/1")
            .build()

        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(3, matches.size)
        assertEquals("test/dummy/a/2",matches[0])
        assertEquals("test/dummy/b/2",matches[1])
        assertEquals("test/dummy/c/2",matches[2])
    }

    @Test
    fun `Include and exclude multiple paths`(){
        val fileFilter = FileFilter.newBuilder()
            .addIncludePath("google/*")
            .addIncludePath("test/dummy/*/1")
            .addExcludePath("test/dummy/a/*")
            .addExcludePath("test/dummy/c/*")
            .build()

        val matches = testPaths.filter { fileFilter.matches(it) }
        assertEquals(2, matches.size)
        assertEquals("google/protobuf",matches[0])
        assertEquals("test/dummy/b/1",matches[1])
    }

}