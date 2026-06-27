package com.diary.app.startup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StartupPerformanceSourceTest {

    @Test
    fun `database builder no longer forces writableDatabase open on creation path`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDatabase.kt").readText()

        assertFalse(source.contains(".also { it.openHelper.writableDatabase }"))
        assertTrue(source.contains("Room.databaseBuilder("))
    }

    @Test
    fun `application startup does not initialize achievement database access on main thread`() {
        val source = File("src/main/java/com/diary/app/DiaryApplication.kt").readText()

        assertFalse(source.contains("val achievementDao = database.achievementDao()"))
        assertFalse(source.contains("val diaryDao = database.diaryDao()"))
        assertFalse(source.contains("AchievementRepository(achievementDao, diaryDao)"))
        assertFalse(source.contains("AchievementRepository(achievementDao, diaryDao, database.tagDao(), database.mediaDao())"))
        assertTrue(source.contains("warmUpCoreData()"))
    }

    @Test
    fun `main activity shows startup gate instead of forcing synchronous database path`() {
        val source = File("src/main/java/com/diary/app/MainActivity.kt").readText()

        assertTrue(source.contains("AppStartupState.Initializing"))
        assertTrue(source.contains("StartupLoadingScreen"))
        assertTrue(source.contains("StartupErrorScreen"))
    }
}
