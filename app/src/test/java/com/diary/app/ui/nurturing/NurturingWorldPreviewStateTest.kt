package com.diary.app.ui.nurturing

import org.junit.Assert.assertEquals
import org.junit.Test

class NurturingWorldPreviewStateTest {

    @Test
    fun builds_pet_focused_preview_when_pet_has_feedback_and_active_title() {
        val state = buildNurturingWorldPreview(
            petName = "小记",
            petStateLabel = "平静",
            petMessage = "今晚也辛苦了，我在这里。",
            islandLevel = 7,
            islandMoodLabel = "夜色宁静",
            recentTitle = "凌晨诗人"
        )

        assertEquals("小记正在等你", state.headline)
        assertEquals("今晚也辛苦了，我在这里。", state.petSnippet)
        assertEquals("夜色宁静 · Lv.7", state.islandSnippet)
        assertEquals("最近珍藏：凌晨诗人", state.collectionSnippet)
    }

    @Test
    fun falls_back_to_generic_copy_when_optional_values_are_missing() {
        val state = buildNurturingWorldPreview(
            petName = null,
            petStateLabel = null,
            petMessage = null,
            islandLevel = 1,
            islandMoodLabel = null,
            recentTitle = null
        )

        assertEquals("养成世界正在慢慢生长", state.headline)
        assertEquals("去看看你的陪伴精灵今天状态如何", state.petSnippet)
    }

    @Test
    fun uses_default_collection_copy_when_recent_title_missing() {
        val state = buildNurturingWorldPreview(
            petName = "小记",
            petStateLabel = "开心",
            petMessage = "今天看起来很亮堂。",
            islandLevel = 4,
            islandMoodLabel = "微风晴朗",
            recentTitle = null
        )

        assertEquals("今晚也许会有新的珍藏出现", state.collectionSnippet)
    }
}
