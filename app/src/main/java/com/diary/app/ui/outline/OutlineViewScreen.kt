package com.diary.app.ui.outline

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.readingcenter.buildReadingReviewSummary
import com.diary.app.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineViewScreen(
    onNavigateBack: () -> Unit,
    diaryId: Long? = null,
    viewModel: OutlineViewViewModel = viewModel()
) {
    val outline by viewModel.outline.collectAsState()
    val bodyContent by viewModel.bodyContent.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val aiTopics by viewModel.aiTopics.collectAsState()
    val aiTopicsLoading by viewModel.aiTopicsLoading.collectAsState()
    val aiTags by viewModel.aiTags.collectAsState()
    val aiTagsLoading by viewModel.aiTagsLoading.collectAsState()
    val sentimentPoints by viewModel.sentimentPoints.collectAsState()
    val wordFrequencies by viewModel.wordFrequencies.collectAsState()
    val paragraphLengths by viewModel.paragraphLengths.collectAsState()
    val highlightParagraph by viewModel.highlightParagraph.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val comparisonIds by viewModel.comparisonIds.collectAsState()
    val comparisonOutlines by viewModel.comparisonOutlines.collectAsState()
    val comparisonBodies by viewModel.comparisonBodies.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            viewModel.loadDiary(diaryId)
        } else {
            viewModel.loadCurrentSessionDiaryIfNeeded()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "阅读复盘",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.toggleExportDialog() }) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                }
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            ModeSelector(
                currentMode = mode,
                onModeSelected = { viewModel.setMode(it) }
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            outline?.let { data ->
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Text(
                        text = buildReadingReviewSummary(
                            totalWords = data.totalWords,
                            paragraphCount = data.paragraphCount,
                            headingCount = data.items.size
                        ),
                        fontSize = DesignTokens.FontBody,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            }
            when (mode) {
                OutlineMode.SINGLE -> {
                    if (outline == null && bodyContent == null) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                Text("正在解析日记结构...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        SingleEntryContent(
                            outline = outline,
                            bodyContent = bodyContent,
                            aiTags = aiTags,
                            aiTagsLoading = aiTagsLoading,
                            highlightParagraph = highlightParagraph,
                            sentimentPoints = sentimentPoints,
                            wordFrequencies = wordFrequencies,
                            paragraphLengths = paragraphLengths,
                            onTagClick = { viewModel.setMode(OutlineMode.TIME_RANGE) },
                            onOutlineItemClick = { item -> viewModel.scrollToParagraph(item.charOffset) },
                            onClearHighlight = { viewModel.clearHighlight() },
                            comparisonIds = comparisonIds,
                            comparisonOutlines = comparisonOutlines,
                            comparisonBodies = comparisonBodies,
                            onAddComparison = { viewModel.addComparisonEntry(diaryId ?: 0L) },
                            onRemoveComparison = { viewModel.removeComparisonEntry(diaryId ?: 0L) }
                        )
                    }
                }
                OutlineMode.TIME_RANGE -> {
                    TimeRangeContent(
                        startDate = startDate,
                        endDate = endDate,
                        aiTopics = aiTopics,
                        aiTopicsLoading = aiTopicsLoading,
                        onStartDateChanged = { viewModel.setStartDate(it) },
                        onEndDateChanged = { viewModel.setEndDate(it) },
                        onAnalyze = { viewModel.loadTimeRangeAnalysis() }
                    )
                }
                OutlineMode.THEME -> {
                    ThemeContent(
                        aiTopics = aiTopics,
                        aiTopicsLoading = aiTopicsLoading,
                        wordFrequencies = wordFrequencies,
                        paragraphLengths = paragraphLengths
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { viewModel.dismissExportDialog() },
            onExportText = { viewModel.exportText(context) },
            onExportMarkdown = { viewModel.exportMarkdown(context) }
        )
    }
}

@Composable
private fun ModeSelector(currentMode: OutlineMode, onModeSelected: (OutlineMode) -> Unit) {
    val labels = listOf(
        Pair(OutlineMode.SINGLE, "单篇"),
        Pair(OutlineMode.TIME_RANGE, "时间段"),
        Pair(OutlineMode.THEME, "主题")
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { (mode, label) ->
            val selected = currentMode == mode
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                label = "chipBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                    .background(bg)
                    .clickable { onModeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = DesignTokens.FontBody,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SingleEntryContent(
    outline: OutlineData?,
    bodyContent: String?,
    aiTags: List<String>,
    aiTagsLoading: Boolean,
    highlightParagraph: Int?,
    sentimentPoints: List<SentimentPoint>,
    wordFrequencies: List<WordFreq>,
    paragraphLengths: List<Int>,
    onTagClick: () -> Unit,
    onOutlineItemClick: (OutlineItem) -> Unit,
    onClearHighlight: () -> Unit,
    comparisonIds: List<Long>,
    comparisonOutlines: List<OutlineData>,
    comparisonBodies: List<String>,
    onAddComparison: () -> Unit,
    onRemoveComparison: () -> Unit
) {
    if (comparisonIds.size >= 2) {
        val contrastSummary = if (comparisonOutlines.size >= 2) {
            val maxW = comparisonOutlines.maxOf { it.totalWords }
            val minW = comparisonOutlines.minOf { it.totalWords }
            val avgW = comparisonOutlines.map { it.totalWords }.average().toInt()
            "共对比${comparisonOutlines.size}篇日记，最长${maxW}字，最短${minW}字，平均${avgW}字"
        } else ""
        ComparisonView(
            comparisonOutlines = comparisonOutlines,
            comparisonBodies = comparisonBodies,
            contrastSummary = contrastSummary
        )
        return
    }
    val bodyListState = rememberLazyListState()
    val paragraphs = remember(bodyContent) {
        bodyContent?.split(Regex("\n\\s*\n"))?.filter { it.isNotBlank() } ?: emptyList()
    }

    LaunchedEffect(highlightParagraph) {
        highlightParagraph?.let { idx ->
            if (idx < paragraphs.size) {
                bodyListState.animateScrollToItem(idx)
                kotlinx.coroutines.delay(2000)
                onClearHighlight()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                innerPadding = DesignTokens.SpacingXs
            ) {
                Column {
                    Text("目录", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    if (outline == null || outline.items.isEmpty()) {
                        Text("未检测到标题层级", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("使用 # / ## / ### 标记标题", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(outline.items) { item ->
                                OutlineItemRow(
                                    title = item.title,
                                    level = item.level,
                                    onClick = { onOutlineItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
            GlassCard(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                innerPadding = DesignTokens.SpacingXs
            ) {
                Column {
                    Text("正文", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    if (paragraphs.isEmpty()) {
                        Text("暂无内容", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            state = bodyListState,
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(paragraphs) { index, p ->
                                val isHighlighted = highlightParagraph == index
                                val bgAlpha by animateFloatAsState(
                                    targetValue = if (isHighlighted) 0.2f else 0f,
                                    animationSpec = tween(500),
                                    label = "highlight"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = p,
                                        fontSize = DesignTokens.FontBody
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(0.45f).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            if (aiTagsLoading) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                        Text("AI 正在分析主题...", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (aiTags.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("AI 主题标签", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            aiTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 32.dp)
                                        .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { onTagClick() }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tag, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("统计", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    outline?.let { data ->
                        OutlineStat("总字数", "${data.totalWords}")
                        OutlineStat("段落数", "${data.paragraphCount}")
                        OutlineStat("标题数", "${data.items.size}")
                        OutlineStat("预计阅读", "${data.estimatedReadMinutes}分钟")
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    if (sentimentPoints.isNotEmpty()) {
                        Text("情感曲线", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        SentimentCurve(points = sentimentPoints)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    }
                    if (wordFrequencies.isNotEmpty()) {
                        Text("高频词 Top 10", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        WordCloud(words = wordFrequencies)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    }
                    if (paragraphLengths.isNotEmpty()) {
                        Text("段落长度分布", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        ParagraphLengthChart(lengths = paragraphLengths)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeContent(
    startDate: Long?,
    endDate: Long?,
    aiTopics: List<AiTopic>?,
    aiTopicsLoading: Boolean,
    onStartDateChanged: (Long?) -> Unit,
    onEndDateChanged: (Long?) -> Unit,
    onAnalyze: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startLabel = startDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt) } ?: "未选择"
    val endLabel = endDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt) } ?: "未选择"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("选择时间范围", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showStartPicker = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(startLabel, fontSize = DesignTokens.FontBody, color = if (startDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("至", fontSize = DesignTokens.FontSmall)
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showEndPicker = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(endLabel, fontSize = DesignTokens.FontBody, color = if (endDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                enabled = startDate != null && endDate != null
            ) {
                Text("AI 提取主题")
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onStartDateChanged(state.selectedDateMillis)
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndDateChanged(state.selectedDateMillis)
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

    if (aiTopicsLoading) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Text("AI 正在分析主题...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    aiTopics?.forEach { topic ->
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(topic.name, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text("涉及 ${topic.entryCount} 篇日记 · 代表：${topic.representativeTitle}", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(topic.insight, fontSize = DesignTokens.FontBody)
            }
        }
    }
}

@Composable
private fun ThemeContent(
    aiTopics: List<AiTopic>?,
    aiTopicsLoading: Boolean,
    wordFrequencies: List<WordFreq>,
    paragraphLengths: List<Int>
) {
    if (aiTopicsLoading) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Text("AI 正在分析全量主题...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (aiTopicsLoading && aiTopics == null) return

    if (aiTopics == null) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("切换至主题模式开始分析", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        aiTopics.forEach { topic ->
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(topic.name, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text("涉及 ${topic.entryCount} 篇日记 · 代表：${topic.representativeTitle}", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(topic.insight, fontSize = DesignTokens.FontBody)
                }
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        if (wordFrequencies.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("高频词 Top 10", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                WordCloud(words = wordFrequencies)
            }
        }
    }
}

@Composable
private fun ComparisonView(
    comparisonOutlines: List<OutlineData>,
    comparisonBodies: List<String>,
    contrastSummary: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            comparisonOutlines.forEachIndexed { index, data ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        innerPadding = DesignTokens.SpacingXs
                    ) {
                        Column {
                            Text("日记 ${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (data.items.isEmpty()) {
                                Text("无标题层级", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(data.items) { item ->
                                        Text(
                                            text = item.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (item.level == 0) FontWeight.Medium else FontWeight.Normal,
                                            color = if (item.level == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = (item.level * 8).dp, top = 2.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("对比总结", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(contrastSummary, fontSize = DesignTokens.FontBody)
        }
    }
}

@Composable
private fun OutlineItemRow(title: String, level: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = (level * 12).dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (level == 0) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (level == 0) FontWeight.Medium else FontWeight.Normal,
            color = if (level == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun OutlineStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SentimentCurve(points: List<SentimentPoint>) {
    if (points.isEmpty()) return
    Canvas(
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        val w = size.width
        val h = size.height
        val stepX = if (points.size > 1) w / (points.size - 1) else w

        val path = Path()
        val mid = h / 2f
        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = mid - (p.score * mid * 0.8f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = Color(0xFF6B8DB5), style = Stroke(width = 2.dp.toPx()))

        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()
        drawPath(fillPath, color = Color(0xFF6B8DB5).copy(alpha = 0.15f))
    }
}

@Composable
private fun WordCloud(words: List<WordFreq>) {
    if (words.isEmpty()) return
    val maxCount = words.maxOf { it.count }.coerceAtLeast(1)
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        val w = size.width
        val h = size.height
        val cols = 2
        val rows = (words.size + cols - 1) / cols
        val cellW = w / cols
        val cellH = h / rows

        words.forEachIndexed { i, wf ->
            val col = i % cols
            val row = i / cols
            val ratio = wf.count.toFloat() / maxCount
            val fontSize = (10 + ratio * 8).sp
            val textLayout = measurer.measure(
                text = wf.word,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = if (ratio > 0.7f) FontWeight.Bold else FontWeight.Normal,
                    color = Color(0xFF6B8DB5)
                )
            )
            val x = col * cellW + (cellW - textLayout.size.width) / 2f
            val y = row * cellH + (cellH - textLayout.size.height) / 2f
            drawText(textLayout, topLeft = Offset(x, y))
        }
    }
}

@Composable
private fun ParagraphLengthChart(lengths: List<Int>) {
    if (lengths.isEmpty()) return
    val maxLen = lengths.max().coerceAtLeast(1)
    Canvas(
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        val w = size.width
        val h = size.height
        val barW = w / lengths.size.coerceAtLeast(1)
        val barGap = 1.dp.toPx()
        val effectiveBarW = (barW - barGap).coerceAtLeast(1f)

        lengths.forEachIndexed { i, len ->
            val barH = (len.toFloat() / maxLen) * h * 0.9f
            val x = i * barW
            val y = h - barH
            drawRect(
                color = Color(0xFF7BA06E),
                topLeft = Offset(x + barGap / 2f, y),
                size = androidx.compose.ui.geometry.Size(effectiveBarW, barH)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExportText: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出格式", fontWeight = FontWeight.Medium) },
        text = {
            Column {
                Text("选择导出格式：", fontSize = DesignTokens.FontBody)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onExportText,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) { Text("纯文本") }
                    OutlinedButton(
                        onClick = onExportMarkdown,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) { Text("Markdown") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
