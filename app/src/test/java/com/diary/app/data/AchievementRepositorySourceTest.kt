package com.diary.app.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class AchievementRepositorySourceTest {

    @Test
    fun `achievement unlock no longer creates milestone diary entries`() {
        val source = File("src/main/java/com/diary/app/data/AchievementRepository.kt").readText()

        assertFalse(source.contains("createMilestoneDiary("))
        assertFalse(source.contains("里程碑: \${def.name}"))
    }
}
