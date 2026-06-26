package com.diary.app.ui.achievement

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementDetailSourceTest {

    @Test
    fun `achievement detail uses themed artwork instead of raw drawable rendering`() {
        val source = File("src/main/java/com/diary/app/ui/achievement/AchievementDetailScreen.kt").readText()

        assertTrue(source.contains("AchievementArtwork("))
        assertFalse(source.contains("painterResource(id = imageRes)"))
    }
}
