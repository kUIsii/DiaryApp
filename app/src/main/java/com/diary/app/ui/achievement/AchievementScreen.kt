package com.diary.app.ui.achievement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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
import com.diary.app.ui.components.SettingDivider
import com.diary.app.ui.components.staggeredListItem

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
                    AchievementOverviewCard(stats = stats, galleryState = galleryState, viewModel = viewModel)
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
                                color = textSecondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
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
                            onClick = { onNavigateToDetail(card.item.def.key) },
                            modifier = Modifier.staggeredListItem(index)
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
    galleryState: AchievementGalleryState,
    viewModel: AchievementViewModel
) {
    val hero = galleryState.hero
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top row: progress ring + headline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = hero.headline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = hero.supportingLine,
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Category progress bars
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "分类进度",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
                AchievementCategory.entries.forEach { category ->
                    val (unlocked, total) = viewModel.getCategoryProgress(category)
                    if (total > 0) {
                        CategoryProgressBar(
                            name = category.displayName,
                            unlocked = unlocked,
                            total = total,
                            color = categoryColor(category)
                        )
                    }
                }
            }

            // Tier distribution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementTier.entries.forEach { tier ->
                    val (unlocked, total) = viewModel.getTierProgress(tier)
                    TierCountChip(
                        tier = tier,
                        unlocked = unlocked,
                        total = total,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressBar(
    name: String,
    unlocked: Int,
    total: Int,
    color: Color
) {
    val fraction = if (total > 0) unlocked.toFloat() / total else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }

        Text(
            text = "$unlocked/$total",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TierCountChip(
    tier: AchievementTier,
    unlocked: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = tierColor(tier).copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = tier.displayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = tierColor(tier)
            )
            Text(
                text = "$unlocked/$total",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "全部藏品",
                fontSize = 14.sp,
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

        // Quick filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            // State filter chips
            items(AchievementGalleryFilter.entries) { filter ->
                val isSelected = selectedStateFilter == filter
                FilterChipQuick(
                    label = filter.label,
                    isSelected = isSelected,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onStateFilterClick(filter) }
                )
            }

            // Divider
            item {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }

            // Category chips
            items(AchievementCategory.entries) { category ->
                val isSelected = selectedCategory == category
                FilterChipQuick(
                    label = category.displayName,
                    isSelected = isSelected,
                    color = categoryColor(category),
                    onClick = { onCategoryClick(category) }
                )
            }

            // Divider
            item {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }

            // Tier chips
            items(AchievementTier.entries) { tier ->
                val isSelected = selectedTier == tier
                FilterChipQuick(
                    label = tier.displayName,
                    isSelected = isSelected,
                    color = tierColor(tier),
                    onClick = { onTierClick(tier) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipQuick(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) color.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AchievementCompactCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = card.item
    val context = LocalContext.current
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val imageRes = rememberAchievementImageRes(context, item.def.key)
    val isLocked = !item.isUnlocked
    val tierCol = tierColor(item.def.tier)

    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.tween(100),
        label = "cardPress"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image area with tier-colored border frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            tierCol.copy(alpha = 0.6f),
                            tierCol.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageRes != 0) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = item.def.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback: category color background with icon
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Category + status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category label
            Text(
                text = item.def.category.displayName,
                fontSize = 9.sp,
                color = categoryColor(item.def.category).copy(alpha = 0.8f)
            )

            if (item.isUnlocked || (!isHiddenLocked(item) && item.progressFraction > 0f)) {
                Text(
                    text = " · ",
                    fontSize = 9.sp,
                    color = textSecondary.copy(alpha = 0.4f)
                )
            }

            // Status
            if (item.isUnlocked) {
                Text(
                    text = "已达成",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            } else if (!isHiddenLocked(item) && item.progressFraction > 0f) {
                Text(
                    text = "${(item.progressFraction * 100).toInt()}%",
                    fontSize = 9.sp,
                    color = textSecondary.copy(alpha = 0.6f)
                )
            }
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
                        val tierStats = viewModel.getTierProgress(tier)
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { onTierClick(tier) },
                            label = { Text("${tier.displayName} ${tierStats.first}/${tierStats.second}", fontSize = 12.sp) },
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
