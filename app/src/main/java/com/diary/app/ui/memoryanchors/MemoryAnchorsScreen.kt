package com.diary.app.ui.memoryanchors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.AnchorRelation
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryAnchorsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryAnchorsViewModel = viewModel()
) {
    val anchors by viewModel.filteredAnchors.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val selectedAnchor by viewModel.selectedAnchor.collectAsState()
    val relationsForDetail by viewModel.relationsForDetail.collectAsState()
    val relatedEntries by viewModel.relatedEntries.collectAsState()
    val narrativeText by viewModel.narrativeText.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    GradientBackground {
        if (selectedAnchor != null) {
            AnchorDetailView(
                anchor = selectedAnchor!!,
                relations = relationsForDetail,
                relatedEntries = relatedEntries,
                onBack = { viewModel.selectAnchor(null) }
            )
            return@GradientBackground
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "记忆锚点",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.setShowAddDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加锚点")
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                ViewModeButton("列表", AnchorViewMode.LIST, viewMode) { viewModel.setViewMode(AnchorViewMode.LIST) }
                ViewModeButton("网络图", AnchorViewMode.NETWORK, viewMode) { viewModel.setViewMode(AnchorViewMode.NETWORK) }
                ViewModeButton("叙事", AnchorViewMode.NARRATIVE, viewMode) { viewModel.setViewMode(AnchorViewMode.NARRATIVE) }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("搜索锚点...", fontSize = DesignTokens.FontSmall) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(DesignTokens.IconMedium))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "我的锚点 (${anchors.size})",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("排序：", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    SortChip("关联数", AnchorSortMode.RELATIONS_DESC, sortMode) { viewModel.setSortMode(AnchorSortMode.RELATIONS_DESC) }
                    Spacer(modifier = Modifier.width(4.dp))
                    SortChip("时间", AnchorSortMode.CREATED_DESC, sortMode) { viewModel.setSortMode(AnchorSortMode.CREATED_DESC) }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            when (viewMode) {
                AnchorViewMode.LIST -> ListView(
                    anchors = anchors,
                    recommendations = recommendations,
                    stats = stats,
                    onAnchorClick = { viewModel.selectAnchor(it) },
                    onConfirmRecommendation = { viewModel.confirmRecommendation(it) },
                    onDismissRecommendation = { viewModel.dismissRecommendation(it) }
                )
                AnchorViewMode.NETWORK -> NetworkGraphView(
                    anchors = anchors,
                    onNodeClick = { viewModel.selectAnchor(it) }
                )
                AnchorViewMode.NARRATIVE -> NarrativeView(
                    narrativeText = narrativeText,
                    isLoading = isLoading,
                    anchorCount = anchors.size,
                    onRegenerate = { viewModel.generateNarrative() }
                )
            }
        }

        if (viewModel.showAddDialog.collectAsState().value) {
            AddAnchorDialog(
                onDismiss = { viewModel.setShowAddDialog(false) },
                onConfirm = { topic, description ->
                    viewModel.addAnchor(topic, description, 0L)
                }
            )
        }
    }
}

