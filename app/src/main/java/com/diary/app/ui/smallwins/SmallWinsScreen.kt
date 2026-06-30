package com.diary.app.ui.smallwins

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.SmallWin
import com.diary.app.ui.components.EmptyState
import kotlinx.coroutines.delay
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.smallwins.SmallWinsViewModel.Companion.CATEGORIES
import com.diary.app.ui.smallwins.SmallWinsViewModel.Companion.categoryDisplayName
import com.diary.app.ui.smallwins.SmallWinsViewModel.Companion.matchesCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SmallWinsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SmallWinsViewModel = viewModel()
) {
    val todaySmallWins by viewModel.todaySmallWins.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val historyWins by viewModel.historyWins.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val writingBridgeSeed by viewModel.writingBridgeSeed.collectAsState()

    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf(0L) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editWinId by remember { mutableStateOf(0L) }
    var editWinContent by remember { mutableStateOf("") }
    var showWritingBridgeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            viewModel.refreshToday()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", fontSize = 16.sp) },
            text = { Text("确定删除此记录？", fontSize = 14.sp, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSmallWin(pendingDeleteId)
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", fontSize = 14.sp)
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑小确幸", fontSize = 16.sp) },
            text = {
                TextField(
                    value = editWinContent,
                    onValueChange = { editWinContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editSmallWin(editWinId, editWinContent)
                    showEditDialog = false
                }) {
                    Text("保存", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消", fontSize = 14.sp)
                }
            }
        )
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            TopBar(
                selectedTab = selectedTab,
                onTabSelected = viewModel::setSelectedTab,
                onNavigateBack = onNavigateBack
            )
            when (selectedTab) {
                 0 -> TodayTab(
                    todaySmallWins = todaySmallWins,
                    inputText = inputText,
                    selectedCategory = selectedCategory,
                    onInputChange = viewModel::setInputText,
                    onAdd = { viewModel.addSmallWin(it) },
                    onDelete = { id ->
                        pendingDeleteId = id
                        showDeleteDialog = true
                    },
                    onEdit = { id, content ->
                        editWinId = id
                        editWinContent = content
                        showEditDialog = true
                    },
                    onSelectCategory = viewModel::setSelectedCategory,
                    currentStreak = analytics.currentStreak
                )
                1 -> HistoryTab(
                    selectedDate = selectedDate,
                    historyWins = historyWins,
                    onNavigateDay = viewModel::navigateDay,
                    onDelete = { id ->
                        pendingDeleteId = id
                        showDeleteDialog = true
                    },
                    onEdit = { id, content ->
                        editWinId = id
                        editWinContent = content
                        showEditDialog = true
                    }
                )
                2 -> StatsTab(
                    analytics = analytics,
                    aiSummary = aiSummary,
                    isAiLoading = isAiLoading,
                    isAiEnabled = viewModel.isAiEnabled,
                    onGenerateSummary = viewModel::generateSummary,
                    onOpenWritingBridge = {
                        viewModel.prepareWritingBridge()
                        showWritingBridgeDialog = true
                    },
                    onShare = {
                        val text = viewModel.getShareText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享摘要"))
                    }
                )
            }
        }
    }

    if (showWritingBridgeDialog && writingBridgeSeed != null) {
        WritingBridgeDialog(
            seed = writingBridgeSeed!!,
            onDismiss = {
                showWritingBridgeDialog = false
                viewModel.clearWritingBridgeSeed()
            }
        )
    }
}

