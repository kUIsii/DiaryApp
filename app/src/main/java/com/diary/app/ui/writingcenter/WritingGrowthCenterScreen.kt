package com.diary.app.ui.writingcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@Composable
fun WritingGrowthCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit = {},
    onNavigateToWritingCoach: () -> Unit = {},
    onNavigateToWritingLab: () -> Unit = {},
    onNavigateToWritingHint: () -> Unit = {},
    onNavigateToSmallWins: () -> Unit = {},
    viewModel: WritingGrowthCenterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "写作成长中心",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                contentPadding = PaddingValues(bottom = DesignTokens.SpacingLg)
            ) {
                item {
                    HeroCard(
                        onNavigateToEditor = onNavigateToEditor,
                        onNavigateToWritingCoach = onNavigateToWritingCoach,
                        onNavigateToWritingLab = onNavigateToWritingLab
                    )
                }
                items(state.content.sections) { section ->
                    GrowthSectionCard(
                        section = section,
                        onNavigateToEditor = onNavigateToEditor,
                        onNavigateToWritingCoach = onNavigateToWritingCoach,
                        onNavigateToWritingLab = onNavigateToWritingLab,
                        onNavigateToWritingHint = onNavigateToWritingHint,
                        onNavigateToSmallWins = onNavigateToSmallWins
                    )
                }
                if (state.isLoading) {
                    item {
                        Text(
                            text = "正在整理本地写作数据…",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    onNavigateToEditor: () -> Unit,
    onNavigateToWritingCoach: () -> Unit,
    onNavigateToWritingLab: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("今天从哪里开始？", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text("先写一篇，继续最近的思路，或者看一眼成长概览再决定。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onNavigateToEditor) { Text("写一篇") }
                Button(onClick = onNavigateToWritingCoach) { Text("看成长概览") }
                Button(onClick = onNavigateToWritingLab) { Text("写作实验室") }
            }
        }
    }
}

@Composable
private fun GrowthSectionCard(
    section: WritingGrowthSection,
    onNavigateToEditor: () -> Unit,
    onNavigateToWritingCoach: () -> Unit,
    onNavigateToWritingLab: () -> Unit,
    onNavigateToWritingHint: () -> Unit,
    onNavigateToSmallWins: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(section.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(section.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (section.quickActions.isNotEmpty()) {
                section.quickActions.forEach { action ->
                    ActionRow(action = action, onNavigateToEditor, onNavigateToWritingCoach)
                }
            }
            if (section.items.isNotEmpty()) {
                section.items.forEach { item ->
                    ItemRow(
                        item = item,
                        onNavigateToEditor = onNavigateToEditor,
                        onNavigateToWritingCoach = onNavigateToWritingCoach,
                        onNavigateToWritingLab = onNavigateToWritingLab,
                        onNavigateToWritingHint = onNavigateToWritingHint,
                        onNavigateToSmallWins = onNavigateToSmallWins
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRow(
    action: WritingGrowthAction,
    onNavigateToEditor: () -> Unit,
    onNavigateToWritingCoach: () -> Unit
) {
    val onClick = when (action.target) {
        WritingGrowthPrimaryAction.WRITE -> onNavigateToEditor
        WritingGrowthPrimaryAction.CONTINUE -> onNavigateToEditor
        WritingGrowthPrimaryAction.OVERVIEW -> onNavigateToWritingCoach
    }
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.label, fontWeight = FontWeight.SemiBold)
                Text(action.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(
    item: WritingGrowthItem,
    onNavigateToEditor: () -> Unit,
    onNavigateToWritingCoach: () -> Unit,
    onNavigateToWritingLab: () -> Unit,
    onNavigateToWritingHint: () -> Unit,
    onNavigateToSmallWins: () -> Unit
) {
    val onClick = when (item.target) {
        WritingGrowthItemTarget.EDITOR -> onNavigateToEditor
        WritingGrowthItemTarget.WRITING_COACH -> onNavigateToWritingCoach
        WritingGrowthItemTarget.WRITING_LAB -> onNavigateToWritingLab
        WritingGrowthItemTarget.WRITING_HINT -> onNavigateToWritingHint
        WritingGrowthItemTarget.SMALL_WINS -> onNavigateToSmallWins
        WritingGrowthItemTarget.NONE -> null
    }
    val icon = when (item.target) {
        WritingGrowthItemTarget.WRITING_COACH -> Icons.Default.TrendingUp
        WritingGrowthItemTarget.WRITING_LAB -> Icons.Default.AutoAwesome
        WritingGrowthItemTarget.WRITING_HINT -> Icons.Default.Lightbulb
        WritingGrowthItemTarget.SMALL_WINS -> Icons.Default.Star
        else -> Icons.Default.Edit
    }

    ElevatedCard(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.metadata?.let {
                    Text(
                        it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
