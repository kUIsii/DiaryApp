package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorWritingGuideResultTest {

    @Test
    fun `writing guide extracts title and body from ai response`() {
        val result = parseWritingGuideResult(
            """
            雨后的散步
            第一段：今天的天气
            第二段：散步时看到的东西
            """.trimIndent()
        )

        assertEquals("雨后的散步", result.title)
        assertEquals("第一段：今天的天气\n第二段：散步时看到的东西", result.body)
    }

    @Test
    fun `writing guide falls back to full content when body is missing`() {
        val result = parseWritingGuideResult("只写一个标题")

        assertEquals("只写一个标题", result.title)
        assertEquals("只写一个标题", result.body)
    }
}
