package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCalendarSummaryTest {

    @Test
    fun `calendar summary tracks dominant mood and mixed mood accent`() {
        val summary = buildCalendarMoodSummary(
            moodLevels = listOf(3, 5, 5, 2, 3, 6)
        )

        assertEquals(5, summary.primaryMoodLevel)
        assertEquals(3, summary.accentMoodLevel)
        assertEquals(true, summary.hasMixedMoods)
        assertEquals(6, summary.entryCount)
    }

    @Test
    fun `calendar summary preserves entry count and handles moodless days`() {
        val summary = buildCalendarMoodSummary(
            moodLevels = emptyList(),
            entryCount = 4
        )

        assertEquals(4, summary.entryCount)
        assertEquals(null, summary.primaryMoodLevel)
        assertEquals(null, summary.accentMoodLevel)
        assertEquals(false, summary.hasMixedMoods)
    }
}
