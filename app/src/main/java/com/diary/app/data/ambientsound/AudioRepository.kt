package com.diary.app.data.ambientsound

object AudioRepository {
    // 真实音频已放入 assets/ambient_sounds/{id}.mp3（用户提供的 5 个真实录音）
    // 拆为「水声」「林间」两类；backgroundImageUrl 统一为 null -> 全屏走主题化色调背景
    val categories = listOf(
        AudioCategory("water", "水声", null),
        AudioCategory("forest", "林间", null)
    )

    // audioUrl 统一为 null -> 仅走本地 assets；缺失时 UI 提示「音频待添加」
    private val allTracks = listOf(
        AudioTrack("rain_thunder", "water", "雷雨", "雨声与远雷，适合安眠与专注", 0, null, null),
        AudioTrack("sea_waves", "water", "海浪", "平稳起伏，放松身心", 0, null, null),
        AudioTrack("stream_flow", "water", "涓涓细流", "溪流潺潺，轻柔绵长", 0, null, null),
        AudioTrack("waterfall", "water", "瀑布", "水流倾泻，白噪掩蔽", 0, null, null),
        AudioTrack("forest_birds", "forest", "森林晨鸟", "清晨鸟鸣，唤醒自然感", 0, null, null)
    )

    fun getTracks(categoryId: String): List<AudioTrack> = allTracks.filter { it.categoryId == categoryId }
    fun getTrack(id: String): AudioTrack? = allTracks.find { it.id == id }
    fun getAllTracks(): List<AudioTrack> = allTracks
}
