@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.ambientsound

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = "场景环境音", onNavigateBack = onNavigateBack)
            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AmbientSoundType.entries.forEach { type ->
                    val active = type in state.activeSounds
                    val vol = state.volumes[type] ?: 0.5f
                    SoundRow(type = type, active = active, volume = vol,
                        onToggle = { viewModel.toggle(type) },
                        onVolume = { viewModel.setVolume(type, it) })
                    if (type != AmbientSoundType.entries.last()) Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (state.presets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("音效预设", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { presetName = ""; showSaveDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "保存预设", modifier = Modifier.size(16.dp), tint = primary)
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
                    Text("定时关闭", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (state.isSleepFading) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                            Text("渐弱中", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                    Text("剩余 ${state.remainingSeconds / 60}分${state.remainingSeconds % 60}秒",
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
                    Text("停止所有音效")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(onDismissRequest = { showSaveDialog = false },
            title = { Text("保存音效预设") },
            text = {
                OutlinedTextField(value = presetName, onValueChange = { presetName = it }, label = { Text("预设名称") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetName.isNotBlank()) { viewModel.saveCurrentPreset(presetName.trim()); showSaveDialog = false }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("取消") } }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SoundRow(type: AmbientSoundType, active: Boolean, volume: Float, onToggle: () -> Unit, onVolume: (Float) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (active) primary.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
    Surface(color = if (active) primary else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(if (active) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(type.displayName, fontSize = 14.sp, fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = if (active) primary else MaterialTheme.colorScheme.onBackground)
        }
        if (active) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 46.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(value = volume, onValueChange = onVolume, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
                Text("${(volume * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }
    }
}
