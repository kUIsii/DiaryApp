package com.diary.app.ui.achievement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementTier
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

// ── Color palette ───────────────────────────────────────────

private val TierCommon = Color(0xFF7F909B)
private val TierRare = Color(0xFF5A9BDE)
private val TierEpic = Color(0xFF9B6BCF)
private val TierLegendary = Color(0xFFD8B46A)

private fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.COMMON -> TierCommon
    AchievementTier.RARE -> TierRare
    AchievementTier.EPIC -> TierEpic
    AchievementTier.LEGENDARY -> TierLegendary
}

// ── Main Screen ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val galleryState by viewModel.galleryState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStateFilter by viewModel.selectedStateFilter.collectAsState()
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()

    val recentUnlocks = galleryState.recentUnlocks
    val nearCompletion = galleryState.nearCompletion
    val filteredCards = galleryState.filteredCards

    var showFilters by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("成就", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Compact Overview ──────────────────────────────
                item {
                    CompactOverview(stats = stats, galleryState = galleryState)
                }

                // ── Recent Unlocks ───────────────────────────────
                if (recentUnlocks.isNotEmpty()) {
                    item {
                        SectionTitle("最近达成")
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recentUnlocks, key = { it.def.key }) { item ->
                                RecentCard(item = item, onClick = { viewModel.showAchievementDetail(item) })
                            }
                        }
                    }
                }

                // ── Near Completion ──────────────────────────────
                if (nearCompletion.isNotEmpty()) {
                    item {
                        SectionTitle("即将达成")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(nearCompletion, key = { it.def.key }) { item ->
                        NearCompletionRow(item = item, onClick = { viewModel.showAchievementDetail(item) })
                    }
                }

                // ── Filter toggle ────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFilters = !showFilters }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "全部成就",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ── Filter chips (collapsible) ───────────────────
                item {
                    AnimatedVisibility(
                        visible = showFilters,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AchievementGalleryFilter.entries) { filter ->
                                    FilterChip(
                                        selected = selectedStateFilter == filter,
                                        onClick = { viewModel.selectStateFilter(filter) },
                                        label = { Text(filter.label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF6F5846),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AchievementCategory.entries) { category ->
                                    val catStats = stats.categoryCounts[category] ?: (0 to 0)
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = {
                                            Text(
                                                text = "${category.displayName} ${catStats.first}/${catStats.second}",
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Achievement List (single column) ─────────────
                if (filteredCards.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "这里还没有可展示的条目",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredCards, key = { it.item.def.key }) { card ->
                        AchievementRow(
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

// ── Compact Overview ─────────────────────────────────────────

@Composable
private fun CompactOverview(
    stats: com.diary.app.data.AchievementStats,
    galleryState: AchievementGalleryState
) {
    val hero = galleryState.hero
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        gradientColors = listOf(Color(0xFFF7F1E8), Color(0xFFF0E6D6)),
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hero.headline,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3C2E25),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${stats.unlockedCount}/${stats.totalCount}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8D6E52)
                )
            }
            LinearProgressIndicator(
                progress = hero.completionFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFDDB57A),
                trackColor = Color(0xFFE6DDD2)
            )
            Text(
                text = hero.supportingLine,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF8D6E52).copy(alpha = 0.8f)
            )
        }
    }
}

// ── Section Title ────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ── Recent Unlock Card ──────────────────────────────────────

@Composable
private fun RecentCard(item: AchievementItem, onClick: () -> Unit) {
    val tierCol = tierColor(item.def.tier)
    GlassCard(
        modifier = Modifier
            .size(width = 140.dp, height = 140.dp)
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        gradientColors = listOf(tierCol.copy(alpha = 0.12f), Color(0xFFF7F1E8)),
        innerPadding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                AchievementIcon(
                    category = item.def.category,
                    tier = item.def.tier,
                    isUnlocked = true,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.def.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.def.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Near Completion Row ─────────────────────────────────────

@Composable
private fun NearCompletionRow(item: AchievementItem, onClick: () -> Unit) {
    val tierCol = tierColor(item.def.tier)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tierCol.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            AchievementIcon(
                category = item.def.category,
                tier = item.def.tier,
                isUnlocked = false,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.def.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = item.progressFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = tierCol,
                trackColor = tierCol.copy(alpha = 0.12f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(item.progressFraction * 100).toInt()}%",
            fontSize = 12.sp,
            color = tierCol,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Achievement Row (single column) ─────────────────────────

@Composable
private fun AchievementRow(card: AchievementGalleryCardState, onClick: () -> Unit) {
    val item = card.item
    val isUnlocked = item.isUnlocked
    val tierCol = tierColor(item.def.tier)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isUnlocked) tierCol.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isUnlocked) tierCol.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                AchievementIcon(
                    category = item.def.category,
                    tier = item.def.tier,
                    isUnlocked = true,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                LockIcon(modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = card.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status badge
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tierCol.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "已达成",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = tierCol
                )
            }
        } else if (card.isConcealed) {
            Text(
                text = "隐藏",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            Text(
                text = "${(item.progressFraction * 100).toInt()}%",
                fontSize = 11.sp,
                color = tierCol,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
