package com.diary.app.data.ambientsound

data class AudioCategory(
    val id: String,
    val name: String,
    val backgroundImageUrl: String?
)

data class AudioTrack(
    val id: String,
    val categoryId: String,
    val name: String,
    val durationSeconds: Int,
    val audioUrl: String?,
    val imageUrl: String?
)
