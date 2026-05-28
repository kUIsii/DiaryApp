package com.diary.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoodInfo(val level: Int, val label: String, val emoji: String)

private val moodLevels = listOf(
    MoodInfo(1, "沮丧", "😞"),
    MoodInfo(2, "低落", "😔"),
    MoodInfo(3, "平静", "😐"),
    MoodInfo(4, "还好", "🙂"),
    MoodInfo(5, "不错", "😊"),
    MoodInfo(6, "愉快", "😄"),
    MoodInfo(7, "开心", "😁"),
    MoodInfo(8, "兴奋", "🤩"),
    MoodInfo(9, "狂喜", "🥳"),
    MoodInfo(10, "巅峰", "🌟")
)

private val moodColors = listOf(
    Color(0xFFE74C3C), // red
    Color(0xFFE67E22), // orange
    Color(0xFFF39C12), // yellow-orange
    Color(0xFFF1C40F), // yellow
    Color(0xFFD4E157), // yellow-green
    Color(0xFF9CCC65), // lime
    Color(0xFF66BB6A), // green
    Color(0xFF43A047), // dark green
    Color(0xFF2E7D32), // deeper green
    Color(0xFF1B5E20)  // deepest green
)

@Composable
fun MoodSlider(
    selectedLevel: Int?,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel = selectedLevel ?: 5
    val currentMood = moodLevels[currentLevel - 1]
    val currentColor = moodColors[currentLevel - 1]

    Column(modifier = modifier.fillMaxWidth()) {
        // Current mood display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMood.emoji,
                fontSize = 28.sp
            )
            Text(
                text = " ${currentMood.label}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = currentColor
            )
        }

        // Slider
        Slider(
            value = currentLevel.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = currentColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Level labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "沮丧", fontSize = 10.sp, color = moodColors[0])
            Text(text = "兴奋", fontSize = 10.sp, color = moodColors[9])
        }
    }
}
