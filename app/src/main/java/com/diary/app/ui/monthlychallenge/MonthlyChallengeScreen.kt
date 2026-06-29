package com.diary.app.ui.monthlychallenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyChallengeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MonthlyChallengeViewModel = viewModel()
) {
    val currentChallenge by viewModel.currentChallenge.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val challengeTemplates by viewModel.challengeTemplates.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val badgeRecords by viewModel.badgeRecords.collectAsState()
    val showCelebration by viewModel.showCelebration.collectAsState()
    val latestBadge by viewModel.latestBadge.collectAsState()
    val consecutiveMissedDays by viewModel.consecutiveMissedDays.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val now = LocalDate.now()
    val isCurrentMonth = selectedYear == now.year && selectedMonth == now.monthValue
    val completedDays = dailyLogs.filter { it.completed }.map { it.date }.toSet()
    var showBadgeSheet by remember { mutableStateOf(false) }

    val selTemplate = selectedTemplate
    if (showConfirmDialog && selTemplate != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text("选择挑战", fontWeight = FontWeight.Bold) },
            text = { Text("确定选择「${selTemplate.title}」吗？选定后本月不可更换。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSelectChallenge() }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    val badge = latestBadge
    if (showCelebration && badge != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCelebration() },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (badge.badgeType) {
                            "gold" -> Color(0xFFFFD700)
                            "silver" -> Color(0xFFC0C0C0)
                            else -> Color(0xFFCD7F32)
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text("恭喜解锁新成就！", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "你已获得「${badge.title}」！",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCelebration() }) {
                    Text("太棒了")
                }
            }
        )
    }

    if (showBadgeSheet) {
        ModalBottomSheet(onDismissRequest = { showBadgeSheet = false }) {
            Column(modifier = Modifier.padding(DesignTokens.SpacingLg)) {
                Text("成就徽章", fontSize = DesignTokens.FontTitle, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                if (badgeRecords.isEmpty()) {
                    Text("还没有获得任何成就徽章", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    badgeRecords.sortedByDescending { it.earnedAt }.forEach { badge ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = DesignTokens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeColor = when (badge.badgeType) {
                                "gold" -> Color(0xFFFFD700)
                                "silver" -> Color(0xFFC0C0C0)
                                else -> Color(0xFFCD7F32)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                            Column {
                                Text(badge.title, fontWeight = FontWeight.Medium, fontSize = DesignTokens.FontBody)
                                Text(
                                    "${java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.CHINESE).format(java.util.Date(badge.earnedAt))}",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
            }
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "月度挑战",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.goToCurrentMonth() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "返回本月")
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMonth(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上月")
                }
                Text(
                    text = "${selectedYear}年${selectedMonth}月",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 100.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { viewModel.changeMonth(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下月")
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            when {
                isLoading -> LoadingState()
                isCurrentMonth && currentChallenge == null && challengeTemplates != null -> {
                    ChallengeSelectionSection(
                        templates = challengeTemplates!!,
                        onSelect = { viewModel.showSelectChallengeDialog(it) }
                    )
                }
                currentChallenge != null -> {
                    val challenge = currentChallenge!!
                    if (consecutiveMissedDays >= 3 && isCurrentMonth) {
                        MissedDaysBanner(missedDays = consecutiveMissedDays)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                Text(
                                    text = "${challenge.month}月挑战",
                                    fontSize = DesignTokens.FontMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Text(
                                text = challenge.title,
                                fontSize = DesignTokens.FontTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                            Text(
                                text = challenge.description,
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            LinearProgressIndicator(
                                progress = challenge.completedDays.toFloat() / challenge.targetDays.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = "${challenge.completedDays}/${challenge.targetDays} 天 · ${((challenge.completedDays.toFloat() / challenge.targetDays) * 100).toInt()}% 完成",
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            ChallengeStatsRow(stats = stats)
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            ChallengeHeatmapGrid(
                                completedDays = completedDays,
                                year = selectedYear,
                                month = selectedMonth,
                                modifier = Modifier.heightIn(max = 60.dp)
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Divider()
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            BadgeRow(
                                currentChallengeId = challenge.id,
                                badgeRecords = badgeRecords,
                                progress = challenge.completedDays.toFloat() / challenge.targetDays.toFloat(),
                                onBadgeClick = { showBadgeSheet = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "完成记录",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            ChallengeCalendarGrid(
                                completedDays = completedDays,
                                onDayClick = { if (isCurrentMonth) viewModel.toggleDay(it) },
                                readOnly = !isCurrentMonth
                            )
                        }
                    }
                }
                else -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("该月无挑战记录", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)) {
        repeat(3) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                            RoundedCornerShape(DesignTokens.CornerMedium)
                        )
                )
            }
        }
    }
}

@Composable
private fun ChallengeSelectionSection(
    templates: List<ChallengeTemplate>,
    onSelect: (ChallengeTemplate) -> Unit
) {
    Text(
        text = "选择本月的挑战",
        fontSize = DesignTokens.FontMedium,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
    templates.forEach { template ->
        ChallengeTemplateCard(template = template, onClick = { onSelect(template) })
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
    }
}

@Composable
private fun ChallengeTemplateCard(
    template: ChallengeTemplate,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            Text(
                text = template.title,
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = template.description,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Row {
                val chipColor = MaterialTheme.colorScheme.primaryContainer
                val chipTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                ChallengeChip(text = template.category, containerColor = chipColor, contentColor = chipTextColor)
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                ChallengeChip(text = template.difficulty, containerColor = chipColor, contentColor = chipTextColor)
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                ChallengeChip(text = "${template.targetDays}天", containerColor = chipColor, contentColor = chipTextColor)
            }
        }
    }
}

@Composable
private fun ChallengeChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(DesignTokens.CornerSmall),
        color = containerColor
    ) {
        Text(
            text = text,
            fontSize = DesignTokens.FontSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingSm, vertical = DesignTokens.SpacingXs)
        )
    }
}

@Composable
private fun ChallengeStatsRow(stats: ChallengeStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "当前连续", value = "${stats.currentStreak}天")
        StatItem(label = "最长连续", value = "${stats.longestStreak}天")
        StatItem(label = "剩余天数", value = "${stats.remainingDays}天")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChallengeHeatmapGrid(
    completedDays: Set<Long>,
    year: Int,
    month: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(modifier = modifier.fillMaxWidth()) {
        val now = LocalDate.of(year, month, 1)
        val daysInMonth = now.lengthOfMonth()
        val firstDayOfWeek = now.dayOfWeek.value % 7

        val rows = 5
        val cols = 7
        val spacing = 4.dp.toPx()
        val dotRadius = 3.5.dp.toPx()
        val cellWidth = (size.width - spacing * (cols - 1)) / cols
        val cellHeight = (size.height - spacing * (rows - 1)) / rows

        for (week in 0 until rows) {
            for (dayOfWeek in 0 until cols) {
                val day = week * cols + dayOfWeek - firstDayOfWeek + 1
                if (day in 1..daysInMonth) {
                    val date = now.withDayOfMonth(day)
                    val isCompleted = completedDays.any {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate() == date
                    }
                    val cx = dayOfWeek * (cellWidth + spacing) + cellWidth / 2
                    val cy = week * (cellHeight + spacing) + cellHeight / 2
                    drawCircle(
                        color = if (isCompleted) primaryColor else surfaceVariant,
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeRow(
    currentChallengeId: Long,
    badgeRecords: List<BadgeRecord>,
    progress: Float,
    onBadgeClick: () -> Unit
) {
    val challengeBadges = badgeRecords.filter { it.challengeId == currentChallengeId }.map { it.badgeType }.toSet()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBadgeClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgeDot(type = "bronze", unlocked = progress >= 0.6f || "bronze" in challengeBadges)
        Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
        BadgeDot(type = "silver", unlocked = progress >= 0.8f || "silver" in challengeBadges)
        Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
        BadgeDot(type = "gold", unlocked = progress >= 1.0f || "gold" in challengeBadges)
    }
}

@Composable
private fun BadgeDot(type: String, unlocked: Boolean) {
    val bgColor = when (type) {
        "gold" -> if (unlocked) Color(0xFFFFD700) else Color(0xFFFFD700).copy(alpha = 0.25f)
        "silver" -> if (unlocked) Color(0xFFC0C0C0) else Color(0xFFC0C0C0).copy(alpha = 0.25f)
        else -> if (unlocked) Color(0xFFCD7F32) else Color(0xFFCD7F32).copy(alpha = 0.25f)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (unlocked) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        val label = when (type) {
            "gold" -> "金牌"
            "silver" -> "银牌"
            else -> "铜牌"
        }
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MissedDaysBanner(missedDays: Int) {
    AnimatedVisibility(visible = true) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DesignTokens.CornerSmall),
            color = Color(0xFFFFF3CD)
        ) {
            Row(
                modifier = Modifier.padding(DesignTokens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFF856404),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                Text(
                    text = "你已经连续 $missedDays 天未完成挑战了，加油！",
                    fontSize = DesignTokens.FontSmall,
                    color = Color(0xFF856404)
                )
            }
        }
    }
}

@Composable
private fun ChallengeCalendarGrid(
    completedDays: Set<Long>,
    onDayClick: (Long) -> Unit,
    readOnly: Boolean = false
) {
    val now = LocalDate.now()
    val daysInMonth = now.lengthOfMonth()
    val firstDayOfWeek = now.withDayOfMonth(1).dayOfWeek.value % 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        var dayCounter = 1
        for (week in 0 until 6) {
            if (dayCounter > daysInMonth) break
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0 until 7) {
                    if ((week == 0 && dayOfWeek < firstDayOfWeek) || dayCounter > daysInMonth) {
                        Spacer(modifier = Modifier.width(36.dp).height(36.dp))
                    } else {
                        val day = dayCounter
                        val dateMillis = now.withDayOfMonth(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val isCompleted = completedDays.any {
                            java.time.Instant.ofEpochMilli(it)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate() == now.withDayOfMonth(day)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = if (!readOnly) Modifier.clickable { onDayClick(dateMillis) } else Modifier
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = DesignTokens.FontSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = day.toString(),
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = if (!readOnly) Modifier.clickable { onDayClick(dateMillis) } else Modifier
                                )
                            }
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { onClick() })
    }
)
