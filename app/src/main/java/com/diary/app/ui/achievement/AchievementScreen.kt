package com.diary.app.ui.achievement

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.foundation.layout.WindowInsets
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementStats
import com.diary.app.data.AchievementTier
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

private val TierCommon = Color(0xFF8B8A84)
private val TierRare = Color(0xFF4B7FD1)
private val TierEpic = Color(0xFF8F57C8)
private val TierLegendary = Color(0xFFD79B28)

private fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.COMMON -> TierCommon
    AchievementTier.RARE -> TierRare
    AchievementTier.EPIC -> TierEpic
    AchievementTier.LEGENDARY -> TierLegendary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val galleryState by viewModel.galleryState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedStateFilter by viewModel.selectedStateFilter.collectAsState()
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "成就收藏",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementHeroCard(stats = stats, galleryState = galleryState)
                }

                if (galleryState.recentUnlocks.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AchievementSpotlightSection(
                            title = "最近达成",
                            subtitle = "刚刚收进收藏册的几枚小纪念",
                            items = galleryState.recentUnlocks,
                            onClick = viewModel::showAchievementDetail
                        )
                    }
                }

                if (galleryState.nearCompletion.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AchievementSpotlightSection(
                            title = "即将达成",
                            subtitle = "离解锁只差一点点的条目",
                            items = galleryState.nearCompletion,
                            onClick = viewModel::showAchievementDetail
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterTitle("筛选查看")
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StateFilterRow(
                        selected = selectedStateFilter,
                        onSelected = viewModel::selectStateFilter
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CategoryFilterRow(
                        stats = stats,
                        selected = selectedCategory,
                        onSelected = viewModel::selectCategory
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TierFilterRow(
                        selected = selectedTier,
                        onSelected = viewModel::selectTier
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterTitle("全部藏品")
                }

                if (galleryState.filteredCards.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyGalleryState()
                    }
                } else {
                    items(galleryState.filteredCards, key = { it.item.def.key }) { card ->
                        AchievementGalleryCard(
                            card = card,
                            onClick = { viewModel.showAchievementDetail(card.item) }
                        )
                    }
                }
            }
        }
    }

    selectedAchievement?.let { item ->
        AchievementDetailSheet(
            item = item,
            onDismiss = { viewModel.dismissAchievementDetail() }
        )
    }
}

@Composable
private fun AchievementHeroCard(
    stats: AchievementStats,
    galleryState: AchievementGalleryState
) {
    val hero = galleryState.hero

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        gradientColors = listOf(Color(0xFFF7F0E6), Color(0xFFF0E4D6)),
        innerPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "生活留下的痕迹",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8D6E52)
                    )
                    Text(
                        text = hero.headline,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34261D)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${stats.unlockedCount}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B4B2F)
                        )
                        Text(
                            text = "/ ${stats.totalCount}",
                            fontSize = 11.sp,
                            color = Color(0xFF8D6E52)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = hero.completionFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFFD8A96E),
                trackColor = Color(0xFFE6D9C8)
            )

            Text(
                text = hero.supportingLine,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF8D6E52)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroMetaPill(label = "最近", value = galleryState.recentUnlocks.size.toString())
                HeroMetaPill(label = "接近完成", value = galleryState.nearCompletion.size.toString())
                HeroMetaPill(label = "传说", value = hero.unlockedLegendaryCount.toString())
            }
        }
    }
}

@Composable
private fun HeroMetaPill(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.5f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF8D6E52)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B3424)
            )
        }
    }
}

@Composable
private fun AchievementSpotlightSection(
    title: String,
    subtitle: String,
    items: List<AchievementItem>,
    onClick: (AchievementItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.def.key }) { item ->
                SpotlightCard(item = item, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun SpotlightCard(item: AchievementItem, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.size(width = 154.dp, height = 192.dp),
        cornerRadius = 22.dp,
        innerPadding = 12.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementArtwork(
                achievementKey = item.def.key,
                category = item.def.category,
                tier = item.def.tier,
                isUnlocked = item.isUnlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            )

            Text(
                text = item.def.name,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaDot(label = item.def.category.displayName)
                StatusCapsule(
                    label = buildAchievementStatusLabel(item),
                    color = tierColor(item.def.tier)
                )
            }
        }
    }
}

@Composable
private fun FilterTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateFilterRow(
    selected: AchievementGalleryFilter,
    onSelected: (AchievementGalleryFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AchievementGalleryFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6B5744),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    stats: AchievementStats,
    selected: AchievementCategory?,
    onSelected: (AchievementCategory?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AchievementCategory.entries, key = { it.name }) { category ->
            val categoryStats = stats.categoryCounts[category] ?: (0 to 0)
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                label = {
                    Text(
                        text = "${category.displayName} ${categoryStats.first}/${categoryStats.second}",
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = categoryColor(category).copy(alpha = 0.88f),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TierFilterRow(
    selected: AchievementTier?,
    onSelected: (AchievementTier?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AchievementTier.entries, key = { it.name }) { tier ->
            FilterChip(
                selected = selected == tier,
                onClick = { onSelected(tier) },
                label = { Text(tier.displayName, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tierColor(tier).copy(alpha = 0.88f),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun EmptyGalleryState() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 20.dp
    ) {
        Text(
            text = "这个筛选下暂时还没有藏品，换一个条件看看。",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementGalleryCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit
) {
    val item = card.item
    val badgeColor = tierColor(item.def.tier)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 12.dp,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                AchievementArtwork(
                    achievementKey = item.def.key,
                    category = item.def.category,
                    tier = item.def.tier,
                    isUnlocked = item.isUnlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .align(Alignment.TopStart)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = item.def.tier.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.description,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaDot(label = item.def.category.displayName)
                StatusCapsule(label = card.statusLabel, color = badgeColor)
            }

            if (!item.isUnlocked && !card.isConcealed) {
                LinearProgressIndicator(
                    progress = item.progressFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = badgeColor,
                    trackColor = badgeColor.copy(alpha = 0.14f)
                )
            }
        }
    }
}

@Composable
private fun MetaDot(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFB48B5D))
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusCapsule(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
