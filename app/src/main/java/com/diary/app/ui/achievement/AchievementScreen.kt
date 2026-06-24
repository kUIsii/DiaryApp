package com.diary.app.ui.achievement

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementItem
import com.diary.app.data.AchievementTier
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.nurturing.NurturingJourneyCard
import com.diary.app.ui.nurturing.achievementArtRes
import com.diary.app.ui.nurturing.buildNurturingJourneyState
import com.diary.app.ui.nurturing.buildAchievementVisualState

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
    onNavigateBack: () -> Unit,
    onNavigateToPet: () -> Unit = {},
    onNavigateToIsland: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()
    val petState by CrossSystemManager.petState.collectAsState()
    val islandLevel by CrossSystemManager.islandLevel.collectAsState()
    val recentAchievementUnlock by CrossSystemManager.recentAchievementUnlock.collectAsState()
    val activeRareDiscoveryCount by CrossSystemManager.activeRareDiscoveryCount.collectAsState()
    val petStreakDays by CrossSystemManager.petStreakDays.collectAsState()
    val legendaryUnlocked = filteredItems.filter { it.isUnlocked && it.def.tier == AchievementTier.LEGENDARY }
    val recentUnlocked = filteredItems.filter { it.isUnlocked }.take(5)
    val nextMilestones = filteredItems.filterNot { it.isUnlocked }.sortedByDescending { it.progressFraction }.take(3)
    val achievementVisualState = remember(
        stats.unlockedCount,
        stats.totalCount,
        legendaryUnlocked.size,
        nextMilestones.firstOrNull()?.def?.name,
        petState,
        islandLevel
    ) {
        buildAchievementVisualState(
            unlockedCount = stats.unlockedCount,
            totalCount = stats.totalCount,
            legendaryUnlockedCount = legendaryUnlocked.size,
            nearMilestoneName = nextMilestones.firstOrNull()?.def?.name,
            petState = petState,
            islandLevel = islandLevel
        )
    }
    val journeyState = remember(
        petState,
        islandLevel,
        recentAchievementUnlock,
        activeRareDiscoveryCount,
        nextMilestones.firstOrNull()?.def?.name,
        petStreakDays
    ) {
        buildNurturingJourneyState(
            petState = petState,
            islandLevel = islandLevel,
            recentAchievementUnlock = recentAchievementUnlock,
            hasRareDiscovery = activeRareDiscoveryCount > 0,
            nearMilestoneName = nextMilestones.firstOrNull()?.def?.name,
            streakDays = petStreakDays
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("成就殿堂", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "私人收藏馆 · ${stats.unlockedCount}/${stats.totalCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
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
                            Color(0xFF2D3427),
                            Color(0xFF284154),
                            Color(0xFF5B4E39)
                        ),
                        innerPadding = 20.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "珍藏总览",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE0CC9B),
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = achievementVisualState.heroLine,
                                        fontSize = 23.sp,
                                        lineHeight = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF4F0E6)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1AFFF2CB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFF8E6BA),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0x18FFF8ED))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = achievementArtRes(achievementVisualState.artKey)),
                                    contentDescription = "成就馆珍藏预览",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(18.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "收集进度",
                                        fontSize = 13.sp,
                                        color = Color(0xFFE0CC9B)
                                    )
                                    Text(
                                        text = "${stats.unlockedCount} / ${stats.totalCount}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFF4F0E6),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = if (stats.totalCount > 0) stats.unlockedCount.toFloat() / stats.totalCount else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFD6B572),
                                    trackColor = Color(0x26FFFFFF)
                                )
                                Text(
                                    text = if (legendaryUnlocked.isEmpty()) "下一步先去追逐稀有与传说展台。" else "你已经拥有 ${legendaryUnlocked.size} 件传说级藏品。",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE3DCCD).copy(alpha = 0.84f)
                                )
                            }

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                innerPadding = 14.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "策展建议",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE0CC9B)
                                    )
                                    Text(
                                        text = achievementVisualState.nextActionHint,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "已解锁",
                                    value = "${stats.unlockedCount}",
                                    subtitle = "正在展出",
                                    color = Color(0xFFD6B572)
                                )
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "传说级",
                                    value = "${legendaryUnlocked.size}",
                                    subtitle = "镇馆之物",
                                    color = Color(0xFFE8C97A)
                                )
                                MuseumStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "待发现",
                                    value = "${(stats.totalCount - stats.unlockedCount).coerceAtLeast(0)}",
                                    subtitle = "仍可追逐",
                                    color = Color(0xFFA8C5E5)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AchievementCategory.entries) { cat ->
                                val catStats = stats.categoryCounts[cat] ?: (0 to 0)
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = {
                                        Text(
                                            text = "${cat.displayName} ${catStats.first}/${catStats.second}",
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
                                val isSelected = selectedTier == tier
                                val tierCol = tierColor(tier)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTier(tier) },
                                    label = {
                                        Text(
                                            text = "${tier.displayName} ${tier.stars}",
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tierCol,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (recentUnlocked.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            innerPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "最近入藏",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(recentUnlocked, key = { it.def.key }) { item ->
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

                if (nextMilestones.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            innerPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "接近完成",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                nextMilestones.forEach { item ->
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
                    NurturingJourneyCard(
                        state = journeyState,
                        title = "拿到收藏以后",
                        onOpenPet = onNavigateToPet,
                        onOpenIsland = onNavigateToIsland,
                        onOpenAchievement = {}
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        innerPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "徽章陈列柜",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "这里不是任务清单，而是一座把你的坚持、情绪与发现收起来的私人博物馆。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.height(680.dp),
                                contentPadding = PaddingValues(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems, key = { it.def.key }) { item ->
                                    AchievementDisplayCard(
                                        item = item,
                                        onClick = { viewModel.showAchievementDetail(item) }
                                    )
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
            .size(width = 148.dp, height = 168.dp)
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
        gradientColors = listOf(
            tierCol.copy(alpha = 0.22f),
            tierCol.copy(alpha = 0.08f)
        ),
        innerPadding = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                AchievementIcon(
                    category = item.def.category,
                    tier = item.def.tier,
                    isUnlocked = true,
                    modifier = Modifier.size(32.dp)
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
    item: AchievementItem,
    onClick: () -> Unit
) {
    val isUnlocked = item.isUnlocked
    val tierCol = tierColor(item.def.tier)
    val cabinetBrush = if (isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(
                tierCol.copy(alpha = 0.22f),
                Color(0xFFF4EFE5).copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    .background(Color.White.copy(alpha = if (isUnlocked) 0.18f else 0.08f)),
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
                    text = if (item.isHiddenLocked) "未公开藏品" else item.def.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (item.isHiddenLocked) "完成特定条件后才会显露真面目" else item.def.description,
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
                    text = "${item.state.progress} / ${item.def.target}",
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
                        text = "${item.def.tier.displayName}藏品",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = tierCol
                    )
                }
            }
        }
    }
}
