package com.diary.app.ui.achievement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider

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
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Standard header
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
                // Overview card - full width
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementOverviewCard(stats = stats, galleryState = galleryState)
                }

                // Filter bar - full width
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementFilterBar(
                        selectedStateFilter = selectedStateFilter,
                        selectedCategory = selectedCategory,
                        selectedTier = selectedTier,
                        activeFilterCount = viewModel.getActiveFilterCount(),
                        onFilterClick = { viewModel.toggleFilter() },
                        onStateFilterClick = viewModel::selectStateFilter,
                        onCategoryClick = viewModel::selectCategory,
                        onTierClick = viewModel::selectTier
                    )
                }

                // 3-column grid of achievement cards
                if (galleryState.filteredCards.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "这个筛选下暂时还没有藏品",
                                fontSize = 13.sp,
                                color = textTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(galleryState.filteredCards, key = { it.item.def.key }) { card ->
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circular progress
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = hero.completionFraction,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${stats.unlockedCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "/ ${stats.totalCount}",
                            fontSize = 9.sp,
                            color = textSecondary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = hero.headline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = hero.supportingLine,
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            // Right: mini stats
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniStat(label = "最近", value = galleryState.recentUnlocks.size)
                MiniStat(label = "即将", value = galleryState.nearCompletion.size)
                MiniStat(label = "传说", value = hero.unlockedLegendaryCount)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = "$value",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AchievementFilterBar(
    selectedStateFilter: AchievementGalleryFilter,
    selectedCategory: AchievementCategory?,
    selectedTier: AchievementTier?,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
    onStateFilterClick: (AchievementGalleryFilter) -> Unit,
    onCategoryClick: (AchievementCategory?) -> Unit,
    onTierClick: (AchievementTier?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "全部藏品",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                onClick = onFilterClick,
                shape = RoundedCornerShape(999.dp),
                color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (activeFilterCount > 0) "筛选 ($activeFilterCount)" else "筛选",
                        fontSize = 11.sp,
                        color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeFilterCount > 0) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (selectedStateFilter != AchievementGalleryFilter.ALL) {
                    item {
                        FilterChipSmall(
                            label = selectedStateFilter.label,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { onStateFilterClick(selectedStateFilter) }
                        )
                    }
                }
                if (selectedCategory != null) {
                    item {
                        FilterChipSmall(
                            label = selectedCategory.displayName,
                            color = categoryColor(selectedCategory),
                            onClick = { onCategoryClick(selectedCategory) }
                        )
                    }
                }
                if (selectedTier != null) {
                    item {
                        FilterChipSmall(
                            label = selectedTier.displayName,
                            color = tierColor(selectedTier),
                            onClick = { onTierClick(selectedTier) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipSmall(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AchievementCompactCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit
) {
    val item = card.item
    val context = LocalContext.current
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val imageRes = rememberAchievementImageRes(context, item.def.key)
    val isLocked = !item.isUnlocked

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageRes != 0) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = item.def.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback: category color background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(categoryColor(item.def.category).copy(alpha = if (isLocked) 0.08f else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.def.iconEmoji,
                        fontSize = 28.sp
                    )
                }
            }

            // Tier dot - top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tierColor(item.def.tier))
            )

            // Lock overlay
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                color = textTertiary
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
    onStateFilterClick: (AchievementGalleryFilter) -> Unit,
    onCategoryClick: (AchievementCategory?) -> Unit,
    onTierClick: (AchievementTier?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "筛选藏品",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // State filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "状态", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AchievementGalleryFilter.entries, key = { it.name }) { filter ->
                        FilterChip(
                            selected = selectedStateFilter == filter,
                            onClick = { onStateFilterClick(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Category filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "分类", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AchievementCategory.entries, key = { it.name }) { category ->
                        val catStats = stats.categoryCounts[category] ?: (0 to 0)
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategoryClick(category) },
                            label = { Text("${category.displayName} ${catStats.first}/${catStats.second}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor(category),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Tier filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "稀有度", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AchievementTier.entries, key = { it.name }) { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { onTierClick(tier) },
                            label = { Text(tier.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tierColor(tier),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberAchievementImageRes(context: android.content.Context, key: String): Int {
    return context.resources.getIdentifier("achievement_$key", "drawable", context.packageName)
}

private fun isHiddenLocked(item: AchievementItem): Boolean {
    return item.def.isHidden && !item.isUnlocked
}
