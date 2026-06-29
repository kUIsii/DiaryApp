package com.diary.app.ui.voicerecording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecordingLogicTest {

    @Test
    fun `voice memo diary title trims and falls back for blank text`() {
        assertEquals("语音备忘录", buildVoiceMemoTitle(""))
        assertEquals("今天想去公园散步", buildVoiceMemoTitle("今天想去公园散步"))
    }

    @Test
    fun `voice memo diary content escapes quotes and new lines`() {
        assertEquals(
            "{\"ops\":[{\"insert\":\"第一行\\n\\\"第二行\\\"\"}]}",
            buildVoiceMemoDiaryContent("第一行\n\"第二行\"")
        )
    }

    @Test
    fun `voice memo transcript gate ignores blank content`() {
        assertTrue(shouldOfferDiaryCreation("今晚记一件事"))
        assertFalse(shouldOfferDiaryCreation("   "))
    }
}
