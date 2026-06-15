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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.diary.app.ai.AiConfigStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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

    // AI config state
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var configVersion by remember { mutableStateOf(0) }
    val isAiConfigured = remember(configVersion) { AiConfigStore.isConfigured(context) }

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

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ExperimentalFeatureRow(
                            icon = Icons.Default.EmojiEvents,
                            title = "写作里程碑",
                            subtitle = "追踪连续写作天数和累计字数，达成里程碑时在消息中心通知你。",
                            checked = features.writingMilestonesEnabled,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setWritingMilestonesEnabled(it) }
                        )
                        ExperimentalFeatureDivider()
                        ExperimentalFeatureRow(
                            icon = Icons.Default.Lightbulb,
                            title = "AI 洞察卡片",
                            subtitle = "首页偶尔出现轻量的 AI 提示，可能是心情回应、写作鼓励或小发现。",
                            checked = features.aiInsightCardEnabled,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setAiInsightCardEnabled(it) }
                        )
                        ExperimentalFeatureDivider()
                        ExperimentalFeatureRow(
                            icon = Icons.Default.Chat,
                            title = "AI 笔友",
                            subtitle = "写日记时随时可以找 AI 聊几句，像笔友一样给点小建议或随口寒暄。",
                            checked = features.aiPenPalEnabled,
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onCheckedChange = { app.setAiPenPalEnabled(it) }
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ExperimentalNavigateRow(
                            icon = Icons.Default.Key,
                            title = "API Key",
                            subtitle = if (isAiConfigured) "已配置" else "点击配置 Agnes AI 密钥",
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onClick = { showApiKeyDialog = true }
                        )
                        ExperimentalFeatureDivider()
                        ExperimentalNavigateRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "连接测试",
                            subtitle = when {
                                isTesting -> "测试中..."
                                testResult != null -> testResult!!
                                isAiConfigured -> "Agnes 2.0 Flash (免费)"
                                else -> "请先配置 API Key"
                            },
                            accentColor = accentColor,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onClick = if (isAiConfigured && !isTesting) {
                                {
                                    isTesting = true
                                    testResult = null
                                    MainScope().launch {
                                        try {
                                            val result = app.aiService.chat(
                                                com.diary.app.ai.aiRequest("hi", maxTokens = 10)
                                            )
                                            testResult = result.fold(
                                                onSuccess = { "连接成功" },
                                                onFailure = { "失败: ${it.message}" }
                                            )
                                        } catch (e: Exception) {
                                            testResult = "失败: ${e.message}"
                                        } finally {
                                            isTesting = false
                                        }
                                    }
                                }
                            } else null
                        )
                    }
                }

                if (showApiKeyDialog) {
                    var apiKeyInput by remember { mutableStateOf(AiConfigStore.getApiKey(context)) }
                    var endpointInput by remember { mutableStateOf(AiConfigStore.getEndpoint(context)) }
                    AlertDialog(
                        onDismissRequest = { showApiKeyDialog = false },
                        title = { Text("AI 配置") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    label = { Text("API Key") },
                                    placeholder = { Text("粘贴你的 Agnes API Key") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = endpointInput,
                                    onValueChange = { endpointInput = it },
                                    label = { Text("Endpoint") },
                                    placeholder = { Text("https://apihub.agnes-ai.com/v1/") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "填入 Base URL 即可，不需要加 chat/completions。\n默认 Agnes AI 免费服务，无需修改。",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                AiConfigStore.setApiKey(context, apiKeyInput.trim())
                                val cleanedEndpoint = endpointInput.trim()
                                    .removeSuffix("/")
                                    .removeSuffix("chat/completions")
                                    .trimEnd('/') + "/"
                                AiConfigStore.setEndpoint(context, cleanedEndpoint)
                                AiConfigStore.setActiveProvider(context, "agnes")
                                configVersion++
                                showApiKeyDialog = false
                            }) { Text("保存") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showApiKeyDialog = false }) { Text("取消") }
                        }
                    )
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

@Composable
private fun ExperimentalNavigateRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    textColor: Color,
    textSecondary: Color,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "navigateRowScale"
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
                indication = null,
                enabled = onClick != null
            ) { onClick?.invoke() }
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

        if (onClick != null) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
