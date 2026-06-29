package com.diary.app.ui.gesturequickaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureQuickActionLogicTest {

    @Test
    fun `gesture test preview resolves to a real destination for diary creation`() {
        val preview = resolveGestureExecutionPreview("新建日记")

        assertEquals("editor", preview.route)
        assertTrue(preview.canExecute)
    }

    @Test
    fun `gesture test preview resolves search and unsupported actions distinctly`() {
        assertEquals("semantic_search", resolveGestureExecutionPreview("打开搜索").route)
        assertFalse(resolveGestureExecutionPreview("无操作").canExecute)
    }
}
