package com.diary.app.ui.adaptiveinterface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader

@Composable
fun AdaptiveInterfaceScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdaptiveInterfaceViewModel = viewModel()
) {
    val adaptiveEnabled by viewModel.adaptiveEnabled.collectAsState()
    val autoNightMode by viewModel.autoNightMode.collectAsState()
    val compactMode by viewModel.compactMode.collectAsState()
    val totalEntries by viewModel.totalEntries.collectAsState()
    val thisMonthEntries by viewModel.thisMonthEntries.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "自适应界面", onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI 根据使用习惯自动调整界面布局",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "自适应设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AdaptiveToggleRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "启用自适应",
                            subtitle = "AI 自动调整界面布局和功能优先级",
                            checked = adaptiveEnabled,
                            onCheckedChange = { viewModel.setAdaptiveEnabled(it) }
                        )
                        AnimatedVisibility(visible = adaptiveEnabled) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                AdaptiveToggleRow(
                                    icon = Icons.Default.Nightlight,
                                    title = "自动夜间模式",
                                    subtitle = "夜晚写作时自动切换暗色主题",
                                    checked = autoNightMode,
                                    onCheckedChange = { viewModel.setAutoNightMode(it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AdaptiveToggleRow(
                                    icon = Icons.Default.Timer,
                                    title = "精简模式",
                                    subtitle = "短写作时隐藏非必要 UI 元素",
                                    checked = compactMode,
                                    onCheckedChange = { viewModel.setCompactMode(it) }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = adaptiveEnabled) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "AI 建议",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val suggestions by viewModel.suggestions.collectAsState()

                            suggestions.forEach { suggestion ->
                                SuggestionCard(suggestion)
                            }
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    innerPadding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "使用统计",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf(
                            "总日记数" to "$totalEntries 篇",
                            "本月日记" to "$thisMonthEntries 篇",
                            "连续写作" to "连续 $currentStreak 天"
                        ).forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun AdaptiveToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ))
    }
}

@Composable
private fun SuggestionCard(suggestion: AdaptiveSuggestion) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(suggestion.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text(suggestion.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(suggestion.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
