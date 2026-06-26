package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BackupMediaIndexUtilsTest {

    @Test
    fun `media index rebuild extracts referenced media names in content order`() {
        val mediaDir = createTempDir(prefix = "media-dir")
        val thumbDir = File(mediaDir, "thumbs").apply { mkdirs() }
        File(mediaDir, "img_a.jpg").writeText("a")
        File(mediaDir, "img_b.jpg").writeText("b")
        File(thumbDir, "img_a.jpg").writeText("ta")
        File(thumbDir, "img_b.jpg").writeText("tb")

        val entry = DiaryEntry(
            id = 12,
            title = "带图日记",
            content = """<img src="diary-media://img_a.jpg"/><p>中间</p><img src="diary-media://img_b.jpg"/>""",
            plainText = "带图日记",
            moodLevel = null,
            weather = null,
            location = null,
            latitude = null,
            longitude = null,
            isFavorite = false,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val rebuilt = buildDiaryMediaIndexRows(entry, mediaDir, thumbDir)

        assertEquals(listOf("img_a.jpg", "img_b.jpg"), rebuilt.map { it.mediaName })
        assertEquals(listOf(0, 1), rebuilt.map { it.sortOrder })
        assertEquals(listOf(12L, 12L), rebuilt.map { it.entryId })
    }
}
