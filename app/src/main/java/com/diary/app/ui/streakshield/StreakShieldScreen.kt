package com.diary.app.ui.streakshield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.AnimatedCounter
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakShieldScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StreakShieldViewModel = viewModel()
) {
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val monthGrid by viewModel.monthGrid.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val history by viewModel.history.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val autoProtectEnabled by viewModel.autoProtectEnabled.collectAsState()
    val notifyEnabled by viewModel.notifyEnabled.collectAsState()

    var showUseDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf("") }

    val usableCount = inventory.count { !it.isUsed }
    val totalCount = inventory.size

    GradientBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.SpacingLg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
        ) {
            item {
                PageHeader(title = "连续保护罩", onNavigateBack = onNavigateBack)
            }

            item {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                StreakHeader(currentStreak, longestStreak)
            }

            item {
                MonthGrid(monthGrid)
            }

            item {
                InventoryCard(
                    inventory = inventory,
                    usableCount = usableCount,
                    totalCount = totalCount,
                    onUseShield = { itemId ->
                        selectedItemId = itemId
                        showUseDialog = true
                    }
                )
            }

            item {
                SettingsSection(
                    autoProtectEnabled = autoProtectEnabled,
                    notifyEnabled = notifyEnabled,
                    hasUsableShield = usableCount > 0,
                    onToggleAutoProtect = { viewModel.toggleAutoProtect(it) },
                    onToggleNotify = { viewModel.toggleNotify(it) },
                    onManualUse = {
                        val available = inventory.firstOrNull { !it.isUsed }
                        if (available != null) {
                            selectedItemId = available.id
                            showUseDialog = true
                        }
                    }
                )
            }

            if (history.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Default.Shield,
                            title = "暂无使用记录",
                            subtitle = "你还没有使用过保护罩，继续保持每日写作的好习惯！"
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "使用历史",
                        fontSize = DesignTokens.FontMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
                    )
                }
                items(history) { group ->
                    HistoryGroupCard(group)
                }
            }

            item {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(
                    text = "成就",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                AchievementSection(achievements)
            }

            item {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            }
        }
    }

    if (showUseDialog) {
        AlertDialog(
            onDismissRequest = { showUseDialog = false },
            title = { Text("使用保护罩") },
            text = { Text("确定使用保护罩保护今天的写作吗？使用后当天即使忘记写日记也不会断签。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.useShield(selectedItemId)
                    showUseDialog = false
                }) {
                    Text("确定使用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUseDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StreakHeader(currentStreak: Int, longestStreak: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDD25", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                AnimatedCounter(
                    targetValue = currentStreak,
                    suffix = " 天",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = "已连续写作 $currentStreak 天",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = "最长连续：$longestStreak 天",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(grid: List<Pair<Int?, Boolean>>) {
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "本月写作日历",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val cell = grid.getOrNull(index)
                        val day = cell?.first
                        val hasEntry = cell?.second == true
                        val cellColor by animateColorAsState(
                            targetValue = when {
                                day == null -> Color.Transparent
                                hasEntry -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            },
                            animationSpec = tween(300),
                            label = "cellColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .then(
                                    if (day != null) {
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cellColor)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 11.sp,
                                    color = if (hasEntry) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(
    inventory: List<ShieldItem>,
    usableCount: Int,
    totalCount: Int,
    onUseShield: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "保护罩库存",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                val displayItems = inventory.take(6)
                for (item in displayItems) {
                    val isUsable = !item.isUsed
                    val tint = if (isUsable) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    val onClickModifier = if (isUsable) {
                        Modifier.clickable { onUseShield(item.id) }
                    } else Modifier
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isUsable) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                            .then(
                                if (isUsable) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .then(onClickModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                val emptySlots = 6 - displayItems.size
                for (i in 0 until emptySlots) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = "可用 $usableCount / 共 $totalCount",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(
    autoProtectEnabled: Boolean,
    notifyEnabled: Boolean,
    hasUsableShield: Boolean,
    onToggleAutoProtect: (Boolean) -> Unit,
    onToggleNotify: (Boolean) -> Unit,
    onManualUse: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "设置",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "自动保护", fontSize = DesignTokens.FontBody)
                Switch(
                    checked = autoProtectEnabled,
                    onCheckedChange = onToggleAutoProtect
                )
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "通知提醒", fontSize = DesignTokens.FontBody)
                Switch(
                    checked = notifyEnabled,
                    onCheckedChange = onToggleNotify
                )
            }
            if (hasUsableShield) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Button(
                    onClick = onManualUse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(DesignTokens.CornerLarge)
                ) {
                    Text("手动使用保护罩")
                }
            }
        }
    }
}

@Composable
private fun HistoryGroupCard(group: ShieldHistoryGroup) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            val displayMonth = try {
                val parts = group.month.split("-")
                "${parts[0]}年${parts[1].toInt()}月"
            } catch (e: Exception) {
                group.month
            }
            Text(
                text = displayMonth,
                fontSize = DesignTokens.FontSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            group.items.forEach { item ->
                HistoryItemRow(item)
                if (item != group.items.last()) {
                    Divider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingSm),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(item: ShieldItem) {
    val usedDate = item.usedAt?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usedDate?.format(DateTimeFormatter.ofPattern("MM月dd日")) ?: "",
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (item.source) {
                    "achievement" -> "成就奖励"
                    else -> "月度发放"
                } + " · " + when (item.triggerType) {
                    "auto" -> "自动触发"
                    "manual" -> "手动使用"
                    else -> ""
                },
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchievementSection(achievements: List<ShieldAchievement>) {
    val allAchievements = listOf(
        ShieldAchievement("first_use", "初次守护", "第一次使用保护罩"),
        ShieldAchievement("three_uses", "守护达人", "累计使用3次保护罩"),
        ShieldAchievement("six_shields", "全副武装", "获得6个保护罩"),
        ShieldAchievement("streak_30", "自强不息", "连续30天无需使用保护罩")
    )
    val unlockedIds = achievements.map { it.id }.toSet()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            allAchievements.forEach { ach ->
                val unlocked = unlockedIds.contains(ach.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ach.title,
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium,
                            color = if (unlocked) Color.Unspecified
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = ach.description,
                            fontSize = DesignTokens.FontSmall,
                            color = if (unlocked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    if (unlocked) {
                        Text(
                            text = "已解锁",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "未解锁",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
                if (ach != allAchievements.last()) {
                    Divider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingXs),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                }
            }
        }
    }
}
