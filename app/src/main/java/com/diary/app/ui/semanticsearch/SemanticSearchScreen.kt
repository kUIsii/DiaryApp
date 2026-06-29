package com.diary.app.ui.semanticsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SemanticSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: SemanticSearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "\u8BED\u4E49\u641C\u7D22", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (state.searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
                ) {
                    state.searchHistory.forEach { historyItem ->
                        SuggestionChip(
                            onClick = { viewModel.searchFromHistory(historyItem) },
                            label = { Text(historyItem, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("\u641C\u7D22\u65E5\u8BB0\u5185\u5BB9...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (state.hasSearched) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\u627E\u5230 ${state.results.size} \u4E2A\u7ED3\u679C\uFF0C\u8017\u65F6 ${state.searchTimeMs}ms",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)) {
                        FilterChip(
                            selected = state.sortOrder == SortOrder.RELEVANCE,
                            onClick = { viewModel.setSortOrder(SortOrder.RELEVANCE) },
                            label = { Text("\u76F8\u5173\u5EA6", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.sortOrder == SortOrder.DATE,
                            onClick = { viewModel.setSortOrder(SortOrder.DATE) },
                            label = { Text("\u65E5\u671F", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.groupByMonth,
                            onClick = { viewModel.setGroupByMonth(!state.groupByMonth) },
                            label = { Text("\u5206\u7EC4", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            when {
                state.isIndexing -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Text("\u6B63\u5728\u5EFA\u7ACB\u641C\u7D22\u7D22\u5F15...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                state.isSearching -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text("\u641C\u7D22\u4E2D...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                state.hasSearched && state.results.isEmpty() && state.query.isNotBlank() -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "\u6CA1\u6709\u627E\u5230\u76F8\u5173\u7ED3\u679C",
                            fontSize = DesignTokens.FontBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.results.isNotEmpty() -> {
                    if (state.groupByMonth && state.groupedResults.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                        ) {
                            state.groupedResults.forEach { (month, entries) ->
                                item(key = "month_$month") {
                                    Text(
                                        text = month,
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = DesignTokens.SpacingXs)
                                    )
                                }
                                items(entries, key = { it.entry.id }) { result ->
                                    SearchResultCard(result, dateFormat, onNavigateToDetail)
                                }
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                        ) {
                            items(state.results, key = { it.entry.id }) { result ->
                                SearchResultCard(result, dateFormat, onNavigateToDetail)
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
                !state.hasSearched -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = "\u8F93\u5165\u5173\u952E\u8BCD\u641C\u7D22\u4F60\u7684\u65E5\u8BB0",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            Text(
                                text = "\u652F\u6301\u4E2D\u6587\u5206\u8BCD\u548CTF-IDF\u76F8\u5173\u5EA6\u6392\u5E8F",
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    dateFormat: SimpleDateFormat,
    onNavigateToDetail: (Long) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail(result.entry.id) }
    ) {
        Column {
            Text(
                text = result.entry.title.ifBlank { "\u65E0\u6807\u9898" },
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = result.snippet,
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = "${dateFormat.format(Date(result.entry.createdAt))} \u00B7 \u76F8\u5173\u5EA6 ${(result.score * 100).toInt()}%",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
