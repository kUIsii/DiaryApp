package com.diary.app.ui.ambientsound

import com.diary.app.data.ambientsound.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientSoundUiLogicTest {

    @Test
    fun `track list keeps repository order and does not reorder on play`() {
        val tracks = listOf(
            track("s1"),
            track("s2"),
            track("s3"),
            track("s4")
        )

        val ordered = orderAmbientTracksForDisplay(tracks = tracks)

        assertEquals(listOf("s1", "s2", "s3", "s4"), ordered.map { it.id })
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
