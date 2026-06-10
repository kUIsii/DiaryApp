package com.diary.app.ui.experimental

import com.diary.app.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExperimentalFeatureLogicTest {

    @Test
    fun `swipe target moves to next main screen when dragging left`() {
        val target = resolveMainScreenSwipeTarget(
            currentRoute = "home",
            totalDrag = -60f,
            enabled = true
        )

        assertEquals("timeline", target)
    }

    @Test
    fun `swipe target moves to previous main screen when dragging right`() {
        val target = resolveMainScreenSwipeTarget(
            currentRoute = "todo",
            totalDrag = 70f,
            enabled = true
        )

        assertEquals("timeline", target)
    }

    @Test
    fun `swipe target stays null when feature disabled or already at edge`() {
        assertNull(
            resolveMainScreenSwipeTarget(
                currentRoute = "home",
                totalDrag = 80f,
                enabled = true
            )
        )

        assertNull(
            resolveMainScreenSwipeTarget(
                currentRoute = "timeline",
                totalDrag = -80f,
                enabled = false
            )
        )
    }

    @Test
    fun `completed items stay in original order when experiment enabled`() {
        val items = listOf(
            TodoItem(id = 1, title = "first", isCompleted = false),
            TodoItem(id = 2, title = "second", isCompleted = true),
            TodoItem(id = 3, title = "third", isCompleted = false)
        )

        val ordered = orderTodoItemsForDisplay(items, keepCompletedInPlace = true)

        assertEquals(listOf(1L, 2L, 3L), ordered.map { it.id })
    }

    @Test
    fun `completed items move to the end when experiment disabled`() {
        val items = listOf(
            TodoItem(id = 1, title = "first", isCompleted = false),
            TodoItem(id = 2, title = "second", isCompleted = true),
            TodoItem(id = 3, title = "third", isCompleted = false)
        )

        val ordered = orderTodoItemsForDisplay(items, keepCompletedInPlace = false)

        assertEquals(listOf(1L, 3L, 2L), ordered.map { it.id })
    }
}
