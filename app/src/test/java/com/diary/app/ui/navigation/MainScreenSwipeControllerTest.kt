package com.diary.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainScreenSwipeControllerTest {

    @Test
    fun `left swipe moves to next main route`() {
        assertEquals(
            "timeline",
            resolveMainScreenSwipeTarget(
                currentRoute = "home",
                totalDrag = -60f,
                enabled = true
            )
        )
    }

    @Test
    fun `right swipe moves to previous main route`() {
        assertEquals(
            "tools",
            resolveMainScreenSwipeTarget(
                currentRoute = "todo",
                totalDrag = 60f,
                enabled = true
            )
        )
    }

    @Test
    fun `swipe order matches bottom navigation including tools`() {
        assertEquals(
            "tools",
            resolveMainScreenSwipeTarget(
                currentRoute = "timeline",
                totalDrag = -60f,
                enabled = true
            )
        )
        assertEquals(
            "timeline",
            resolveMainScreenSwipeTarget(
                currentRoute = "tools",
                totalDrag = 60f,
                enabled = true
            )
        )
    }

    @Test
    fun `swipe stays null when disabled or route is unknown`() {
        assertNull(
            resolveMainScreenSwipeTarget(
                currentRoute = "timeline",
                totalDrag = -80f,
                enabled = false
            )
        )
        assertNull(
            resolveMainScreenSwipeTarget(
                currentRoute = "settings",
                totalDrag = -80f,
                enabled = true
            )
        )
    }
}
