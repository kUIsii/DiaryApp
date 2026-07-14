package com.diary.app.data.ambientsound

import androidx.annotation.DrawableRes

data class AudioTrack(
    val id: String,
    val name: String,
    val subtitle: String,
    val durationSeconds: Int,
    val audioUrl: String? = null,
    val imageUrl: String? = null,
    @DrawableRes val imageRes: Int = 0
)
