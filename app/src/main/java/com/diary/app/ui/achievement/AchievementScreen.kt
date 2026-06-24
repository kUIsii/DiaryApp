package com.diary.app.ui.achievement

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Star
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
import kotlin.math.max

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

    val hero = galleryState.hero
    val recentUnlocks = galleryState.recentUnlocks
    val nearCompletion = galleryState.nearCompletion
    val filteredCards = galleryState.filteredCards
    val gridRows = max((filteredCards.size + 1) / 2, 1)
    val gridHeight = (gridRows * 212).dp

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("生活痕迹收藏册", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "把写过的日子整理成可以回望的目录",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
                        )
                    }
                },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 30.dp,
                        enableShadow = true,
                        gradientColors = listOf(
                            Color(0xFFF7F1E8),
                            Color(0xFFF0E6D6),
                            Color(0xFFE6DDD2)
                        ),
                        innerPadding = 20.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "总览",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8D6E52),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = hero.headline,
                                        fontSize = 24.sp,
                                        lineHeight = 31.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3C2E25)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = hero.supportingLine,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF625244)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFE9D8BF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CollectionsBookmark,
                                        contentDescription = null,
                                        tint = Color(0xFF7A5C3E),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF5C4A3E), Color(0xFF7A6251))
                                        )
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "收录进度",
                                        fontSize = 13.sp,
                                        color = Color(0xFFF2E5D4)
                                    )
                                    Text(
                                        text = "${stats.unlockedCount} / ${stats.totalCount}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = hero.completionFraction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFDDB57A),
                                    trackColor = Color.White.copy(alpha = 0.18f)
                                )
                                Text(
                                    text = if (hero.unlockedLegendaryCount > 0) {
                                        "你已经收进 ${hero.unlockedLegendaryCount} 条传说级生活痕迹。"
                                    } else {
                                        "收藏册还在变厚，下一条值得记住的痕迹正在路上。"
                                    },
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFF5EBDD).copy(alpha = 0.9f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "已收录",
                                    value = stats.unlockedCount.toString(),
                                    subtitle = "可随时翻看",
                                    color = Color(0xFFB9823B)
                                )
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "接近完成",
                                    value = nearCompletion.size.toString(),
                                    subtitle = "已有眉目",
                                    color = Color(0xFF7E8FA8)
                                )
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "隐藏线索",
                                    value = viewModel.getHiddenAchievementCount().toString(),
                                    subtitle = "仍待显影",
                                    color = Color(0xFF8F7796)
                                )
                            }
                        }
                    }
                }

                if (recentUnlocks.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            innerPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionHeader(
                                    title = "最近达成",
                                    subtitle = "最近几次被收进行囊的生活痕迹"
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(recentUnlocks, key = { it.def.key }) { item ->
                                        RecentShowcaseCard(
                                            item = item,
                                            onClick = { viewModel.showAchievementDetail(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (nearCompletion.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            innerPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionHeader(
                                    title = "接近完成",
                                    subtitle = "这些线索已经有了形状，再写几次就能归档"
                                )
                                nearCompletion.forEach { item ->
                                    UpcomingMilestoneRow(
                                        item = item,
                                        onClick = { viewModel.showAchievementDetail(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "筛选",
                                subtitle = "按分类、状态和层级整理这本收藏册"
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AchievementGalleryFilter.entries) { filter ->
                                    FilterChip(
                                        selected = selectedStateFilter == filter,
                                        onClick = { viewModel.selectStateFilter(filter) },
                                        label = { Text(filter.label, fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF6F5846),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AchievementCategory.entries) { category ->
                                    val catStats = stats.categoryCounts[category] ?: (0 to 0)
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = {
                                            Text(
                                                text = "${category.displayName} ${catStats.first}/${catStats.second}",
                                                fontSize = 13.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AchievementTier.entries) { tier ->
                                    val tierCol = tierColor(tier)
                                    FilterChip(
                                        selected = selectedTier == tier,
                                        onClick = { viewModel.selectTier(tier) },
                                        label = { Text(tier.displayName, fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = tierCol,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "收藏册网格",
                                subtitle = when (selectedStateFilter) {
                                    AchievementGalleryFilter.ALL -> "完整翻看所有已显露的收藏条目"
                                    AchievementGalleryFilter.UNLOCKED -> "只看已经真正收入册中的内容"
                                    AchievementGalleryFilter.NEAR_COMPLETION -> "优先查看进展最靠前的条目"
                                    AchievementGalleryFilter.HIDDEN -> "保留神秘感的隐藏线索"
                                }
                            )

                            if (filteredCards.isEmpty()) {
                                EmptyGalleryCard(selectedStateFilter = selectedStateFilter)
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.height(gridHeight),
                                    contentPadding = PaddingValues(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredCards, key = { it.item.def.key }) { card ->
                                        AchievementDisplayCard(
                                            card = card,
                                            onClick = { viewModel.showAchievementDetail(card.item) }
                                        )
                                    }
                                }
                            }
                        }
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
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
        )
    }
}

@Composable
private fun EmptyGalleryCard(selectedStateFilter: AchievementGalleryFilter) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        gradientColors = listOf(
            Color(0xFFF5EFE6),
            Color(0xFFEEE5D8)
        ),
        innerPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE1D3C0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7B6755)
                )
            }
            Text(
                text = when (selectedStateFilter) {
                    AchievementGalleryFilter.ALL -> "这里还没有可展示的条目"
                    AchievementGalleryFilter.UNLOCKED -> "还没有解锁成就被收入收藏册"
                    AchievementGalleryFilter.NEAR_COMPLETION -> "目前没有足够接近完成的条目"
                    AchievementGalleryFilter.HIDDEN -> "隐藏线索暂时还没有露面"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "继续记录、回顾和整理，新的页签会慢慢出现。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MuseumStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RecentShowcaseCard(
    item: AchievementItem,
    onClick: () -> Unit
) {
    val tierCol = tierColor(item.def.tier)
    GlassCard(
        modifier = Modifier
            .size(width = 156.dp, height = 176.dp)
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
        gradientColors = listOf(
            tierCol.copy(alpha = 0.18f),
            Color(0xFFF7F1E8)
        ),
        innerPadding = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center
            ) {
                AchievementIcon(
                    category = item.def.category,
                    tier = item.def.tier,
                    isUnlocked = true,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.def.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.def.flavorText.ifBlank { item.def.description },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UpcomingMilestoneRow(
    item: AchievementItem,
    onClick: () -> Unit
) {
    val tierCol = tierColor(item.def.tier)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tierCol.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = tierCol,
                    modifier = Modifier.size(18.dp)
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
                Text(
                    text = item.def.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${(item.progressFraction * 100).toInt()}%",
                fontSize = 12.sp,
                color = tierCol,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = item.progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = tierCol,
            trackColor = tierCol.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun AchievementDisplayCard(
    card: AchievementGalleryCardState,
    onClick: () -> Unit
) {
    val item = card.item
    val isUnlocked = item.isUnlocked
    val tierCol = tierColor(item.def.tier)
    val cabinetBrush = if (isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(
                tierCol.copy(alpha = 0.22f),
                Color(0xFFF4EFE5).copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
        innerPadding = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cabinetBrush)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (isUnlocked) 0.2f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    AchievementIcon(
                        category = item.def.category,
                        tier = item.def.tier,
                        isUnlocked = true,
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    LockIcon(modifier = Modifier.size(30.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.description,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isUnlocked) {
                LinearProgressIndicator(
                    progress = item.progressFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = tierCol.copy(alpha = 0.75f),
                    trackColor = tierCol.copy(alpha = 0.14f)
                )
                Text(
                    text = if (card.isConcealed) {
                        "隐藏线索"
                    } else {
                        "${item.state.progress} / ${item.def.target}"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tierCol.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${item.def.tier.displayName}收藏",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = tierCol
                    )
                }
            }
        }
    }
}
