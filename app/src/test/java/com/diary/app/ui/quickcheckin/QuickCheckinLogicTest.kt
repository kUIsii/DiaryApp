package com.diary.app.ui.quickcheckin

import com.diary.app.data.QuickCheckin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCheckinLogicTest {

    @Test
    fun `submit button enables when photo is attached even if text is empty`() {
        assertTrue(shouldEnableQuickCheckinSubmit(selectedMood = null, text = "", photoUri = "file://photo.jpg"))
        assertFalse(shouldEnableQuickCheckinSubmit(selectedMood = null, text = "   ", photoUri = null))
    }

    @Test
    fun `history summary surfaces recent count and top mood`() {
        val summary = buildQuickCheckinHistorySummary(
            listOf(
                QuickCheckin(id = 1, moodLevel = 4, text = "A", createdAt = 1),
                QuickCheckin(id = 2, moodLevel = 4, text = "B", createdAt = 2),
                QuickCheckin(id = 3, moodLevel = 3, text = "C", createdAt = 3)
            )
        )

        assertEquals("最近 3 条 · 最常见心情 开心", summary)
    }
}
