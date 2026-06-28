package com.diary.app.ui.ambientsound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = "场景环境音", onNavigateBack = onNavigateBack)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (state.presets.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "音效预设",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.presets.take(5).forEach { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.applyPreset(preset) },
                                label = { Text(preset.name, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        IconButton(
                            onClick = {
                                presetName = ""
                                showSaveDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "保存当前为预设", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "选择环境音",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    AmbientSoundType.entries.forEach { type ->
                        val isActive = type in state.activeSounds
                        val volume = state.volumes[type] ?: 0.5f
                        SoundTypeRow(
                            type = type,
                            isActive = isActive,
                            volume = volume,
                            onToggle = { viewModel.toggle(type) },
                            onVolumeChange = { viewModel.setVolume(type, it) }
                        )
                        if (type != AmbientSoundType.entries.last()) {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        }
                    }
                }
            }

            if (state.activeSounds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                            Text(
                                text = "定时关闭",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (state.isSleepFading) {
                                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "渐弱中",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimerOption.entries.forEach { option ->
                                val selected = state.timerOption == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setTimer(option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (state.remainingSeconds > 0) {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            val minutes = state.remainingSeconds / 60
                            val secs = state.remainingSeconds % 60
                            Text(
                                text = "剩余 ${minutes}分${secs}秒",
                                fontSize = DesignTokens.FontSmall,
                                color = if (state.isSleepFading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                Button(
                    onClick = { viewModel.stopAll() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止所有音效")
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存音效预设") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            viewModel.saveCurrentPreset(presetName.trim())
                            showSaveDialog = false
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SoundTypeRow(
    type: AmbientSoundType,
    isActive: Boolean,
    volume: Float,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onToggle)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    fontSize = DesignTokens.FontBody,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }
            if (isActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "播放中",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        if (isActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = DesignTokens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