@Composable
private fun TopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val tabTitles = listOf("今日", "历史", "统计")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "小确幸",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun TodayTab(
    todaySmallWins: List<SmallWin>,
    inputText: String,
    selectedCategory: String,
    onInputChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (Long, String) -> Unit,
    onSelectCategory: (String) -> Unit,
    currentStreak: Int
) {
    val filteredWins = if (selectedCategory == "all") todaySmallWins
    else todaySmallWins.filter { matchesCategory(it.content, selectedCategory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCard(
                cornerRadius = 12.dp,
                innerPadding = 10.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "连续 $currentStreak 天",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            GlassCard(
                cornerRadius = 12.dp,
                innerPadding = 10.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = todaySmallWins.size.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "今日",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            cornerRadius = 12.dp,
            innerPadding = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "今天有什么小确幸...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                IconButton(
                    onClick = { onAdd(inputText) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(CATEGORIES) { category ->
                val isSelected = selectedCategory == category
                val chipColor = categoryColor(category)
                Text(
                    text = categoryDisplayName(category),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else chipColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) chipColor
                            else chipColor.copy(alpha = 0.12f)
                        )
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (todaySmallWins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (selectedCategory == "all") {
                    EmptyState(
                        icon = Icons.Default.Star,
                        title = "还没有记录",
                        subtitle = "写下今天的第一件小确幸吧"
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.Star,
                        title = "没有匹配的记录",
                        subtitle = "试试其他分类"
                    )
                }
            }
        } else if (filteredWins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Star,
                    title = "没有匹配的记录",
                    subtitle = "试试其他分类"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredWins, key = { it.id }) { smallWin ->
                    SmallWinCard(
                        smallWin = smallWin,
                        onDelete = { onDelete(smallWin.id) },
                        onEdit = { onEdit(smallWin.id, smallWin.content) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    selectedDate: Long,
    historyWins: List<SmallWin>,
    onNavigateDay: (Int) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (Long, String) -> Unit
) {
    val date = LocalDate.ofEpochDay(selectedDate / 86400000)
    val dateText = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    val isToday = date == LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { onNavigateDay(-1) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isToday) "今天 - ${historyWins.size} 件"
                    else "共 ${historyWins.size} 件",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onNavigateDay(1) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "后一天",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyWins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.DateRange,
                    title = if (isToday) "今天还没有记录" else "这一天还没有记录",
                    subtitle = if (isToday) "切换到今天标签来记录小确幸吧"
                    else "试试查看其他日期"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyWins, key = { it.id }) { smallWin ->
                    SmallWinCard(
                        smallWin = smallWin,
                        onDelete = { onDelete(smallWin.id) },
                        onEdit = { onEdit(smallWin.id, smallWin.content) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StatsTab(
    analytics: SmallWinsAnalytics,
    aiSummary: String?,
    isAiLoading: Boolean,
    isAiEnabled: Boolean,
    onGenerateSummary: () -> Unit,
    onOpenWritingBridge: () -> Unit,
    onShare: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "数据总览",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "累计",
                    value = analytics.totalWins.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "当前连续",
                    value = "${analytics.currentStreak} 天",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "最长连续",
                    value = "${analytics.longestStreak} 天",
                    modifier = Modifier.weight(1f)
                )
                GlassCard(cornerRadius = 12.dp, innerPadding = 14.dp, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f".format(analytics.averagePerActiveDay),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "活跃日均 / ${analytics.daysActive} 天",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "全部 %.1f / 天".format(analytics.averagePerDay),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        if (analytics.totalWins > 0) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    text = "本周 vs 上周",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard(cornerRadius = 12.dp, innerPadding = 14.dp) {
                    Column {
                        WeekComparisonRow("本周", analytics.thisWeekCount, analytics.thisWeekCount.toFloat())
                        Spacer(modifier = Modifier.height(8.dp))
                        WeekComparisonRow("上周", analytics.lastWeekCount, analytics.thisWeekCount.toFloat())
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Text(
                    text = "分类统计",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard(cornerRadius = 12.dp, innerPadding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val total = analytics.categoryDistribution.values.sum().toFloat()
                        analytics.categoryDistribution.entries
                            .sortedByDescending { it.value }
                            .forEach { (cat, count) ->
                                CategoryBar(
                                    name = categoryDisplayName(cat),
                                    count = count,
                                    fraction = if (total > 0f) count / total else 0f,
                                    color = categoryColor(cat)
                                )
                            }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        if (isAiEnabled && analytics.totalWins > 0) {
            item {
                Text(
                    text = "AI 分析",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard(cornerRadius = 12.dp, innerPadding = 14.dp) {
                    Column {
                        if (aiSummary == null) {
                            Button(
                                onClick = onGenerateSummary,
                                enabled = !isAiLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                if (isAiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("生成中...", fontSize = 14.sp)
                                } else {
                                    Text("生成总结", fontSize = 14.sp)
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = aiSummary != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            aiSummary?.let { summary ->
                                Text(
                                    text = summary,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenWritingBridge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("写成日记素材", fontSize = 14.sp)
                }
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("分享摘要", fontSize = 14.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun WritingBridgeDialog(
    seed: WritingBridgeSeed,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            innerPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = seed.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = seed.summary,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
                GlassCard(cornerRadius = 12.dp, innerPadding = 12.dp) {
                    Text(
                        text = seed.prompt,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        cornerRadius = 12.dp,
        innerPadding = 14.dp,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WeekComparisonRow(
    label: String,
    count: Int,
    maxCount: Float
) {
    val fraction = if (maxCount > 0f) count / maxCount else 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(48.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count 件",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun CategoryBar(
    name: String,
    count: Int,
    fraction: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(42.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.6f))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
private fun SmallWinCard(
    smallWin: SmallWin,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    GlassCard(
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = smallWin.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val timeText = Instant.ofEpochMilli(smallWin.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    Text(
                        text = timeText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val cats = SmallWinsViewModel.parseCategories(smallWin.content)
                    cats.forEach { cat ->
                        Text(
                            text = categoryDisplayName(cat),
                            fontSize = 11.sp,
                            color = categoryColor(cat),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(categoryColor(cat).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(44.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(44.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun categoryColor(category: String): Color = when (category) {
    "health" -> Color(0xFF43A047)
    "work" -> Color(0xFF1E88E5)
    "family" -> Color(0xFFFB8C00)
    "growth" -> Color(0xFF8E24AA)
    "fun" -> Color(0xFFE53935)
    "food" -> Color(0xFFFF6D00)
    "other" -> Color(0xFF78909C)
    "all" -> Color(0xFF546E7A)
    else -> Color(0xFF78909C)
}
