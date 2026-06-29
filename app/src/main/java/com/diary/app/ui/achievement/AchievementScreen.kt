package com.diary.app.ui.achievement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToMonthlyChallenge: () -> Unit = {},
    onNavigateToStreakShield: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val galleryState by viewModel.galleryState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedStateFilter by viewModel.selectedStateFilter.collectAsState()

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "成就收藏",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feature entry cards
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementFeatureCards(
                        onNavigateToMonthlyChallenge = onNavigateToMonthlyChallenge,
                        onNavigateToStreakShield = onNavigateToStreakShield
                    )
                }

                // Overview card
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AchievementOverviewCard(stats = stats, galleryState = galleryState)
                }

                // State filter chips
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AchievementGalleryFilter.entries.forEach { filter ->
                            val isSelected = selectedStateFilter == filter
                            Surface(
                                onClick = { viewModel.selectStateFilter(filter) },
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
                    }
                }

                // Category filter chips
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AchievementCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                onClick = { viewModel.selectCategory(cat) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) categoryColor(cat).copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = cat.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) categoryColor(cat) else textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Tier filter chips
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AchievementTier.entries.forEach { tier ->
                            val isSelected = selectedTier == tier
                            Surface(
                                onClick = { viewModel.selectTier(tier) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) tierColor(tier).copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "${tier.displayName} ${tier.stars}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) tierColor(tier) else textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
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
}

@Composable
private fun AchievementFeatureCards(
    onNavigateToMonthlyChallenge: () -> Unit,
    onNavigateToStreakShield: () -> Unit
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToMonthlyChallenge),
            cornerRadius = 16.dp,
            innerPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "月度挑战",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "每月新目标",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }
        }

        GlassCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToStreakShield),
            cornerRadius = 16.dp,
            innerPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "连续保护罩",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "保持记录习惯",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }
        }
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
private fun AchievementCompactCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit
) {
    val item = card.item
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AchievementBadge(
            achievementKey = item.def.key,
            category = item.def.category,
            tier = item.def.tier,
            unlocked = item.isUnlocked,
            modifier = Modifier.size(72.dp),
            size = 72
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = if (isHiddenLocked(item)) "???" else item.def.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )

        if (item.isUnlocked) {
            Text(
                text = "已达成",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        } else if (!isHiddenLocked(item) && item.progressFraction > 0f) {
            Text(
                text = "${(item.progressFraction * 100).toInt()}%",
                fontSize = 8.sp,
                color = textSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

private fun isHiddenLocked(item: com.diary.app.data.AchievementItem): Boolean {
    return item.def.isHidden && !item.isUnlocked
}
