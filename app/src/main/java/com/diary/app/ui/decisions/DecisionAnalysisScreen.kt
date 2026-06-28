package com.diary.app.ui.decisions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.Decision
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionAnalysisScreen(
    onNavigateBack: () -> Unit,
    viewModel: DecisionViewModel = viewModel()
) {
    val decisions by viewModel.decisions.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "决策追踪", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "AI识别的重大决定",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = "当你在日记中提到重大决定时，AI自动标记并追踪后续。",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "我的决定 (${decisions.size})",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
            )

            if (decisions.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "还没有识别到决策",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                        Text(
                            text = "在日记中提到重大决定时，系统会自动识别",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    items(decisions.size) { index ->
                        val decision = decisions[index]
                        DecisionItem(
                            decision = decision,
                            dateFormat = dateFormat
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DecisionItem(decision: Decision, dateFormat: SimpleDateFormat) {
    val hasOutcome = decision.outcome != null
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (hasOutcome) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = decision.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${dateFormat.format(Date(decision.madeAt))} · ${if (hasOutcome) "已有结果" else "进行中"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (decision.context.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = decision.context.take(100) + if (decision.context.length > 100) "..." else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
