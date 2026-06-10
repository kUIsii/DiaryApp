package com.diary.app.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiaryDaoSourceTest {

    @Test
    fun `safe dao queries downgrade oversized inline image content before reading full entries`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDao.kt").readText()
        val exporterSource = File("src/main/java/com/diary/app/data/DiaryExporter.kt").readText()

        assertTrue(source.contains("suspend fun getEntryByIdSafe(id: Long): DiaryEntry?"))
        assertTrue(source.contains("suspend fun getEntriesBatchForExport(offset: Int, limit: Int): List<DiaryEntry>"))
        assertTrue(source.contains("CASE WHEN instr(content, 'data:image/') > 0 AND length(content) > 262144 THEN '' ELSE content END AS content"))
        assertTrue(exporterSource.contains("dao.getEntriesBatchForExport(offset, batchSize)"))
    }
}
