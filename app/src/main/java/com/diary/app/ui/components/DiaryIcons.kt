package com.diary.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.diary.app.ui.theme.MoodCalm
import com.diary.app.ui.theme.MoodCheerful
import com.diary.app.ui.theme.MoodDepressed
import com.diary.app.ui.theme.MoodDown
import com.diary.app.ui.theme.MoodExcited
import com.diary.app.ui.theme.MoodHappy
import com.diary.app.ui.theme.WeatherCloudy
import com.diary.app.ui.theme.WeatherOvercast
import com.diary.app.ui.theme.WeatherRainy
import com.diary.app.ui.theme.WeatherStormy
import com.diary.app.ui.theme.WeatherSunny
import com.diary.app.ui.theme.WeatherWindy

data class IconWithTint(val icon: ImageVector, val tint: Color)

fun moodIconForLevel(level: Int): IconWithTint {
    return when (level.coerceIn(1, 6)) {
        1 -> IconWithTint(Icons.Default.MoodBad, MoodDepressed.first)
        2 -> IconWithTint(Icons.Default.SentimentDissatisfied, MoodDown.first)
        3 -> IconWithTint(Icons.Default.SentimentNeutral, MoodCalm.first)
        4 -> IconWithTint(Icons.Default.Mood, MoodHappy.first)
        5 -> IconWithTint(Icons.Default.SentimentSatisfied, MoodCheerful.first)
        6 -> IconWithTint(Icons.Default.SentimentVerySatisfied, MoodExcited.first)
        else -> IconWithTint(Icons.Default.SentimentNeutral, MoodCalm.first)
    }
}

fun moodColorForLevel(level: Int): Color {
    return when (level.coerceIn(1, 6)) {
        1 -> MoodDepressed.first
        2 -> MoodDown.first
        3 -> MoodCalm.first
        4 -> MoodHappy.first
        5 -> MoodCheerful.first
        6 -> MoodExcited.first
        else -> MoodCalm.first
    }
}

fun moodLabelForLevel(level: Int): String {
    return when (level.coerceIn(1, 6)) {
        1 -> "沮丧"
        2 -> "低落"
        3 -> "平静"
        4 -> "开心"
        5 -> "愉快"
        6 -> "兴奋"
        else -> "平静"
    }
}

fun weatherIconFor(weather: String?): IconWithTint {
    return when (weather) {
        "晴", "晴天" -> IconWithTint(Icons.Default.WbSunny, WeatherSunny)
        "多云" -> IconWithTint(Icons.Default.Cloud, WeatherCloudy)
        "阴", "阴天" -> IconWithTint(Icons.Default.CloudQueue, WeatherOvercast)
        "雨", "雨天" -> IconWithTint(Icons.Default.Umbrella, WeatherRainy)
        "雪", "雪天" -> IconWithTint(Icons.Default.CloudQueue, WeatherCloudy)
        "雷", "雷暴" -> IconWithTint(Icons.Default.Thunderstorm, WeatherStormy)
        "风", "大风" -> IconWithTint(Icons.Default.Air, WeatherWindy)
        else -> IconWithTint(Icons.Default.WbSunny, WeatherSunny)
    }
}

fun weatherIconForType(type: String): ImageVector = when (type) {
    "晴", "晴天" -> Icons.Default.WbSunny
    "多云" -> Icons.Default.Cloud
    "阴", "阴天" -> Icons.Default.CloudQueue
    "雨", "雨天" -> Icons.Default.Umbrella
    "雪", "雪天" -> Icons.Default.CloudQueue
    "风", "大风" -> Icons.Default.Air
    "雷", "雷暴" -> Icons.Default.Thunderstorm
    else -> Icons.Default.WbSunny
}

fun weatherLabelFor(weather: String?): String {
    return when (weather) {
        "晴", "晴天" -> "晴天"
        "多云" -> "多云"
        "阴", "阴天" -> "阴天"
        "雨", "雨天" -> "雨天"
        "雪", "雪天" -> "雪天"
        "雷", "雷暴" -> "雷暴"
        "风", "大风" -> "大风"
        else -> weather ?: ""
    }
}
