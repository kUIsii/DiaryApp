package com.diary.app.ui.values

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValuesExtractionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ValuesViewModel = viewModel()
) {
    val values by viewModel.values.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "价值观", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "AI从你的日记中提取的价值观",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = "我们嘴上说在乎的东西和实际写下来的东西往往不一样。日记是诚实的。",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "你最在乎的 (${values.size})",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
            )

            if (values.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "还没有提取到价值观，多写几篇日记后系统会自动分析。",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    items(values) { value ->
                        val description = when (value.category) {
                            "家庭" -> "你花最多时间记录与家人的相处"
                            "成长" -> "你频繁提到学习和自我提升"
                            "健康" -> "运动和饮食是你日记的常客"
                            "友情" -> "你珍惜与朋友的每一次聚会"
                            "事业" -> "工作成就和职业发展是你关注的重点"
                            "兴趣" -> "你享受爱好带来的乐趣和放松"
                            else -> value.evidence.take(50)
                        }
                        ValueItem(value.value, description, value.confidence)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ValueItem(value: String, description: String, confidence: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 26.dp)
            )
        }
    }
}
