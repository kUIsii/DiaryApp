package com.diary.app.ui.editor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.moodColorForLevel

private data class MoodInfo(val level: Int, val label: String, val icon: ImageVector)

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
    onLevelChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel = selectedLevel ?: 0
    val currentColor = if (currentLevel > 0) moodColorForLevel(currentLevel) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentLevel > 0) {
                Icon(
                    moodIcons[currentLevel - 1],
                    null,
                    tint = currentColor,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = " ${moodLabels[currentLevel - 1]}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = currentColor
                )
            } else {
                Text(
                    text = "点击选择心情",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 until 6) {
                MoodItem(
                    index = i,
                    isSelected = currentLevel == (i + 1),
                    color = moodColorForLevel(i + 1),
                    onClick = {
                        // Toggle: click same mood to deselect
                        if (selectedLevel == i + 1) {
                            onLevelChange(null)
                        } else {
                            onLevelChange(i + 1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MoodItem(
    index: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    // 点击弹性动画
    var pressed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "mood_press_scale"
    )

    // 选中脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "mood_pulse")
    val pulseScale by if (isSelected) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mood_pulse_scale"
        )
    } else {
        animateFloatAsState(
            targetValue = 1.0f,
            animationSpec = spring(),
            label = "mood_idle_scale"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .scale(pulseScale)
        ) {
            // 外圈光环效果（仅选中）
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f))
                )
            }

            // 圆形背景
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.25f),
                                    color.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.08f),
                                    color.copy(alpha = 0.04f)
                                )
                            )
                        }
                    )
            ) {
                Icon(
                    moodIcons[index],
                    contentDescription = moodLabels[index],
                    tint = if (isSelected) color else color.copy(alpha = 0.5f),
                    modifier = Modifier.size(if (isSelected) 32.dp else 24.dp)
                )
            }
        }

        Text(
            text = moodLabels[index],
            fontSize = if (isSelected) 16.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    // 重置 pressed 状态
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}
