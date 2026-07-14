package com.diary.app.data.ambientsound

import com.diary.app.R

object AudioRepository {
    // 真实音频已放入 assets/ambient_sounds/{id}.mp3（用户提供的 5 个真实录音）
    // 不再分类，扁平展示；每个音轨配有独立 drawable 图标
    private val allTracks = listOf(
        AudioTrack("rain_thunder", "雷雨淅沥", "雨声与远雷，安眠专注", 0, imageRes = R.drawable.ambient_rain_thunder),
        AudioTrack("sea_waves",  "海浪轻拍", "平稳起伏，放松身心", 0, imageRes = R.drawable.ambient_sea_waves),
        AudioTrack("stream_flow","溪流潺潺", "清泉石上，轻柔绵长", 0, imageRes = R.drawable.ambient_stream_flow),
        AudioTrack("waterfall",  "飞瀑流泉", "倾泻而下，白噪掩蔽", 0, imageRes = R.drawable.ambient_waterfall),
        AudioTrack("forest_birds","林间鸟语", "清晨鸟鸣，唤醒自然", 0, imageRes = R.drawable.ambient_forest_birds)
    )

    fun getTrack(id: String): AudioTrack? = allTracks.find { it.id == id }
    fun getAllTracks(): List<AudioTrack> = allTracks
}
