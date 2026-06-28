package com.diary.app.ui.writinglab

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
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
fun WritingLabScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingLabViewModel = viewModel()
) {
    val activeExperiment by viewModel.activeExperiment.collectAsState()
    val participations by viewModel.participations.collectAsState()
    val completedExperiments by viewModel.completedExperiments.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "写作实验室", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (activeExperiment != null) {
                val exp = activeExperiment!!
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "本周实验",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text(
                            text = exp.title,
                            fontSize = DesignTokens.FontTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                        Text(
                            text = exp.description,
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        LinearProgressIndicator(
                            progress = participations.size.toFloat() / 7f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = "已完成 ${participations.size}/7 天",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无进行中的实验",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (completedExperiments.isNotEmpty()) {
                Text(
                    text = "过往实验",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        completedExperiments.forEach { exp ->
                            PastExperimentItem(
                                title = exp.title,
                                description = exp.description,
                                completed = exp.status == "completed"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastExperimentItem(title: String, description: String, completed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (completed) {
            Text(
                text = "已完成",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
