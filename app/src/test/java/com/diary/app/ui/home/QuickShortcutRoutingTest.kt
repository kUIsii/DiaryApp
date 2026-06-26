package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickShortcutRoutingTest {

    @Test
    fun `todo shortcut maps to todo route`() {
        assertEquals("todo", resolveQuickShortcutNavigation("todo"))
    }

    @Test
    fun `countdown shortcut maps to countdown route`() {
        assertEquals("countdown", resolveQuickShortcutNavigation("countdown"))
    }
}