@Composable
private fun RowScope.ViewModeButton(
    text: String,
    mode: AnchorViewMode,
    currentMode: AnchorViewMode,
    onClick: () -> Unit
) {
    val isSelected = mode == currentMode
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DesignTokens.CornerSmall),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(44.dp).weight(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = DesignTokens.FontSmall,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SortChip(
    text: String,
    mode: AnchorSortMode,
    currentMode: AnchorSortMode,
    onClick: () -> Unit
) {
    val isSelected = mode == currentMode
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = DesignTokens.SpacingSm, vertical = 0.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Text(
            text,
            fontSize = DesignTokens.FontSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListView(
    anchors: List<AnchorWithDetails>,
    recommendations: List<AiRecommendation>,
    stats: AnchorStats,
    onAnchorClick: (AnchorWithDetails) -> Unit,
    onConfirmRecommendation: (AiRecommendation) -> Unit,
    onDismissRecommendation: (AiRecommendation) -> Unit
) {
    if (anchors.isEmpty() && recommendations.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "还没有记忆锚点，点击右上角 + 添加。",
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
        if (stats.topicFrequencies.isNotEmpty()) {
            item {
                WordCloud(topicFrequencies = stats.topicFrequencies)
            }
        }

        item {
            StatsSummary(stats = stats)
        }

        recommendations.forEach { rec ->
            item {
                RecommendationCard(
                    recommendation = rec,
                    onConfirm = { onConfirmRecommendation(rec) },
                    onDismiss = { onDismissRecommendation(rec) }
                )
            }
        }

        items(anchors) { item ->
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            AnchorItem(
                title = item.anchor.topic,
                date = dateFormat.format(Date(item.anchor.createdAt)),
                relatedCount = item.relatedCount,
                onClick = { onAnchorClick(item) }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatsSummary(stats: AnchorStats) {
    GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingMd) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("总锚点数", "${stats.totalAnchors}")
            StatItem("平均关联", String.format("%.1f", stats.avgRelations))
            StatItem("最活跃", stats.mostActiveAnchor.ifEmpty { "-" })
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
        Text(label, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WordCloud(topicFrequencies: Map<String, Int>) {
    if (topicFrequencies.isEmpty()) return

    val maxFreq = topicFrequencies.maxOf { it.value }.toFloat()
    val entries = topicFrequencies.entries.toList()
    val colors = remember {
        listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5),
            Color(0xFF009688), Color(0xFFFF9800), Color(0xFF795548),
            Color(0xFF2196F3), Color(0xFF4CAF50)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val density = this.density
            val cellW = size.width / 4
            val cellH = size.height / 3
            entries.forEachIndexed { index, entry ->
                val col = index % 4
                val row = index / 4
                if (row < 3) {
                    val ratio = entry.value / maxFreq
                    val fontSize = (10f + ratio * 18f)
                    val x = col * cellW + cellW / 2
                    val y = row * cellH + cellH / 2
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.key,
                        x,
                        y,
                        android.graphics.Paint().apply {
                            color = colors[index % colors.size].toArgb()
                            textSize = fontSize * density
                            textAlign = android.graphics.Paint.Align.CENTER
                            this.alpha = ((0.6f + 0.4f * ratio) * 255).toInt().coerceIn(0, 255)
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: AiRecommendation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(
                        "AI 推荐",
                        fontSize = DesignTokens.FontSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "忽略", modifier = Modifier.size(DesignTokens.IconSmall))
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                recommendation.suggestedTopic,
                fontSize = DesignTokens.FontBody,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                recommendation.reason,
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.height(44.dp)
            ) {
                Text("确认添加为锚点", fontSize = DesignTokens.FontSmall)
            }
        }
    }
}

@Composable
private fun NetworkGraphView(
    anchors: List<AnchorWithDetails>,
    onNodeClick: (AnchorWithDetails) -> Unit
) {
    if (anchors.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("暂无锚点", fontSize = DesignTokens.FontBody)
        }
        return
    }

    var selectedNodeId by remember { mutableStateOf<Long?>(null) }
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(anchors) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = minOf(size.width, size.height) / 2f - 50f
                        anchors.forEachIndexed { index, anchor ->
                            val angle = 2 * Math.PI * index / anchors.size
                            val nx = cx + r * cos(angle).toFloat()
                            val ny = cy + r * sin(angle).toFloat()
                            val dist = sqrt(
                                (offset.x - nx) * (offset.x - nx) +
                                (offset.y - ny) * (offset.y - ny)
                            )
                            if (dist < 28f) {
                                selectedNodeId = anchor.anchor.id
                                onNodeClick(anchor)
                            }
                        }
                    }
                }
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2f - 50f
            val positions = anchors.indices.map { i ->
                val angle = 2 * Math.PI * i / anchors.size
                Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
            }

            for (i in positions.indices) {
                for (j in i + 1 until positions.size) {
                    drawLine(
                        color = primary.copy(alpha = 0.12f),
                        start = positions[i],
                        end = positions[j],
                        strokeWidth = 1.5f
                    )
                }
            }

            positions.forEachIndexed { index, pos ->
                val isSelected = selectedNodeId == anchors[index].anchor.id
                val isHighlighted = anchors[index].relatedCount > 0
                val nodeRadius = if (isSelected) 22f else if (isHighlighted) 16f else 12f
                drawCircle(
                    color = primary,
                    radius = nodeRadius + 2f,
                    center = pos
                )
                drawCircle(
                    color = if (isSelected) primaryContainer else Color.White,
                    radius = nodeRadius,
                    center = pos
                )
                drawContext.canvas.nativeCanvas.drawText(
                    anchors[index].anchor.topic.take(4),
                    pos.x,
                    pos.y + 5f,
                    android.graphics.Paint().apply {
                        color = if (isSelected) onPrimaryContainer.toArgb() else primary.toArgb()
                        textSize = if (isSelected) 28f else 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                )
            }

            if (selectedNodeId != null) {
                val sel = anchors.find { it.anchor.id == selectedNodeId } ?: return@Canvas
                val selIndex = anchors.indexOf(sel)
                if (selIndex >= 0 && selIndex < positions.size) {
                    val selPos = positions[selIndex]
                    drawLine(
                        color = primary.copy(alpha = 0.3f),
                        start = selPos,
                        end = Offset(selPos.x, selPos.y + 40f),
                        strokeWidth = 2f
                    )
                    val labelOffsetY = if (selPos.y > cy) 50f else -60f
                    drawContext.canvas.nativeCanvas.drawText(
                        sel.anchor.topic,
                        selPos.x,
                        selPos.y + labelOffsetY,
                        android.graphics.Paint().apply {
                            color = primary.toArgb()
                            textSize = 36f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NarrativeView(
    narrativeText: String?,
    isLoading: Boolean,
    anchorCount: Int,
    onRegenerate: () -> Unit
) {
    Column {
        if (anchorCount < 2) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "需要至少2个锚点才能生成综合叙事",
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        if (narrativeText != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = narrativeText, fontSize = DesignTokens.FontBody)
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            TextButton(
                onClick = onRegenerate,
                modifier = Modifier.height(44.dp)
            ) {
                Text("重新生成", fontSize = DesignTokens.FontSmall)
            }
        }
    }
}

@Composable
private fun AnchorDetailView(
    anchor: AnchorWithDetails,
    relations: List<AnchorRelation>,
    relatedEntries: List<DiaryEntry>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.SpacingLg)
    ) {
        PageHeader(title = anchor.anchor.topic, onNavigateBack = onBack)

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        if (anchor.anchor.description.isNotBlank()) {
            Text(
                anchor.anchor.description,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        }

        Text(
            "关联日记 (${relations.size})",
            fontSize = DesignTokens.FontMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        if (relations.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "暂无关联日记",
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(relations.size) { index ->
                    val relation = relations[index]
                    val entry = relatedEntries.getOrNull(index)
                    TimelineItem(
                        relation = relation,
                        diaryTitle = entry?.title ?: "日记 #${relation.diaryId}",
                        isLast = index == relations.lastIndex
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    relation: AnchorRelation,
    diaryTitle: String,
    isLast: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
        }
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        GlassCard(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else DesignTokens.SpacingSm)) {
            Column {
                Text(
                    diaryTitle,
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    "关联度: ${String.format("%.2f", relation.relevanceScore)} · ${dateFormat.format(Date(relation.createdAt))}",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnchorItem(
    title: String,
    date: String,
    relatedCount: Int,
    onClick: () -> Unit = {}
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium)
                Text(
                    text = "$date · $relatedCount 篇关联",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddAnchorDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记忆锚点") },
        text = {
            Column {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("主题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(topic, description) },
                enabled = topic.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
