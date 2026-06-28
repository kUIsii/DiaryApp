package com.diary.app.ui.streakshield

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
fun StreakShieldScreen(
    onNavigateBack: () -> Unit,
    viewModel: StreakShieldViewModel = viewModel()
) {
    val currentShield by viewModel.currentShield.collectAsState()
    val isUsed by viewModel.isUsed.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "连续保护罩", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Text(
                        text = "本月保护罩",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                    Text(
                        text = if (isUsed) "已使用" else if (currentShield != null) "已激活" else "待激活",
                        fontSize = DesignTokens.FontHeadline,
                        fontWeight = FontWeight.Bold,
                        color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = if (isUsed) "本月保护罩已使用，下月将自动重置"
                               else "如果某天忘记写日记，保护罩会自动激活",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "保护罩规则",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    ShieldRule("每月获得1次保护罩机会")
                    ShieldRule("忘记写日记时自动激活")
                    ShieldRule("保持连续天数不断")
                    ShieldRule("知道有保护罩反而更不容易忘记")
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "使用历史",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = if (isUsed) "本月保护罩已使用"
                               else "你还没有使用过保护罩。继续保持每日写作的好习惯！",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShieldRule(rule: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(6.dp)
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = rule, fontSize = 14.sp)
    }
}
