package com.diary.app.ui.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ai.AiConfigStore
import com.diary.app.ai.AiServiceManager
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider
import kotlinx.coroutines.delay

@Composable
fun AiManagementScreen(
    aiService: AiServiceManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val providers = remember { aiService.getAllProviders() }
    val activeProviderId = remember { mutableStateOf(AiConfigStore.getActiveProvider(context)) }
    var showConfigDialog by remember { mutableStateOf<String?>(null) }
    var showActivatedHint by remember { mutableStateOf(false) }
    var activatedProviderName by remember { mutableStateOf("") }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val providerColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )

    // Auto-hide activation hint
    LaunchedEffect(showActivatedHint) {
        if (showActivatedHint) {
            delay(2000)
            showActivatedHint = false
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
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
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI 管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Activation hint
                AnimatedVisibility(
                    visible = showActivatedHint,
                    enter = fadeIn() + slideInVertically()
                ) {
                    GlassCard(
                        cornerRadius = 14.dp,
                        innerPadding = 12.dp,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "已切换到 $activatedProviderName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Provider section
                SectionHeader(
                    title = "选择 AI 服务商",
                    icon = Icons.Default.SmartToy,
                    color = MaterialTheme.colorScheme.primary
                )

                val detailedStats = remember { aiService.getDetailedUsageStats() }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    providers.forEachIndexed { index, provider ->
                        val isActive = activeProviderId.value == provider.id
                        val color = providerColors[index % providerColors.size]
                        val configured = AiConfigStore.getApiKey(context, provider.id).isNotBlank()
                        val selectedModel = AiConfigStore.getModel(context, provider.id)
                            .ifBlank { provider.defaultModel }

                        val provRequests = detailedStats.providerRequests[provider.id] ?: 0
                        val provTokens = detailedStats.providerTokens[provider.id] ?: 0

                        ProviderCard(
                            name = provider.displayName,
                            model = selectedModel,
                            color = color,
                            isActive = isActive,
                            isConfigured = configured,
                            requestCount = provRequests,
                            tokenCount = provTokens,
                            onClick = {
                                AiConfigStore.setActiveProvider(context, provider.id)
                                activeProviderId.value = provider.id
                                activatedProviderName = provider.displayName
                                showActivatedHint = true
                            },
                            onConfigClick = { showConfigDialog = provider.id }
                        )
                    }
                }

                // Usage stats section
                val rateStats = remember { aiService.getUsageStats() }

                SectionHeader(
                    title = "今日用量",
                    icon = Icons.Default.DateRange,
                    color = MaterialTheme.colorScheme.secondary
                )

                GlassCard(cornerRadius = 24.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Main stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            UsageStatItem(
                                value = "${rateStats.dailyTotal}",
                                label = "请求数",
                                sublabel = "/ ${rateStats.dailyLimit}",
                                color = MaterialTheme.colorScheme.primary
                            )
                            UsageStatItem(
                                value = formatTokens(detailedStats.tokens),
                                label = "Token 消耗",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Per-model breakdown
                        if (detailedStats.modelTokens.isNotEmpty()) {
                            SettingDivider()
                            Text(
                                text = "模型明细",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textTertiary
                            )
                            detailedStats.modelTokens.forEach { (model, tokens) ->
                                val requests = detailedStats.modelRequests[model] ?: 0
                                ModelUsageRow(
                                    model = model,
                                    requests = requests,
                                    tokens = tokens
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    showConfigDialog?.let { providerId ->
        val provider = providers.find { it.id == providerId } ?: return@let
        AiProviderConfigDialog(
            providerId = providerId,
            providerName = provider.displayName,
            defaultEndpoint = when (providerId) {
                "deepseek" -> "https://api.deepseek.com/"
                "modelscope" -> "https://api-inference.modelscope.cn/v1/"
                else -> "https://apihub.agnes-ai.com/v1/"
            },
            models = provider.availableModels,
            currentModel = AiConfigStore.getModel(context, providerId).ifBlank { provider.defaultModel },
            onDismiss = { showConfigDialog = null },
            onSaved = { key, endpoint, model ->
                AiConfigStore.setApiKey(context, providerId, key)
                AiConfigStore.setEndpoint(context, providerId, endpoint)
                AiConfigStore.setModel(context, providerId, model)
                showConfigDialog = null
            }
        )
    }
}

@Composable
private fun ProviderCard(
    name: String,
    model: String,
    color: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
    isConfigured: Boolean,
    requestCount: Int,
    tokenCount: Int,
    onClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "providerCard"
    )

    GlassCard(
        cornerRadius = 18.dp,
        innerPadding = 14.dp,
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: icon + name + check
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = if (isActive) 0.15f else 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "使用中",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = color,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color.copy(alpha = 0.1f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = model,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "当前使用",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Stats and config row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConfigured && (requestCount > 0 || tokenCount > 0)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${requestCount} 次请求",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${formatTokens(tokenCount)} tokens",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else if (isConfigured) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "已配置",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = if (isConfigured) "修改配置" else "配置 API Key",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onConfigClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UsageStatItem(
    value: String,
    label: String,
    sublabel: String = "",
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Row {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (sublabel.isNotEmpty()) {
                Text(
                    text = sublabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ModelUsageRow(
    model: String,
    requests: Int,
    tokens: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = model,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "${requests} 次",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTokens(tokens),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTokens(tokens: Int): String {
    return when {
        tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0)
        tokens >= 1_000 -> "%.1fK".format(tokens / 1_000.0)
        else -> "$tokens"
    }
}

@Composable
private fun AiProviderConfigDialog(
    providerId: String,
    providerName: String,
    defaultEndpoint: String,
    models: List<String>,
    currentModel: String,
    onDismiss: () -> Unit,
    onSaved: (apiKey: String, endpoint: String, model: String) -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(AiConfigStore.getApiKey(context, providerId)) }
    var endpoint by remember { mutableStateOf(AiConfigStore.getEndpoint(context, providerId).ifBlank { defaultEndpoint }) }
    var selectedModel by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$providerName 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("粘贴你的 $providerName API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint") },
                    placeholder = { Text(defaultEndpoint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (models.size > 1) {
                    Text(text = "模型", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedModel = model }
                                .background(
                                    if (selectedModel == model) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedModel == model) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedModel == model) {
                                    Icon(
                                        Icons.Default.Check, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = model, fontSize = 14.sp)
                        }
                    }
                }

                Text(
                    text = when (providerId) {
                        "deepseek" -> "Deepseek API 兼容 OpenAI 格式。输入 ¥1/百万token，输出 ¥2/百万token。"
                        "modelscope" -> "ModelScope 提供免费 Qwen 模型。"
                        "agnes" -> "Agnes AI 免费服务，无需修改 Endpoint。"
                        else -> ""
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanedEndpoint = endpoint.trim().removeSuffix("/").removeSuffix("chat/completions").trimEnd('/') + "/"
                onSaved(apiKey.trim(), cleanedEndpoint, selectedModel)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
