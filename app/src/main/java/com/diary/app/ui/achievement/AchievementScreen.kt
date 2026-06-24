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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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

private val TierCommon = Color(0xFF78909C)
private val TierRare = Color(0xFF42A5F5)
private val TierEpic = Color(0xFFAB47BC)
private val TierLegendary = Color(0xFFFFC107)

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
    val filteredItems by viewModel.filteredItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(text = "\u6210\u5C31\u6BBF\u5802", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "\u8FD4\u56DE")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                cornerRadius = 16.dp, innerPadding = 16.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "\u5DF2\u89E3\u9501", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${stats.unlockedCount} / ${stats.totalCount}",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = if (stats.totalCount > 0) stats.unlockedCount.toFloat() / stats.totalCount else 0f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AchievementCategory.entries) { cat ->
                    val catStats = stats.categoryCounts[cat] ?: (0 to 0)
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected, onClick = { viewModel.selectCategory(cat) },
                        label = { Text(text = "${cat.iconEmoji} ${cat.displayName}", fontSize = 13.sp) },
                        trailingIcon = {
                            Text(
                                text = "${catStats.first}/${catStats.second}", fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AchievementTier.entries) { tier ->
                    val isSelected = selectedTier == tier
                    val tierCol = tierColor(tier)
                    FilterChip(
                        selected = isSelected, onClick = { viewModel.selectTier(tier) },
                        label = { Text(text = "${tier.displayName} ${tier.stars}", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tierCol, selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.def.key }) { item ->
                    AchievementCard(item = item, onClick = { viewModel.showAchievementDetail(item) })
                }
            }
        }
    }

    selectedAchievement?.let { item ->
        AchievementDetailSheet(item = item, onDismiss = { viewModel.dismissAchievementDetail() })
    }
}

@Composable
private fun AchievementCard(item: AchievementItem, onClick: () -> Unit) {
    val isUnlocked = item.isUnlocked
    val tierCol = tierColor(item.def.tier)

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 14.dp, innerPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked) Brush.linearGradient(listOf(tierCol.copy(alpha = 0.2f), tierCol.copy(alpha = 0.1f)))
                        else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isUnlocked) item.def.iconEmoji else "\uD83D\uDD12", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (item.isHiddenLocked) "???" else item.def.name,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (item.isHiddenLocked) "\u5B8C\u6210\u7279\u5B9A\u6761\u4EF6\u89E3\u9501" else item.def.description,
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!isUnlocked) {
                LinearProgressIndicator(
                    progress = item.progressFraction,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = tierCol.copy(alpha = 0.6f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${item.state.progress} / ${item.def.target}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                Text(text = "\u5DF2\u89E3\u9501", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tierCol)
            }
        }
    }
}
