package com.diary.app.ui.home

import com.diary.app.data.DiaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDeleteMapperTest {

    @Test
    fun `toTrashEntry preserves key diary fields`() {
        val entry = DiaryEntry(
            id = 10L,
            title = "A",
            content = "content",
            plainText = "plain",
            moodLevel = 3,
            weather = "sunny",
            location = "here",
            latitude = 1.2,
            longitude = 3.4,
            isFavorite = true,
            createdAt = 1L,
            updatedAt = 2L
        )

        val trashEntry = toTrashEntry(entry)

        assertEquals(10L, trashEntry.originalId)
        assertEquals("A", trashEntry.title)
        assertEquals("plain", trashEntry.plainText)
        assertTrue(trashEntry.isFavorite)
        assertEquals(1L, trashEntry.createdAt)
        assertEquals(2L, trashEntry.updatedAt)
    }
}
