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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.diary.app.ui.theme.MoodCalm
import com.diary.app.ui.theme.MoodCheerful
import com.diary.app.ui.theme.MoodDepressed
import com.diary.app.ui.theme.MoodExcited
import com.diary.app.ui.theme.MoodHappy
import com.diary.app.ui.theme.WeatherCloudy
import com.diary.app.ui.theme.WeatherRainy
import com.diary.app.ui.theme.WeatherStormy
import com.diary.app.ui.theme.WeatherSunny
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader

private fun soundIcon(type: AmbientSoundType): ImageVector = when (type) {
    AmbientSoundType.WHITE_NOISE -> Icons.Default.GraphicEq
    AmbientSoundType.RAIN -> Icons.Default.WaterDrop
    AmbientSoundType.FOREST -> Icons.Default.Nature
    AmbientSoundType.OCEAN -> Icons.Default.Waves
    AmbientSoundType.CAFE -> Icons.Default.Coffee
}

private fun soundColor(type: AmbientSoundType): Color = when (type) {
    AmbientSoundType.WHITE_NOISE -> MoodDepressed.first
    AmbientSoundType.RAIN -> WeatherRainy
    AmbientSoundType.FOREST -> MoodHappy.first
    AmbientSoundType.OCEAN -> MoodCalm.first
    AmbientSoundType.CAFE -> MoodCheerful.first
}

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

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = "\u573A\u666F\u73AF\u5883\u97F3", onNavigateBack = onNavigateBack)
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 8.dp) {
                AmbientSoundType.entries.forEach { type ->
                    val active = type in state.activeSounds
                    val vol = state.volumes[type] ?: 0.5f
                    SoundCard(type = type, active = active, volume = vol,
                        onToggle = { viewModel.toggle(type) },
                        onVolume = { viewModel.setVolume(type, it) })
                    if (type != AmbientSoundType.entries.last()) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            if (state.presets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\u97F3\u6548\u9884\u8BBE", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { presetName = ""; showSaveDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "\u4FDD\u5B58\u9884\u8BBE", modifier = Modifier.size(18.dp), tint = primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.presets.take(5).forEach { preset ->
                            FilterChip(selected = false, onClick = { viewModel.applyPreset(preset) },
                                label = { Text(preset.name, fontSize = 11.sp) }, modifier = Modifier.weight(1f, fill = false))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u5B9A\u65F6\u5173\u95ED", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (state.isSleepFading) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                            Text("\u6E10\u5F31\u4E2D", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimerOption.entries.forEach { opt ->
                        FilterChip(selected = state.timerOption == opt, onClick = { viewModel.setTimer(opt) },
                            label = { Text(opt.label, fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                    }
                }
                if (state.remainingSeconds > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\u5269\u4F59 ${state.remainingSeconds / 60}\u5206${state.remainingSeconds % 60}\u79D2",
                        fontSize = 12.sp, color = if (state.isSleepFading) MaterialTheme.colorScheme.error else onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }

            if (state.activeSounds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.stopAll() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("\u505C\u6B62\u6240\u6709\u97F3\u6548")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(onDismissRequest = { showSaveDialog = false },
            title = { Text("\u4FDD\u5B58\u97F3\u6548\u9884\u8BBE") },
            text = {
                OutlinedTextField(value = presetName, onValueChange = { presetName = it }, label = { Text("\u9884\u8BBE\u540D\u79F0") }, singleLine = true)
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
private fun SoundCard(type: AmbientSoundType, active: Boolean, volume: Float, onToggle: () -> Unit, onVolume: (Float) -> Unit) {
    val color = soundColor(type)
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor by animateColorAsState(
        targetValue = if (active) color.copy(alpha = 0.08f) else Color.Transparent,
        label = "soundBg"
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (active) color else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(soundIcon(type), contentDescription = null,
                    tint = if (active) Color.White else colorOnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(type.displayName, fontSize = 15.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) color else MaterialTheme.colorScheme.onBackground)
                if (active) {
                    Text("\u64AD\u653E\u4E2D", fontSize = 11.sp, color = color.copy(alpha = 0.7f))
                }
            }
        }
        if (active) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(value = volume, onValueChange = onVolume, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
                Text("${(volume * 100).toInt()}%", fontSize = 11.sp, color = colorOnSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
        }
    }
}
