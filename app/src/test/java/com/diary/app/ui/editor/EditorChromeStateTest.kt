package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorChromeStateTest {

    @Test
    fun `keyboard showing reopens toolbar unless user manually hid it`() {
        val visible = onEditorKeyboardVisibilityChanged(
            state = EditorChromeState(showToolbar = false),
            isKeyboardVisible = true
        )
        val manuallyHidden = onEditorKeyboardVisibilityChanged(
            state = EditorChromeState(
                showToolbar = false,
                isToolbarManuallyHidden = true
            ),
            isKeyboardVisible = true
        )

        assertTrue(visible.showToolbar)
        assertFalse(manuallyHidden.showToolbar)
    }

    @Test
    fun `toolbar toggle locks explicit user choice`() {
        val toggled = onEditorToolbarVisibilityToggled(EditorChromeState(showToolbar = true))

        assertFalse(toggled.showToolbar)
        assertTrue(toggled.isToolbarManuallyHidden)
        assertTrue(toggled.isToolbarLocked)
    }

    @Test
    fun `tapping same category closes category and unlocks keyboard driven behavior`() {
        val updated = onEditorToolbarCategoryTapped(
            state = EditorChromeState(activeCategory = 2, isToolbarLocked = true),
            category = 2
        )

        assertEquals(-1, updated.activeCategory)
        assertTrue(updated.keepToolbarOpen)
        assertFalse(updated.isToolbarLocked)
    }

    @Test
    fun `user hide action collapses toolbar without keeping it open`() {
        val updated = onEditorToolbarHiddenByUser(
            EditorChromeState(showToolbar = true, keepToolbarOpen = true)
        )

        assertFalse(updated.showToolbar)
        assertTrue(updated.isToolbarManuallyHidden)
        assertFalse(updated.keepToolbarOpen)
    }
}
