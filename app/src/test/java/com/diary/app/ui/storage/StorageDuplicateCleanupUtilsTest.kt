package com.diary.app.ui.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StorageDuplicateCleanupUtilsTest {

    @Test
    fun `duplicate cleanup removes every file except first file of each group`() {
        val a = File("a.jpg")
        val b = File("b.jpg")
        val c = File("c.jpg")
        val d = File("d.jpg")
        val e = File("e.jpg")

        val removals = duplicateFilesToRemove(
            listOf(
                listOf(a, b, c),
                listOf(d),
                listOf(e, b)
            )
        )

        assertEquals(listOf(b, c), removals)
    }
}
