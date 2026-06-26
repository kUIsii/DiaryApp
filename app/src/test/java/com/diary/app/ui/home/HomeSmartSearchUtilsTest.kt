package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSmartSearchUtilsTest {

    @Test
    fun `smart search only accepts supported mood levels`() {
        assertEquals(1, parseSmartSearchMoodLevel("1"))
        assertEquals(5, parseSmartSearchMoodLevel("5"))
        assertEquals(null, parseSmartSearchMoodLevel("6"))
        assertEquals(null, parseSmartSearchMoodLevel("0"))
        assertEquals(null, parseSmartSearchMoodLevel("开心"))
    }
}
