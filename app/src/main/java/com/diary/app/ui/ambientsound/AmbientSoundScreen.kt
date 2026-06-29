@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private val timerOptions = listOf(TimerOption.OFF, TimerOption.MIN_15, TimerOption.MIN_30, TimerOption.MIN_60)

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "\u573A\u666F\u73AF\u5883\u97F3", onNavigateBack = onNavigateBack)

            if (state.presets.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = surface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u97F3\u6548\u9884\u8BBE", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            items(state.presets.take(8)) { preset ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.applyPreset(preset) },
                                    label = { Text(preset.name, fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { presetName = ""; showSaveDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "\u4FDD\u5B58\u9884\u8BBE", modifier = Modifier.size(16.dp), tint = primary)
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AmbientSoundType.entries, key = { it }) { type ->
                    val active = type in state.activeSounds
                    val vol = state.volumes[type] ?: 0.5f
                    SoundGridCard(
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
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                Text(
                                    "\u6E10\u5F31\u4E2D",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (state.remainingSeconds > 0) {
                        Text(
                            "\u5269\u4F59 ${state.remainingSeconds / 60}\u5206${state.remainingSeconds % 60}\u79D2",
                            fontSize = 11.sp,
                            color = if (state.isSleepFading) MaterialTheme.colorScheme.error else onSurfaceVariant,
                            modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.activeSounds.isNotEmpty()) {
                            Text(
                                "\u6B63\u5728\u64AD\u653E\uFF1A",
                                fontSize = 12.sp,
                                color = onSurfaceVariant
                            )
                            Text(
                                state.activeSounds.joinToString("\u3001") { it.displayName },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = { viewModel.stopAll() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("\u505C\u6B62\u5168\u90E8", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("\u4FDD\u5B58\u97F3\u6548\u9884\u8BBE") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("\u9884\u8BBE\u540D\u79F0") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetName.isNotBlank()) { viewModel.saveCurrentPreset(presetName.trim()); showSaveDialog = false }
                }) { Text("\u4FDD\u5B58") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("\u53D6\u6D88") } }
        )
    }
}

@Composable
private fun SoundGridCard(
    type: AmbientSoundType,
    active: Boolean,
    volume: Float,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit
) {
    val color = soundColor(type)
    val containerColor by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.5f) else Color.Transparent,
        label = "cardBorder"
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (active) color else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    soundIcon(type),
                    contentDescription = null,
                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                type.displayName,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) color else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = volume,
                onValueChange = onVolume,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).height(20.dp),
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
            )
        }
    }
}
