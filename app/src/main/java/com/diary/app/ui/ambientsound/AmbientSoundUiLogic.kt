package com.diary.app.ui.ambientsound

import com.diary.app.data.ambientsound.AudioTrack

// 环境音列表保持仓库原始顺序，点击播放后位置不跳动（不再按收藏/最近重排）。
fun orderAmbientTracksForDisplay(
    tracks: List<AudioTrack>
): List<AudioTrack> = tracks

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
