package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorTagSuggestionTriggerTest {

    @Test
    fun `tag suggestions trigger once when content first crosses threshold`() {
        assertEquals(false, shouldTriggerTagSuggestion(previousLength = 0, currentLength = 199))
        assertEquals(true, shouldTriggerTagSuggestion(previousLength = 199, currentLength = 200))
        assertEquals(true, shouldTriggerTagSuggestion(previousLength = 199, currentLength = 260))
        assertEquals(false, shouldTriggerTagSuggestion(previousLength = 220, currentLength = 260))
    }
}
