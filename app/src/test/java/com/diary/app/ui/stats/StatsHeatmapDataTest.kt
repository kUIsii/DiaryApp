package com.diary.app.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsHeatmapDataTest {

    @Test
    fun `heatmap data preserves multiple entries on the same day`() {
        val today = LocalDate.of(2026, 6, 12)
        val yesterday = today.minusDays(1)

        val data = buildHeatmapData(
            entryDates = listOf(yesterday, yesterday, today),
            now = today,
            days = 2
        )

        assertEquals(yesterday, data[0].date)
        assertEquals(2, data[0].count)
        assertEquals(today, data[1].date)
        assertEquals(1, data[1].count)
    }
}
