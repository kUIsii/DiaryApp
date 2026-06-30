package com.diary.app.ui.writingcenter

import com.diary.app.data.DiaryPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingGrowthCenterPreviewTest {

    @Test
    fun `preview builder prefers recent title and uses preview fallback text`() {
        val previews = listOf(
            preview(id = 1, title = "昨日日记", plainText = "今天去了河边散步"),
            preview(id = 2, title = "", plainText = "   ")
        )

        val model = buildWritingGrowthCenterFromPreviews(previews, hasAiSupport = true)

        assertEquals("昨日日记", model.sections.first { it.title == "今日起点" }.items.first().title)
        assertTrue(model.sections.first { it.title == "最近沉淀" }.items.isNotEmpty())
    }

    private fun preview(id: Long, title: String, plainText: String) = DiaryPreview(
        id = id,
        title = title,
        plainText = plainText,
        moodLevel = null,
        weather = null,
        location = null,
        latitude = null,
        longitude = null,
        isFavorite = false,
        createdAt = 1_000L * id,
        updatedAt = 1_000L * id
    )
}
