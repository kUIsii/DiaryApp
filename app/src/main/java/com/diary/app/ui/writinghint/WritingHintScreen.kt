package com.diary.app.ui.writinghint

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
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
fun WritingHintScreen(
    onNavigateBack: () -> Unit,
    viewModel: WritingHintViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "写作灵感",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.generateHints() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "换一批")
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (state.isLoading) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text(
                            "正在生成写作灵感...",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                state.hints.forEachIndexed { index, hint ->
                    HintCard(
                        category = hint.category,
                        content = hint.content,
                        index = index
                    )
                    if (index < state.hints.lastIndex) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    }
                }

                if (state.errorMsg != null) {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Text(
                        text = state.errorMsg!!,
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

                OutlinedButton(
                    onClick = { viewModel.generateHints() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium))
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text("换一批")
                }
            }
        }
    }
}

@Composable
private fun HintCard(category: String, content: String, index: Int) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )
    val accentColor = colors[index % colors.size]

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
            Text(
                text = content,
                fontSize = DesignTokens.FontBody,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
