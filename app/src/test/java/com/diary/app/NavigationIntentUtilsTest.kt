package com.diary.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationIntentUtilsTest {

    @Test
    fun `new diary shortcut resolves to editor`() {
        assertEquals(
            "editor",
            resolveExternalNavigation(
                action = "com.diary.app.NEW_DIARY",
                navigateTo = null,
                requestedAction = null
            )
        )
    }

    @Test
    fun `quick todo shortcut resolves to todo`() {
        assertEquals(
            "todo_add",
            resolveExternalNavigation(
                action = "com.diary.app.QUICK_TODO",
                navigateTo = null,
                requestedAction = null
            )
        )
    }

    @Test
    fun `todo add request resolves to todo add`() {
        assertEquals(
            "todo_add",
            resolveExternalNavigation(
                action = null,
                navigateTo = "todo",
                requestedAction = "add"
            )
        )
    }

    @Test
    fun `countdown request is preserved`() {
        assertEquals(
            "countdown",
            resolveExternalNavigation(
                action = null,
                navigateTo = "countdown",
                requestedAction = null
            )
        )
    }
}
