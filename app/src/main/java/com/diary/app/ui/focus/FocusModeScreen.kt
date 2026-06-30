package com.diary.app.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.readingcenter.buildReadingFocusSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusModeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReading: (Long?) -> Unit = {},

    viewModel: FocusModeViewModel = viewModel()
) {
    val app = LocalContext.current.applicationContext as DiaryApplication
    val isRunning by viewModel.isRunning.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()
    val selectedSound by viewModel.selectedSound.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()
    val readingSession by app.readingSessionStore.session.collectAsState()

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val focusSummary = buildReadingFocusSummary(readingSession, selectedDuration)

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "专注模式",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer circle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimerCircle(
                    progress = if (selectedDuration > 0) {
                        remainingSeconds.toFloat() / (selectedDuration * 60)
                    } else 0f,
                    isRunning = isRunning
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = if (isRunning) "专注中..." else "准备就绪",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning) {
                    Button(
                        onClick = { viewModel.startSession() },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "开始",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.pauseSession() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "暂停",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.stopSession() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "停止",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                innerPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "当前阅读",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = focusSummary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (readingSession.diaryId != null) {
                        TextButton(onClick = { onNavigateToReading(readingSession.diaryId) }) {
                            Text("直接返回阅读")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration selection
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "专注时长",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(15, 25, 45, 60).forEach { duration ->
                            DurationChip(
                                minutes = duration,
                                isSelected = selectedDuration == duration,
                                onClick = { viewModel.setDuration(duration) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ambient sound selection
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "环境音",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SoundChip(
                            label = "无",
                            isSelected = selectedSound == null,
                            onClick = { viewModel.setSound(null) }
                        )
                        SoundChip(
                            label = "雨声",
                            isSelected = selectedSound == "rain",
                            onClick = { viewModel.setSound("rain") }
                        )
                        SoundChip(
                            label = "咖啡厅",
                            isSelected = selectedSound == "cafe",
                            onClick = { viewModel.setSound("cafe") }
                        )
                        SoundChip(
                            label = "白噪音",
                            isSelected = selectedSound == "whitenoise",
                            onClick = { viewModel.setSound("whitenoise") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (readingSession.diaryId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedActionButton(
                        label = "返回阅读",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToReading(readingSession.diaryId) }
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Session history
            if (completedSessions.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "历史记录",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        completedSessions.take(5).forEach { session ->
                            SessionItem(session)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerCircle(progress: Float, isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.size(240.dp)) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Background circle
        drawCircle(
            color = surfaceVariantColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )

        // Pulsing dot when running
        if (isRunning) {
            val dotRadius = 6.dp.toPx()
            val angle = -90f + 360f * progress
            val dotX = center.x + radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val dotY = center.y + radius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            drawCircle(
                color = primaryColor,
                radius = dotRadius * (1f + 0.2f * kotlin.math.sin(animatedProgress * 2 * Math.PI).toFloat()),
                center = Offset(dotX, dotY)
            )
        }
    }
}

@Composable
private fun DurationChip(minutes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${minutes}分钟",
            fontSize = 14.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SoundChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
private fun ElevatedActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(label)
    }
}

@Composable
private fun SessionItem(session: com.diary.app.data.FocusSession) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(session.startTime))
    val duration = session.durationMinutes

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${duration}分钟专注",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateStr,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        session.ambientSound?.let { sound ->
            Text(
                text = when (sound) {
                    "rain" -> "雨声"
                    "cafe" -> "咖啡厅"
                    "whitenoise" -> "白噪音"
                    else -> sound
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
