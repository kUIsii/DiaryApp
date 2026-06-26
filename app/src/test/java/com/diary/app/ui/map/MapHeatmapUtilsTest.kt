package com.diary.app.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapHeatmapUtilsTest {

    @Test
    fun `build heatmap spots clusters nearby markers and sorts by density`() {
        val spots = buildHeatmapSpots(
            listOf(
                marker(1, 31.2304, 121.4737),
                marker(2, 31.23045, 121.47375),
                marker(3, 39.9042, 116.4074)
            )
        )

        assertEquals(2, spots.size)
        assertEquals(2, spots.first().count)
        assertTrue(spots.first().radiusMeters > spots.last().radiusMeters)
        assertTrue(spots.first().alpha > spots.last().alpha)
    }

    @Test
    fun `heatmap color grows denser as count increases`() {
        val lowDensity = heatmapSpotColorArgb(count = 1, maxCount = 5)
        val highDensity = heatmapSpotColorArgb(count = 5, maxCount = 5)

        assertTrue((highDensity and 0xFF) < (lowDensity and 0xFF))
    }

    private fun marker(id: Long, latitude: Double, longitude: Double) = MapMarker(
        id = id,
        title = "marker-$id",
        latitude = latitude,
        longitude = longitude,
        location = "位置$id",
        createdAt = id,
        moodLevel = null
    )
}
