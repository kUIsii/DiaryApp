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

data class IconWithTint(val icon: ImageVector, val tint: Color)

fun moodIconForLevel(level: Int): IconWithTint {
    return when (level.coerceIn(1, 6)) {
        1 -> IconWithTint(Icons.Default.MoodBad, Color(0xFFE57373))
        2 -> IconWithTint(Icons.Default.SentimentDissatisfied, Color(0xFFFFB74D))
        3 -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
        4 -> IconWithTint(Icons.Default.Mood, Color(0xFFAED581))
        5 -> IconWithTint(Icons.Default.SentimentSatisfied, Color(0xFF81C784))
        6 -> IconWithTint(Icons.Default.SentimentVerySatisfied, Color(0xFF4FC3F7))
        else -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
    }
}

fun moodColorForLevel(level: Int): Color {
    return when (level.coerceIn(1, 6)) {
        1 -> Color(0xFFE57373)
        2 -> Color(0xFFFFB74D)
        3 -> Color(0xFFFFF176)
        4 -> Color(0xFFAED581)
        5 -> Color(0xFF81C784)
        6 -> Color(0xFF4FC3F7)
        else -> Color(0xFFFFF176)
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
        "晴", "晴天" -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
        "多云" -> IconWithTint(Icons.Default.Cloud, Color(0xFF90A4AE))
        "阴", "阴天" -> IconWithTint(Icons.Default.CloudQueue, Color(0xFF78909C))
        "雨", "雨天" -> IconWithTint(Icons.Default.Umbrella, Color(0xFF64B5F6))
        "雷", "雷暴" -> IconWithTint(Icons.Default.Thunderstorm, Color(0xFFBA68C8))
        "风", "大风" -> IconWithTint(Icons.Default.Air, Color(0xFF80CBC4))
        else -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
    }
}

fun weatherIconForType(type: String): ImageVector = when (type) {
    "晴" -> Icons.Default.WbSunny
    "多云" -> Icons.Default.Cloud
    "阴" -> Icons.Default.CloudQueue
    "雨" -> Icons.Default.Umbrella
    "风" -> Icons.Default.Air
    "雷雨" -> Icons.Default.Thunderstorm
    else -> Icons.Default.WbSunny
}
