package com.diary.app.ui.ambientsound

import com.diary.app.data.ambientsound.AudioTrack
import org.junit.Assert.assertEquals
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
