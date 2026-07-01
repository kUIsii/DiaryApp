package com.diary.app.ui.ambientsound

import com.diary.app.data.ambientsound.AudioTrack

fun orderAmbientTracksForDisplay(
    tracks: List<AudioTrack>,
    favoriteIds: Set<String>,
    recentIds: List<String>
): List<AudioTrack> {
    val recentIndex = recentIds.withIndex().associate { it.value to it.index }
    return tracks.sortedWith(
        compareBy<AudioTrack>(
            { if (it.id in favoriteIds) 0 else 1 },
            { recentIndex[it.id] ?: Int.MAX_VALUE }
        ).thenBy { it.name }
    )
}

fun buildAmbientTrackSupportingText(
    baseSubtitle: String,
    trackId: String,
    favoriteIds: Set<String>,
    recentIds: List<String>
): String {
    val markers = buildList {
        if (trackId in favoriteIds) add("已收藏")
        if (trackId in recentIds) add("最近播放")
        if (baseSubtitle.isNotBlank()) add(baseSubtitle)
    }
    return markers.joinToString(" · ")
}

class AmbientPlaybackSessionGate {
    private var suppressNextStopCallback = false

    fun beginSessionReplacement() {
        suppressNextStopCallback = true
    }

    fun shouldDispatchStopCallback(): Boolean {
        if (suppressNextStopCallback) {
            suppressNextStopCallback = false
            return false
        }
        return true
    }
}
