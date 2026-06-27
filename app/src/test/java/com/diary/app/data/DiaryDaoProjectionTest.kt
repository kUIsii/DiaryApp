package com.diary.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiaryDaoProjectionTest {

    @Test
    fun `safe entry projections keep writing duration field for round trip integrity`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDao.kt").readText()

        assertTrue(source.contains("writing_duration_seconds"))
        assertTrue(
            source.contains(
                "plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt, writing_duration_seconds"
            )
        )
    }

    @Test
    fun `preview projections continue avoiding heavy content column`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDao.kt").readText()

        assertTrue(source.contains("fun getAllPreviews(): Flow<List<DiaryPreview>>"))
        assertTrue(source.contains("suspend fun getAllPreviewsOnce(): List<DiaryPreview>"))
        assertFalse(
            source.contains(
                "SELECT id, title, content, plainText, moodLevel, weather, location, latitude, longitude, isFavorite, createdAt, updatedAt FROM diary_entries"
            )
        )
    }

    @Test
    fun `backup payload still omits unstable writing duration export field intentionally`() {
        val importer = File("src/main/java/com/diary/app/data/DiaryImporter.kt").readText()
        val backupManager = File("src/main/java/com/diary/app/data/BackupManager.kt").readText()

        assertFalse(importer.contains("writingDurationSeconds"))
        assertFalse(backupManager.contains("writingDurationSeconds ="))
    }
}
