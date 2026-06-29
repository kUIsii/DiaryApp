package com.diary.app.ui.lockscreenquickwrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenQuickWriteLogicTest {

    @Test
    fun `diary payload escapes quotes and new lines`() {
        assertEquals(
            "{\"ops\":[{\"insert\":\"第一行\\n\\\"第二行\\\"\"}]}",
            buildDiaryContentFromQuickWrite("第一行\n\"第二行\"")
        )
    }

    @Test
    fun `smart link suggestion keeps the source quick write id`() {
        val suggestion = SmartLinkSuggestion(
            quickWriteId = 99L,
            linkedEntryId = 12L,
            message = "这段内容和周三的日记有关联，要放在一起吗？"
        )

        assertEquals(99L, suggestion.quickWriteId)
        assertEquals(12L, suggestion.linkedEntryId)
        assertTrue(suggestion.message.contains("有关联"))
    }
}
