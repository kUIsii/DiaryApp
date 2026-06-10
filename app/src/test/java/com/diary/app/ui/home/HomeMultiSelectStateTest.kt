package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMultiSelectStateTest {

    @Test
    fun `startSelection enables multiselect and selects first id`() {
        val state = HomeMultiSelectState.startSelection(42L)

        assertTrue(state.isEnabled)
        assertEquals(setOf(42L), state.selectedIds)
    }

    @Test
    fun `toggleSelection adds and removes ids while staying enabled`() {
        val state = HomeMultiSelectState.startSelection(1L)

        val afterAdd = state.toggleSelection(2L)
        val afterRemove = afterAdd.toggleSelection(1L)

        assertTrue(afterAdd.selectedIds.containsAll(setOf(1L, 2L)))
        assertEquals(setOf(2L), afterRemove.selectedIds)
        assertTrue(afterRemove.isEnabled)
    }

    @Test
    fun `toggleSelection can leave empty selection while staying enabled`() {
        val state = HomeMultiSelectState.startSelection(7L)

        val result = state.toggleSelection(7L)

        assertTrue(result.isEnabled)
        assertTrue(result.selectedIds.isEmpty())
    }

    @Test
    fun `clearSelection disables multiselect and removes all ids`() {
        val state = HomeMultiSelectState.startSelection(5L).toggleSelection(6L)

        val cleared = state.clearSelection()

        assertFalse(cleared.isEnabled)
        assertTrue(cleared.selectedIds.isEmpty())
    }
}
