@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.MoodCalm
import com.diary.app.ui.theme.MoodCheerful
import com.diary.app.ui.theme.MoodDepressed
import com.diary.app.ui.theme.MoodHappy
import com.diary.app.ui.theme.WeatherRainy

private fun soundIcon(type: AmbientSoundType): ImageVector = when (type) {
    AmbientSoundType.WHITE_NOISE -> Icons.Default.GraphicEq
    AmbientSoundType.RAIN -> Icons.Default.WaterDrop
    AmbientSoundType.FOREST -> Icons.Default.Nature
    AmbientSoundType.OCEAN -> Icons.Default.Waves
    AmbientSoundType.CAFE -> Icons.Default.Coffee
    AmbientSoundType.STREAM -> Icons.Default.WaterDrop
    AmbientSoundType.WIND -> Icons.Default.GraphicEq
    AmbientSoundType.BIRDS -> Icons.Default.Nature
    AmbientSoundType.NIGHT -> Icons.Default.Star
    AmbientSoundType.FIRE -> Icons.Default.LocalFireDepartment
    AmbientSoundType.FAN -> Icons.Default.GraphicEq
    AmbientSoundType.THUNDER -> Icons.Default.Star
}

private fun soundColor(type: AmbientSoundType): Color = when (type) {
    AmbientSoundType.WHITE_NOISE -> MoodDepressed.first
    AmbientSoundType.RAIN -> WeatherRainy
    AmbientSoundType.FOREST -> MoodHappy.first
    AmbientSoundType.OCEAN -> MoodCalm.first
    AmbientSoundType.CAFE -> MoodCheerful.first
    AmbientSoundType.STREAM -> Color(0xFF42A5F5)
    AmbientSoundType.WIND -> Color(0xFFB0BEC5)
    AmbientSoundType.BIRDS -> Color(0xFF66BB6A)
    AmbientSoundType.NIGHT -> Color(0xFF5C6BC0)
    AmbientSoundType.FIRE -> Color(0xFFFF7043)
    AmbientSoundType.FAN -> Color(0xFF90A4AE)
    AmbientSoundType.THUNDER -> Color(0xFF7E57C2)
}

private val timerOptions = listOf(TimerOption.OFF, TimerOption.MIN_15, TimerOption.MIN_30, TimerOption.MIN_60, TimerOption.MIN_90)

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "场景环境音", onNavigateBack = onNavigateBack)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AmbientSoundType.entries, key = { it }) { type ->
                    val active = type in state.activeSounds
                    val vol = state.volumes[type] ?: 0.5f
                    SoundRowCard(
                        type = type,
                        active = active,
                        volume = vol,
                        onToggle = { viewModel.toggle(type) },
                        onVolume = { viewModel.setVolume(type, it) }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
                color = surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = primary)
                        timerOptions.forEach { opt ->
                            FilterChip(
                                selected = state.timerOption == opt,
                                onClick = { viewModel.setTimer(opt) },
                                label = { Text(opt.label, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        if (state.isSleepFading) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("渐弱中", fontSize = 9.sp, color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    if (state.remainingSeconds > 0) {
                        Text("剩余 ${state.remainingSeconds / 60}分${state.remainingSeconds % 60}秒",
                            fontSize = 11.sp,
                            color = if (state.isSleepFading) MaterialTheme.colorScheme.error else onSurfaceVariant,
                            modifier = Modifier.padding(start = 22.dp, top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("总音量", fontSize = 11.sp, color = onSurfaceVariant, modifier = Modifier.width(36.dp))
                        Slider(
                            value = state.masterVolume,
                            onValueChange = { viewModel.setMasterVolume(it) },
                            modifier = Modifier.weight(1f).height(24.dp),
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary)
                        )
                        Text("${(state.masterVolume * 100).toInt()}%", fontSize = 10.sp, color = onSurfaceVariant, modifier = Modifier.width(32.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = state.meanderEnabled,
                            onClick = { viewModel.toggleMeander() },
                            label = { Text("漫游", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(if (state.activeSounds.isNotEmpty()) "播放中：${state.activeSounds.joinToString("、") { it.displayName }}" else "未播放",
                            fontSize = 11.sp, color = onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.stopAll() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("停止", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundRowCard(
    type: AmbientSoundType,
    active: Boolean,
    volume: Float,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit
) {
    val color = soundColor(type)
    val containerColor by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.4f) else Color.Transparent,
        label = "cardBorder"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulseTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 1f, targetValue = 1.12f,
                animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                label = "pulse"
            )
            val pulseAlpha by pulseTransition.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(if (active) color else MaterialTheme.colorScheme.surfaceVariant)
                    .scale(if (active) pulseScale else 1f)
                    .graphicsLayer { alpha = if (active) pulseAlpha else 1f },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    soundIcon(type),
                    contentDescription = null,
                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                type.displayName,
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) color else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(52.dp),
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (active) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (active) "暂停" else "播放",
                    tint = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Slider(
                value = volume,
                onValueChange = onVolume,
                modifier = Modifier.weight(1f).height(24.dp),
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
            )
        }
    }
}
