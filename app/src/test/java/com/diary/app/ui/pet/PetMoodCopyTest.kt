package com.diary.app.ui.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetMoodCopyTest {

    @Test
    fun returns_warm_default_copy_for_calm_state() {
        assertEquals(
            "今晚也辛苦了，我会陪你慢慢安静下来。",
            buildPetMoodCopy(stateLabel = "平静", feedbackText = "")
        )
    }

    @Test
    fun prefers_live_feedback_when_present() {
        assertEquals(
            "今天看起来很亮堂。",
            buildPetMoodCopy(stateLabel = "开心", feedbackText = "今天看起来很亮堂。")
        )
    }
}
