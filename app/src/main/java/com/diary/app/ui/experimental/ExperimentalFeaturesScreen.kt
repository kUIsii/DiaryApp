package com.diary.app.ui.experimental

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiConfigStore
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@Composable
fun ExperimentalFeaturesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val features by app.experimentalFeatures.collectAsState()

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = "实验功能",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "正在测试的功能可以在这里单独开关",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ExperimentalFeatureRow(
                            icon = Icons.Default.CompareArrows,
                            title = "主页面左右滑动切换",
                            subtitle = "首页、时间线、待办、我的之间可左右滑动切换，动画沿用待办页现有风格。",
                            checked = features.mainScreenSwipeEnabled,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setMainScreenSwipeEnabled(it) }
                        )
                        ExperimentalFeatureDivider()
                        ExperimentalFeatureRow(
                            icon = Icons.Default.Reorder,
                            title = "完成项保留原位置",
                            subtitle = "待办和备忘中勾选完成后保留在当前位置，只显示划线和完成状态，不自动下沉到底部。",
                            checked = features.keepCompletedItemsInPlace,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setKeepCompletedItemsInPlace(it) }
                        )
                    }
                }

                // AI 默契功能区域
                var showApiKeyDialog by remember { mutableStateOf(false) }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ExperimentalFeatureRow(
                            icon = Icons.Default.Psychology,
                            title = "默契",
                            subtitle = "AI 静默辅助，让写日记的体验更自然",
                            checked = features.aiEnabled,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setAiEnabled(it) }
                        )

                        if (features.aiEnabled) {
                            ExperimentalFeatureDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showApiKeyDialog = true }
                                    .padding(vertical = 12.dp, horizontal = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "配置 API Key",
                                    fontSize = 14.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.Title,
                                title = "静默标题",
                                subtitle = "写了内容没标题时，placeholder自动变成一句概括",
                                checked = features.aiSilentTitle,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiSilentTitle(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.History,
                                title = "记忆回响",
                                subtitle = "编辑器底部悄悄浮现一篇相关的旧日记",
                                checked = features.aiMemoryEcho,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiMemoryEcho(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.Today,
                                title = "今日回顾",
                                subtitle = "首页偶尔出现\"X年前的今天\"",
                                checked = features.aiOnThisDay,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiOnThisDay(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.TrendingUp,
                                title = "情绪天气图",
                                subtitle = "日历未写日记的日期显示情绪延续色",
                                checked = features.aiMoodTrend,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiMoodTrend(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.Timer,
                                title = "写作节奏",
                                subtitle = "写作超5分钟时出现缓慢呼吸的光点",
                                checked = features.aiWritingRhythm,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiWritingRhythm(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.Label,
                                title = "标签直觉",
                                subtitle = "与当前内容相关的标签悄悄排到前面",
                                checked = features.aiTagIntuition,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiTagIntuition(it) }
                            )
                            ExperimentalFeatureDivider()
                            ExperimentalFeatureRow(
                                icon = Icons.Default.Star,
                                title = "安静的里程碑",
                                subtitle = "写到第100篇时，首页角落出现一颗小星星",
                                checked = features.aiMilestones,
                                accentColor = accentColor,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onCheckedChange = { app.setAiMilestones(it) }
                            )
                        }
                    }
                }

                // API Key 配置弹窗
                if (showApiKeyDialog) {
                    var apiKey by remember { mutableStateOf(AiConfigStore.getApiKey(context)) }
                    var endpoint by remember { mutableStateOf(AiConfigStore.getEndpoint(context)) }
                    var model by remember { mutableStateOf(AiConfigStore.getModel(context)) }

                    AlertDialog(
                        onDismissRequest = { showApiKeyDialog = false },
                        title = { Text("AI 服务配置") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = endpoint,
                                    onValueChange = { endpoint = it },
                                    label = { Text("Endpoint") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    label = { Text("Model") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                AiConfigStore.setApiKey(context, apiKey)
                                AiConfigStore.setEndpoint(context, endpoint)
                                AiConfigStore.setModel(context, model)
                                showApiKeyDialog = false
                            }) {
                                Text("保存")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showApiKeyDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "这里会逐步放入你想长期试用、但还不确定是否保留的功能。",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ExperimentalFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    textColor: Color,
    textSecondary: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "experimentalRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 3.dp, end = 8.dp)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.45f)
            )
        )
    }
}

@Composable
private fun ExperimentalFeatureDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    )
}
