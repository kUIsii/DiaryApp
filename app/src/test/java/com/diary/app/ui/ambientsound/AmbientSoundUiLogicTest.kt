package com.diary.app.ui.ambientsound

import com.diary.app.data.ambientsound.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundUiLogicTest {

    @Test
    fun `favorites appear before recent tracks and recent tracks keep recency order`() {
        val tracks = listOf(
            track("s1"),
            track("s2"),
            track("s3"),
            track("s4")
        )

        val ordered = orderAmbientTracksForDisplay(
            tracks = tracks,
            favoriteIds = setOf("s3"),
            recentIds = listOf("s2", "s4")
        )

        assertEquals(listOf("s3", "s2", "s4", "s1"), ordered.map { it.id })
    }

    @Test
    fun `supporting text surfaces favorite and recent markers`() {
        val supportingText = buildAmbientTrackSupportingText(
            baseSubtitle = "轻柔雨声伴眠",
            trackId = "s5",
            favoriteIds = setOf("s5"),
            recentIds = listOf("s5")
        )

        assertEquals("已收藏 · 最近播放 · 轻柔雨声伴眠", supportingText)
    }

    @Test
    fun `saved track does not restore when an active session already exists`() {
        assertFalse(
            shouldRestoreAmbientTrack(
                savedTrackId = "s1",
                hasActiveSession = true
            )
        )
    }

    @Test
    fun `state sync closes fullscreen when playback session is gone`() {
        val state = AmbientSoundState(
            currentTrack = track("s1"),
            isFullscreenPlayerVisible = true
        )

        val synced = syncAmbientStateWithPlayer(
            state = state,
            snapshot = AmbientPlayerSnapshot(
                currentTrack = null,
                isPlaying = false,
                volume = 0.35f,
                duration = 0,
                progress = 0,
                sleepRemainingSeconds = 0,
                meanderEnabled = false,
                hasSession = false
            )
        )

        assertNull(synced.currentTrack)
        assertFalse(synced.isFullscreenPlayerVisible)
        assertFalse(synced.isPlaying)
    }

    @Test
    fun `state sync keeps playback details when session is active`() {
        val activeTrack = track("s2")
        val state = AmbientSoundState(isFullscreenPlayerVisible = true)

        val synced = syncAmbientStateWithPlayer(
            state = state,
            snapshot = AmbientPlayerSnapshot(
                currentTrack = activeTrack,
                isPlaying = true,
                volume = 0.8f,
                duration = 180000,
                progress = 42000,
                sleepRemainingSeconds = 300,
                meanderEnabled = true,
                hasSession = true
            )
        )

        assertEquals(activeTrack, synced.currentTrack)
        assertTrue(synced.isPlaying)
        assertEquals(0.8f, synced.volume, 0.0001f)
        assertEquals(180000, synced.duration)
        assertEquals(42000, synced.progress)
        assertEquals(300, synced.sleepRemainingSeconds)
        assertTrue(synced.meanderEnabled)
        assertTrue(synced.isFullscreenPlayerVisible)
    }

    private fun track(id: String): AudioTrack {
        return AudioTrack(
            id = id,
            categoryId = "sleep",
            name = id,
            subtitle = "subtitle-$id",
            durationSeconds = 30,
            audioUrl = null,
            imageUrl = null
        )
    }
}
