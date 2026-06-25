package com.diary.app.ui.tools

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ai.AiConfigStore
import com.diary.app.ai.AiServiceManager
import com.diary.app.ui.components.GradientBackground

private val ProviderColors = mapOf(
    "agnes" to Color(0xFF7C4DFF),
    "modelscope" to Color(0xFFFF6D00),
    "deepseek" to Color(0xFF00BFA5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiManagementScreen(
    aiService: AiServiceManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val providers = remember { aiService.getAllProviders() }
    val activeProviderId = remember { mutableStateOf(AiConfigStore.getActiveProvider(context)) }
    var showConfigDialog by remember { mutableStateOf<String?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("AI 管理", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择 AI 服务商",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                providers.forEach { provider ->
                    val isActive = activeProviderId.value == provider.id
                    val color = ProviderColors[provider.id] ?: Color.Gray
                    val configured = AiConfigStore.getApiKey(context, provider.id).isNotBlank()
                    val selectedModel = AiConfigStore.getModel(context, provider.id).ifBlank { provider.defaultModel }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isActive) color.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isActive) 0.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    AiConfigStore.setActiveProvider(context, provider.id)
                                    activeProviderId.value = provider.id
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = provider.displayName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = selectedModel,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "当前使用",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (configured) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoChip(label = "API Key", icon = Icons.Default.Key, color = color)
                                    InfoChip(label = "已配置", icon = Icons.Default.Check, color = Color(0xFF4CAF50))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                onClick = { showConfigDialog = provider.id },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = if (configured) "修改配置" else "配置 API Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Usage stats
                val stats = remember { aiService.getUsageStats() }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "今日用量",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            UsageStat(label = "今日请求数", value = "${stats.dailyTotal}")
                            UsageStat(label = "每日限额", value = "${stats.dailyLimit}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
private fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UsageStat(label: String, value: String) {
    Column {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    else Color.Transparent
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
                                        tint = Color.White, modifier = Modifier.size(12.dp)
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
