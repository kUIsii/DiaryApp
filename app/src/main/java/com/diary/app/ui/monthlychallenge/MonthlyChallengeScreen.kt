package com.diary.app.ui.monthlychallenge

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyChallengeScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyChallengeViewModel = viewModel()
) {
    val currentChallenge by viewModel.currentChallenge.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val completedDays = dailyLogs.filter { it.completed }.map { it.date }.toSet()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "月度挑战", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (currentChallenge != null) {
                val challenge = currentChallenge!!
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                            Text(
                                text = "${challenge.month}月挑战",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text(
                            text = challenge.title,
                            fontSize = DesignTokens.FontTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                        Text(
                            text = challenge.description,
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        LinearProgressIndicator(
                            progress = challenge.completedDays.toFloat() / challenge.targetDays.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = "${challenge.completedDays}/${challenge.targetDays} 天 · ${((challenge.completedDays.toFloat() / challenge.targetDays) * 100).toInt()}% 完成",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "完成记录",
                            fontSize = DesignTokens.FontMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        ChallengeCalendarGrid(
                            completedDays = completedDays,
                            onDayClick = { date -> viewModel.toggleDay(date) }
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                        Text("正在加载挑战...", fontSize = DesignTokens.FontBody)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCalendarGrid(
    completedDays: Set<Long>,
    onDayClick: (Long) -> Unit
) {
    val now = java.time.LocalDate.now()
    val daysInMonth = now.lengthOfMonth()
    val firstDayOfWeek = now.withDayOfMonth(1).dayOfWeek.value % 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Calendar days
        var dayCounter = 1
        for (week in 0 until 6) {
            if (dayCounter > daysInMonth) break
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0 until 7) {
                    if ((week == 0 && dayOfWeek < firstDayOfWeek) || dayCounter > daysInMonth) {
                        Spacer(modifier = Modifier.width(36.dp).height(36.dp))
                    } else {
                        val day = dayCounter
                        val dateMillis = now.withDayOfMonth(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val isCompleted = completedDays.any { 
                            java.time.Instant.ofEpochMilli(it)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate() == now.withDayOfMonth(day)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small,
                                    onClick = { onDayClick(dateMillis) }
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = day.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onDayClick(dateMillis) }
                                )
                            }
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { onClick() })
    }
)
