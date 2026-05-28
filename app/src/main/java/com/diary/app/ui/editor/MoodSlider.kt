package com.diary.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoodInfo(val level: Int, val label: String, val icon: ImageVector)

private val moodColors = listOf(
    Color(0xFFE74C3C),
    Color(0xFFE67E22),
    Color(0xFFF39C12),
    Color(0xFF9CCC65),
    Color(0xFF66BB6A),
    Color(0xFF2E7D32)
)

private val moodIcons = listOf(
    Icons.Default.MoodBad,
    Icons.Default.SentimentDissatisfied,
    Icons.Default.SentimentNeutral,
    Icons.Default.Mood,
    Icons.Default.SentimentSatisfied,
    Icons.Default.SentimentVerySatisfied
)

private val moodLabels = listOf("沮丧", "低落", "平静", "开心", "愉快", "兴奋")

@Composable
fun MoodSlider(
    selectedLevel: Int?,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel = selectedLevel ?: 3
    val currentColor = moodColors[currentLevel - 1]

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(moodIcons[currentLevel - 1], null, tint = currentColor, modifier = Modifier.size(22.dp))
            Text(
                text = " ${moodLabels[currentLevel - 1]}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = currentColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 until 6) {
                val isSelected = currentLevel == (i + 1)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) currentColor.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { onLevelChange(i + 1) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(moodIcons[i], null, tint = moodColors[i], modifier = Modifier.size(22.dp))
                    Text(
                        text = moodLabels[i],
                        fontSize = 11.sp,
                        color = if (isSelected) currentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
