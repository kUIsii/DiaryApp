package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSmartSearchApplyTest {

    @Test
    fun `smart search prefers parsed keywords when available`() {
        assertEquals("露营", resolveSmartSearchQuery("上周开心的露营日记", "露营"))
        assertEquals("原始查询", resolveSmartSearchQuery("原始查询", ""))
        assertEquals("原始查询", resolveSmartSearchQuery("原始查询", "   "))
    }
}
