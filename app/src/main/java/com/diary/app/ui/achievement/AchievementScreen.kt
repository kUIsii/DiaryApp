package com.diary.app.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

internal val TierColors = mapOf(
    AchievementTier.COMMON to Color(0xFF8B8A84),
    AchievementTier.RARE to Color(0xFF4B7FD1),
    AchievementTier.EPIC to Color(0xFF8F57C8),
    AchievementTier.LEGENDARY to Color(0xFFD79B28)
)

internal fun tierColor(tier: AchievementTier): Color = TierColors[tier] ?: Color.Gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val galleryState by viewModel.galleryState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedStateFilter by viewModel.selectedStateFilter.collectAsState()
    val isFilterExpanded by viewModel.isFilterExpanded.collectAsState()

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

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
                    text = "成就收藏",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview card
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementOverviewCard(stats = stats, galleryState = galleryState)
                }

                // Filter chips row
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementFilterRow(
                        selectedStateFilter = selectedStateFilter,
                        selectedCategory = selectedCategory,
                        selectedTier = selectedTier,
                        onStateFilterClick = viewModel::selectStateFilter,
                        onCategoryClick = viewModel::selectCategory,
                        onTierClick = viewModel::selectTier,
                        onFilterSheetClick = { viewModel.toggleFilter() }
                    )
                }

                // Grid
                if (galleryState.filteredCards.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂时还没有藏品",
                                fontSize = 13.sp,
                                color = textSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(
                        count = galleryState.filteredCards.size,
                        key = { galleryState.filteredCards[it].item.def.key }
                    ) { index ->
                        val card = galleryState.filteredCards[index]
                        AchievementCompactCard(
                            card = card,
                            onClick = { onNavigateToDetail(card.item.def.key) }
                        )
                    }
                }
            }
        }
    }

    if (isFilterExpanded) {
        AchievementFilterSheet(
            selectedStateFilter = selectedStateFilter,
            selectedCategory = selectedCategory,
            selectedTier = selectedTier,
            stats = stats,
            viewModel = viewModel,
            onStateFilterClick = viewModel::selectStateFilter,
            onCategoryClick = viewModel::selectCategory,
            onTierClick = viewModel::selectTier,
            onDismiss = { viewModel.collapseFilter() }
        )
    }
}

@Composable
private fun AchievementOverviewCard(
    stats: AchievementStats,
    galleryState: AchievementGalleryState
) {
    val hero = galleryState.hero
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val completionPercent = (hero.completionFraction * 100).toInt()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: progress ring + text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = hero.completionFraction,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "$completionPercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = hero.headline,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "${stats.unlockedCount} / ${stats.totalCount}",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            // Right: tier counts
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AchievementTier.entries.forEach { tier ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${tier.tierInt}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor(tier)
                        )
                        Text(
                            text = tier.displayName.take(1),
                            fontSize = 9.sp,
                            color = textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementFilterRow(
    selectedStateFilter: AchievementGalleryFilter,
    selectedCategory: AchievementCategory?,
    selectedTier: AchievementTier?,
    onStateFilterClick: (AchievementGalleryFilter) -> Unit,
    onCategoryClick: (AchievementCategory?) -> Unit,
    onTierClick: (AchievementTier?) -> Unit,
    onFilterSheetClick: () -> Unit
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // State filter chips (compact)
        AchievementGalleryFilter.entries.take(2).forEach { filter ->
            val isSelected = selectedStateFilter == filter
            Surface(
                onClick = { onStateFilterClick(filter) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = filter.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Filter sheet trigger
        Surface(
            onClick = onFilterSheetClick,
            shape = RoundedCornerShape(999.dp),
            color = if (selectedCategory != null || selectedTier != null)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (selectedCategory != null || selectedTier != null)
                        MaterialTheme.colorScheme.primary else textSecondary
                )
                Text(
                    text = "筛选",
                    fontSize = 11.sp,
                    color = if (selectedCategory != null || selectedTier != null)
                        MaterialTheme.colorScheme.primary else textSecondary
                )
            }
        }
    }
}

@Composable
private fun AchievementCompactCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit
) {
    val item = card.item
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Artwork - uses gradient background + vector icon
        AchievementArtwork(
            achievementKey = item.def.key,
            category = item.def.category,
            tier = item.def.tier,
            isUnlocked = item.isUnlocked,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 12
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Name
        Text(
            text = if (isHiddenLocked(item)) "???" else item.def.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Status
        if (item.isUnlocked) {
            Text(
                text = "已达成",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        } else if (!isHiddenLocked(item) && item.progressFraction > 0f) {
            Text(
                text = "${(item.progressFraction * 100).toInt()}%",
                fontSize = 9.sp,
                color = textSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementFilterSheet(
    selectedStateFilter: AchievementGalleryFilter,
    selectedCategory: AchievementCategory?,
    selectedTier: AchievementTier?,
    stats: AchievementStats,
    viewModel: AchievementViewModel,
    onStateFilterClick: (AchievementGalleryFilter) -> Unit,
    onCategoryClick: (AchievementCategory?) -> Unit,
    onTierClick: (AchievementTier?) -> Unit,
    onDismiss: () -> Unit
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "筛选",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "分类", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AchievementCategory.entries.take(4).forEach { category ->
                        val catStats = stats.categoryCounts[category] ?: (0 to 0)
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryClick(category) },
                            label = { Text("${category.displayName} ${catStats.first}/${catStats.second}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor(category),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AchievementCategory.entries.drop(4).forEach { category ->
                        val catStats = stats.categoryCounts[category] ?: (0 to 0)
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryClick(category) },
                            label = { Text("${category.displayName} ${catStats.first}/${catStats.second}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor(category),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tier
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "稀有度", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AchievementTier.entries.forEach { tier ->
                        val tierStats = viewModel.getTierProgress(tier)
                        val isSelected = selectedTier == tier
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTierClick(tier) },
                            label = { Text("${tier.displayName} ${tierStats.first}/${tierStats.second}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tierColor(tier),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun isHiddenLocked(item: com.diary.app.data.AchievementItem): Boolean {
    return item.def.isHidden && !item.isUnlocked
}
