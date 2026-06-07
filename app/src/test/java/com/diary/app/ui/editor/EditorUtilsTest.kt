package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorUtilsTest {

    @Test
    fun `summarize selected names returns empty label when no valid names`() {
        val summary = summarizeSelectedNames(
            names = listOf("", "   "),
            emptyLabel = "标签"
        )

        assertEquals("标签", summary)
    }

    @Test
    fun `summarize selected names limits visible names and appends hidden count`() {
        val summary = summarizeSelectedNames(
            names = listOf("工作", "阅读", "散步"),
            emptyLabel = "标签"
        )

        assertEquals("工作 · 阅读 +1", summary)
    }
}
