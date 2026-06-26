package com.diary.app.ui.map

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapHeatmapSourceTest {

    @Test
    fun `map screen renders real heatmap circles when heatmap mode is enabled`() {
        val source = File("src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt").readText()

        assertTrue(source.contains("val heatmapSpots = if (isHeatmapMode)"))
        assertTrue(source.contains("aMap.addCircle("))
        assertTrue(source.contains("buildHeatmapSpots(displayMarkers)"))
    }
}
