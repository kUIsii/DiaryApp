package com.diary.app.ui.achievement

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.Challenge
import com.diary.app.data.ChallengeCategory
import com.diary.app.data.ChallengeProgress
import com.diary.app.data.ChallengeType
import com.diary.app.ui.components.GlassCard

@Composable
fun ChallengeCard(
    challengeProgress: ChallengeProgress,
    modifier: Modifier = Modifier
) {
    val challenge = challengeProgress.challenge
    val progress = challengeProgress.progress
    val isCompleted = challengeProgress.isCompleted
    val progressPercent = (progress.toFloat() / challenge.target).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 500),
        label = "challengeProgress"
    )

    val categoryColor = when (challenge.category) {
        ChallengeCategory.WRITING -> Color(0xFF4CAF50)
        ChallengeCategory.MOOD -> Color(0xFFE91E63)
        ChallengeCategory.EXPLORATION -> Color(0xFF2196F3)
        ChallengeCategory.STREAK -> Color(0xFFFF9800)
    }

    val typeLabel = when (challenge.type) {
        ChallengeType.WEEKLY -> "每周"
        ChallengeType.MONTHLY -> "每月"
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF4CAF50) else categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = challenge.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 10.sp,
                                color = categoryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = challenge.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) Color(0xFF4CAF50) else categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$progress / ${challenge.target}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isCompleted) "已完成" else "${(progressPercent * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) Color(0xFF4CAF50) else categoryColor
                )
            }
        }
    }
}

@Composable
fun ChallengeSection(
    weeklyChallenges: List<ChallengeProgress>,
    monthlyChallenges: List<ChallengeProgress>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (weeklyChallenges.isNotEmpty()) {
            Text(
                text = "每周挑战",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            weeklyChallenges.forEach { challenge ->
                ChallengeCard(
                    challengeProgress = challenge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (monthlyChallenges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "每月挑战",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            monthlyChallenges.forEach { challenge ->
                ChallengeCard(
                    challengeProgress = challenge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
