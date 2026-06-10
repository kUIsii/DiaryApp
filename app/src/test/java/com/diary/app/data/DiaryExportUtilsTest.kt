package com.diary.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryExportUtilsTest {

    @Test
    fun `export content stays unchanged for normal sized entries`() {
        val content = """{"ops":[{"insert":"hello"}]}"""

        val normalized = normalizeContentForExport(content)

        assertTrue(normalized == content)
    }

    @Test
    fun `export content strips oversized inline image payloads`() {
        val hugeBase64 = "a".repeat(MAX_EXPORT_INLINE_MEDIA_LENGTH + 128)
        val content = """{"ops":[{"insert":{"image":"data:image/jpeg;base64,$hugeBase64"}}]}"""

        val normalized = normalizeContentForExport(content)

        assertFalse(normalized.contains(hugeBase64))
        assertTrue(normalized.contains("\"image\":\"\""))
    }
}
