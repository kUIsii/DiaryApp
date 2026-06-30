package com.diary.app.ui.readingcenter

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.formatEntryDateTime
import com.diary.app.ui.theme.DesignTokens

@Composable
fun ReadingCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImmersiveReader: (Long?) -> Unit = {},
    onNavigateToFocusMode: () -> Unit = {},
    viewModel: ReadingCenterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "阅读中心",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                contentPadding = PaddingValues(bottom = DesignTokens.SpacingLg)
            ) {
                item {
                    HeroCard(
                        content = state.content,
                        session = state.session,
                        onActionClick = { action ->
                            when (action.target) {
                                ReadingCenterTarget.IMMERSIVE_READER -> onNavigateToImmersiveReader(state.session.diaryId)
                                ReadingCenterTarget.FOCUS_MODE -> onNavigateToFocusMode()
                            }
                        }
                    )
                }

                item {
                    FeatureCard(
                        features = state.content.featureItems,
                        onFeatureClick = { target ->
                            when (target) {
                                ReadingCenterTarget.IMMERSIVE_READER -> onNavigateToImmersiveReader(state.session.diaryId)
                                ReadingCenterTarget.FOCUS_MODE -> onNavigateToFocusMode()
                            }
                        }
                    )
                }

                item {
                    OverviewCard(state.content.overviewItems)
                }

                item {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (state.recentEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "最近会话",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(state.recentEntries, key = { it.id }) { preview ->
                        RecentEntryCard(
                            preview = preview,
                            currentSessionId = state.session.diaryId,
                            onContinue = {
                                viewModel.prepareReadingEntry(preview)
                                onNavigateToImmersiveReader(preview.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    content: ReadingCenterContent,
    session: ReadingSessionSnapshot,
    onActionClick: (ReadingCenterHeroAction) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(content.heroTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                text = content.heroSummary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (session.previewText != null) {
                Text(
                    text = "“${session.previewText}”",
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            content.heroActions.forEach { action ->
                ElevatedButton(
                    onClick = { onActionClick(action) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(action.label, modifier = Modifier.weight(1f))
                    Text(
                        action.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    features: List<ReadingCenterFeatureItem>,
    onFeatureClick: (ReadingCenterTarget) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("阅读能力入口", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "每一项都指向真实可用的下一步，而不是只做陈列。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            features.forEach { feature ->
                val icon = when (feature.target) {
                    ReadingCenterTarget.IMMERSIVE_READER -> Icons.Default.MenuBook
                    ReadingCenterTarget.FOCUS_MODE -> Icons.Default.Timer
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    innerPadding = 14.dp,
                    onClick = { onFeatureClick(feature.target) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(feature.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = feature.summary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(items: List<ReadingCenterOverviewItem>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("阅读空间概览", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 16.dp,
                            innerPadding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.value,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEntryCard(
    preview: DiaryPreview,
    currentSessionId: Long?,
    onContinue: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = preview.title.ifBlank { "未命名内容" },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = cleanPreviewText(preview.plainText).take(140).ifBlank { "这篇内容还没有可预览的正文。" },
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = buildString {
                    append(formatEntryDateTime(preview.createdAt))
                    if (preview.id == currentSessionId) {
                        append(" · 当前会话")
                    }
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (preview.id == currentSessionId) "回到阅读" else "开始阅读")
                }

            }
        }
    }
}
