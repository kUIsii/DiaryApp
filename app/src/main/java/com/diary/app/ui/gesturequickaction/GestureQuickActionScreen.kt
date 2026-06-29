package com.diary.app.ui.gesturequickaction
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.components.rememberHapticFeedback

private fun gestureIcon(gesture: String): ImageVector = when (gesture) {
    "双击", "三击", "长按", "双指点击" -> Icons.Default.TouchApp
    "上滑" -> Icons.Default.KeyboardArrowUp
    "下滑" -> Icons.Default.KeyboardArrowDown
    "左滑" -> Icons.Default.KeyboardArrowLeft
    "右滑" -> Icons.Default.KeyboardArrowRight
    "双指滑动" -> Icons.Default.SwapHoriz
    "摇晃" -> Icons.Default.History
    else -> Icons.Default.TouchApp
}

private fun gestureTint(index: Int): Color {
    val colors = listOf(
        Color(0xFF5B8DEF), Color(0xFF7C4DFF), Color(0xFFE040FB),
        Color(0xFF536DFE), Color(0xFF00BCD4), Color(0xFF009688),
        Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFF9800),
        Color(0xFFFF5722)
    )
    return colors[index % colors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureQuickActionScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: GestureQuickActionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val showReset by viewModel.resetDialog.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()

    if (showReset) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetDialog() },
            title = { Text("重置所有手势设置") },
            text = { Text("确定要恢复默认手势映射，并清除所有使用统计数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetToDefaults() }) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "手势快捷操作", onNavigateBack = onNavigateBack)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "为常用手势绑定快捷操作，快速执行功能",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    StatsCard(state = state)
                }

                item {
                    state.lastPreview?.let { preview ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            innerPadding = 16.dp
                        ) {
                            Column {
                                Text(
                                    "最近一次测试预览",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    preview.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    preview.note,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (preview.route != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "目标页面: ${preview.route}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "手势映射",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                items(viewModel.gestureOptions) { gesture ->
                    val index = viewModel.gestureIndexMap[gesture] ?: 0
                    GestureActionRow(
                        gesture = gesture,
                        icon = gestureIcon(gesture),
                        tint = gestureTint(index),
                        selectedAction = state.mappings[gesture] ?: "无操作",
                        usageCount = state.stats[gesture] ?: 0,
                        actionOptions = viewModel.actionOptions,
                        onActionSelected = { action -> viewModel.setAction(gesture, action) },
                        onTest = {
                            val action = state.mappings[gesture] ?: "无操作"
                            haptic.click()
                            viewModel.executeAction(action, context)
                            viewModel.recordUsage(gesture)
                        }
                    )
                }

                item {
                    AiSection(
                        isAiEnabled = true,
                        isAnalyzing = state.isAiAnalyzing,
                        suggestions = state.aiSuggestions,
                        onRequestAnalysis = { viewModel.requestAiSuggestions() },
                        onApplySuggestion = { viewModel.applyAiSuggestion(it) },
                        onDismissSuggestions = { viewModel.dismissAiSuggestions() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.showResetDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重置为默认设置", fontWeight = FontWeight.Medium)
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                "使用提示",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "在日记应用的不同界面中，可以通过手势快速触发已设置的操作。\n" +
                                        "长按日期：日历页面长按日期触发\n" +
                                        "滑动条目：在日记列表中左右滑动触发\n" +
                                        "双指操作：在首页用双指点击或滑动\n" +
                                        "摇晃：在任意界面摇晃手机触发",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsCard(state: GestureQuickActionState) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
        Column {
            Text(
                "使用统计",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = state.totalActivations.toString(),
                    label = "总触发次数",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = state.mostUsedGesture.ifEmpty { "-" },
                    label = "最常用手势",
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = state.lastUsedDate.ifEmpty { "-" },
                    label = "最近使用",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureActionRow(
    gesture: String,
    icon: ImageVector,
    tint: Color,
    selectedAction: String,
    usageCount: Int,
    actionOptions: List<String>,
    onActionSelected: (String) -> Unit,
    onTest: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, innerPadding = 12.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = gesture, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(gesture, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("使用 $usageCount 次", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (usageCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$usageCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onTest, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "测试",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedAction,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    actionOptions.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    action,
                                    fontWeight = if (action == selectedAction) FontWeight.Bold else FontWeight.Normal,
                                    color = if (action == selectedAction) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = { onActionSelected(action); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSection(
    isAiEnabled: Boolean,
    isAnalyzing: Boolean,
    suggestions: List<AiSuggestion>?,
    onRequestAnalysis: () -> Unit,
    onApplySuggestion: (AiSuggestion) -> Unit,
    onDismissSuggestions: () -> Unit
) {
    if (!isAiEnabled) return

    if (suggestions != null) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 建议优化", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissSuggestions) { Text("忽略", fontSize = 13.sp) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                suggestions.forEachIndexed { index, s ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        innerPadding = 12.dp
                    ) {
                        Column {
                            Text(s.gesture, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("建议改为: ${s.toAction}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            if (s.reason.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(s.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApplySuggestion(s) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                                ) {
                                    Text("应用", fontSize = 13.sp)
                                }
                                TextButton(onClick = onDismissSuggestions) { Text("跳过", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
        }
    } else if (!isAnalyzing) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            innerPadding = 16.dp,
            onClick = onRequestAnalysis
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI 建议优化", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("分析使用模式，优化手势映射", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 正在分析使用模式...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
