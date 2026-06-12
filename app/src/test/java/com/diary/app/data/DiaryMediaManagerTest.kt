package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryMediaManagerTest {

    @Test
    fun `content appasset image urls are normalized to stable media refs`() {
        val content = """{"ops":[{"insert":{"image":"https://appassets/diary_media/img_1.jpg"}}]}"""

        val normalized = DiaryMediaManager.contentToStableMediaRefs(content)

        assertEquals("""{"ops":[{"insert":{"image":"diary-media://img_1.jpg"}}]}""", normalized)
    }

    @Test
    fun `extract media names supports stable refs and legacy urls`() {
        val content = """
            {"ops":[
              {"insert":{"image":"diary-media://img_1.jpg"}},
              {"insert":{"image":"https://appassets/diary_media/img_2.jpg"}}
            ]}
        """.trimIndent()

        assertEquals(listOf("img_1.jpg", "img_2.jpg"), DiaryMediaManager.extractMediaNames(content))
    }

    @Test
    fun `image sampling lowers very large images before decode`() {
        assertEquals(1, DiaryMediaManager.calculateImageSampleSize(1600, 900, 1600))
        assertEquals(2, DiaryMediaManager.calculateImageSampleSize(4000, 3000, 1600))
        assertEquals(4, DiaryMediaManager.calculateImageSampleSize(9000, 6000, 1600))
    }
}
