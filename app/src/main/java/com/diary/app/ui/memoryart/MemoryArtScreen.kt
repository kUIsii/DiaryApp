package com.diary.app.ui.memoryart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MemoryArtScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: MemoryArtViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showStyleSelector by remember { mutableStateOf(true) }
    var showSeriesSection by remember { mutableStateOf(false) }
    var showCollageSection by remember { mutableStateOf(false) }
    var collageStartTime by remember { mutableLongStateOf(0L) }
    var collageEndTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var seriesKeyword by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var copiedPrompt by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(diaryId) {
        diaryId?.let { viewModel.generateArt(it) }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "记忆艺术", onNavigateBack = onNavigateBack)

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("创作", modifier = Modifier.padding(vertical = DesignTokens.SpacingSm, horizontal = DesignTokens.SpacingLg))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("画廊", modifier = Modifier.padding(vertical = DesignTokens.SpacingSm, horizontal = DesignTokens.SpacingLg))
                }
            }

            when (selectedTab) {
                0 -> CreateTab(
                    state = state,
                    viewModel = viewModel,
                    showStyleSelector = showStyleSelector,
                    onToggleStyleSelector = { showStyleSelector = !showStyleSelector },
                    showExportDialog = showExportDialog,
                    onShowExportDialog = { showExportDialog = true },
                    onHideExportDialog = { showExportDialog = false },
                    copiedPrompt = copiedPrompt,
                    onCopyPrompt = { copiedPrompt = it },
                    showSeriesSection = showSeriesSection,
                    onToggleSeriesSection = { showSeriesSection = !showSeriesSection },
                    seriesKeyword = seriesKeyword,
                    onSeriesKeywordChange = { seriesKeyword = it },
                    showCollageSection = showCollageSection,
                    onToggleCollageSection = { showCollageSection = !showCollageSection },
                    collageStartTime = collageStartTime,
                    collageEndTime = collageEndTime,
                    onCollageStartTimeChange = { collageStartTime = it },
                    onCollageEndTimeChange = { collageEndTime = it }
                )
                1 -> GalleryTab(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun CreateTab(
    state: MemoryArtUiState,
    viewModel: MemoryArtViewModel,
    showStyleSelector: Boolean,
    onToggleStyleSelector: () -> Unit,
    showExportDialog: Boolean,
    onShowExportDialog: () -> Unit,
    onHideExportDialog: () -> Unit,
    copiedPrompt: String?,
    onCopyPrompt: (String?) -> Unit,
    showSeriesSection: Boolean,
    onToggleSeriesSection: () -> Unit,
    seriesKeyword: String,
    onSeriesKeywordChange: (String) -> Unit,
    showCollageSection: Boolean,
    onToggleCollageSection: () -> Unit,
    collageStartTime: Long,
    collageEndTime: Long,
    onCollageStartTimeChange: (Long) -> Unit,
    onCollageEndTimeChange: (Long) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.SpacingLg),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
    ) {
        // Style selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("艺术风格", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                TextButton(onClick = onToggleStyleSelector) {
                    Text(if (showStyleSelector) "收起" else "展开", fontSize = DesignTokens.FontBody)
                }
            }
        }

        if (showStyleSelector) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    ArtStyle.values().forEach { style ->
                        val isSelected = state.selectedStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                                .clickable { viewModel.selectStyle(style) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = style.label,
                                fontSize = if (isSelected) DesignTokens.FontBody else DesignTokens.FontSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Art canvas
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = DesignTokens.CornerLarge,
                innerPadding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(DesignTokens.SpacingLg)
                ) {
                    if (state.isGenerating || state.artConfig == null && state.currentArtwork == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val config = state.artConfig
                        val artwork = state.currentArtwork
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val style = state.selectedStyle

                            drawArtwork(style, artwork, config, width, height)
                        }
                    }
                }
            }
        }

        // Color palette
        item {
            val palette = state.currentArtwork?.palette ?: state.artConfig?.colorPalette?.map {
                String.format("#%08X", it)
            } ?: emptyList()

            if (palette.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerLarge,
                    innerPadding = DesignTokens.SpacingLg
                ) {
                    Column {
                        Text("色彩方案", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                        ) {
                            palette.forEach { hex ->
                                val color = try {
                                    val clean = hex.removePrefix("#")
                                    val rgb = when (clean.length) {
                                        6 -> (0xFF000000 or clean.toLong(16))
                                        8 -> clean.toLong(16)
                                        else -> 0xFF9E9E9E
                                    }
                                    Color(rgb)
                                } catch (_: Exception) { Color.Gray }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(color, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export prompt buttons
        item {
            if (state.currentArtwork != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerLarge,
                    innerPadding = DesignTokens.SpacingLg
                ) {
                    Column {
                        Text("导出提示词", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                        ) {
                            PromptFormat.values().forEach { format ->
                                val isSelected = state.selectedPromptFormat == format
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        )
                                        .clickable { viewModel.selectPromptFormat(format) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = format.label,
                                        fontSize = DesignTokens.FontSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        val artwork = state.currentArtwork!!
                        val prompt = viewModel.exportPrompt(artwork, state.selectedPromptFormat)
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(prompt))
                                onCopyPrompt(state.selectedPromptFormat.label)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(DesignTokens.CornerSmall)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium))
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                            Text(if (copiedPrompt == state.selectedPromptFormat.label) "已复制" else "复制到剪贴板", fontSize = DesignTokens.FontBody)
                        }
                    }
                }
            }
        }

        // Evolution Series section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("演变系列", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                TextButton(onClick = onToggleSeriesSection) {
                    Text(if (showSeriesSection) "收起" else "展开", fontSize = DesignTokens.FontBody)
                }
            }
        }

        if (showSeriesSection) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerLarge,
                    innerPadding = DesignTokens.SpacingLg
                ) {
                    Column {
                        Text("输入主题关键词，AI 将生成该主题随时间演变的系列作品", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = seriesKeyword,
                                onValueChange = onSeriesKeywordChange,
                                placeholder = { Text("如: 旅行, 家人, 梦想", fontSize = DesignTokens.FontBody) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = DesignTokens.FontBody)
                            )
                            Button(
                                onClick = { viewModel.generateEvolutionSeries(seriesKeyword) },
                                enabled = seriesKeyword.isNotBlank() && !state.isGenerating,
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(DesignTokens.CornerSmall)
                            ) {
                                Text("生成", fontSize = DesignTokens.FontBody)
                            }
                        }
                    }
                }
            }

            if (state.evolutionSeries.isNotEmpty()) {
                items(state.evolutionSeries) { series ->
                    EvolutionSeriesCard(series = series, viewModel = viewModel)
                }
            }
        }

        // Memory Collage section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("记忆拼贴", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
                TextButton(onClick = onToggleCollageSection) {
                    Text(if (showCollageSection) "收起" else "展开", fontSize = DesignTokens.FontBody)
                }
            }
        }

        if (showCollageSection) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerLarge,
                    innerPadding = DesignTokens.SpacingLg
                ) {
                    Column {
                        Text("选择时间范围，AI 将生成该时期的综合拼贴作品", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Button(
                            onClick = {
                                val oneMonthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                viewModel.generateCollage(oneMonthAgo, System.currentTimeMillis())
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !state.isGenerating,
                            shape = RoundedCornerShape(DesignTokens.CornerSmall)
                        ) {
                            Text("生成最近30天的拼贴", fontSize = DesignTokens.FontBody)
                        }
                    }
                }
            }

            if (state.collage != null) {
                item {
                    CollageCard(collage = state.collage!!)
                }
            }
        }
    }
}

@Composable
private fun EvolutionSeriesCard(series: EvolutionSeries, viewModel: MemoryArtViewModel) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Text("主题: ${series.theme}", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
            Text(series.emotionTransition, fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                items(series.artworks) { artwork ->
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                    ) {
                        MiniArtCanvas(artwork = artwork)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageCard(collage: MemoryCollage) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg
    ) {
        Column {
            Text("记忆拼贴", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
            Text("包含 ${collage.entryIds.size} 篇日记", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(DesignTokens.CornerSmall))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    collage.artworks.forEachIndexed { index, artwork ->
                        val segmentWidth = width / collage.artworks.size.coerceAtLeast(1)
                        val alpha = ((index + 1).toFloat() / collage.artworks.size) * 0.6f + 0.2f
                        val sx = index * segmentWidth
                        drawRect(
                            color = Color(artwork.palette.firstOrNull()?.let { colorFromHex(it) } ?: 0xFF9E9E9E),
                            topLeft = Offset(sx, 0f),
                            size = Size(segmentWidth, height),
                            alpha = alpha
                        )
                        artwork.composition.shapes.take(5).forEach { shape ->
                            val cx = sx + shape.x * segmentWidth
                            val cy = shape.y * height
                            val sz = shape.size * segmentWidth.coerceAtMost(height)
                            drawCircle(
                                color = Color(shape.color).copy(alpha = shape.alpha * 0.5f),
                                radius = sz / 2,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                items(collage.dominantColors.take(6)) { color ->
                    Box(modifier = Modifier.size(32.dp).background(Color(color), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun MiniArtCanvas(artwork: MemoryArtwork) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val seed = artwork.id.hashCode()
        val random = Random(seed)

        artwork.composition.shapes.forEach { shape ->
            val x = shape.x * w
            val y = shape.y * h
            val sz = shape.size * w.coerceAtMost(h)
            val color = Color(shape.color).copy(alpha = shape.alpha.coerceIn(0.2f, 0.8f))
            when (shape.type) {
                ShapeType.CIRCLE, ShapeType.SPLASH -> {
                    drawCircle(color = color, radius = sz / 2, center = Offset(x, y))
                }
                ShapeType.SQUARE -> {
                    rotate(shape.rotation, pivot = Offset(x, y)) {
                        drawRect(color = color, topLeft = Offset(x - sz / 2, y - sz / 2), size = Size(sz, sz))
                    }
                }
                ShapeType.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(x, y - sz / 2); lineTo(x - sz / 2, y + sz / 2); lineTo(x + sz / 2, y + sz / 2); close()
                    }
                    rotate(shape.rotation, pivot = Offset(x, y)) { drawPath(path, color) }
                }
                ShapeType.LINE -> {
                    val ex = x + cos(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                    val ey = y + sin(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                    drawLine(color = color, start = Offset(x, y), end = Offset(ex, ey), strokeWidth = 2f)
                }
                ShapeType.ARC -> {
                    drawArc(color = color, startAngle = shape.rotation, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(x - sz / 2, y - sz / 2), size = Size(sz, sz), style = Stroke(width = 2f))
                }
                ShapeType.STROKE -> {
                    val ex = x + cos(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                    val ey = y + sin(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                    drawLine(color = color, start = Offset(x, y), end = Offset(ex, ey), strokeWidth = sz * 0.1f)
                }
                ShapeType.DOT -> {
                    drawCircle(color = color, radius = sz * 0.05f, center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun GalleryTab(state: MemoryArtUiState, viewModel: MemoryArtViewModel) {
    val clipboardManager = LocalClipboardManager.current
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.SpacingLg, vertical = DesignTokens.SpacingSm),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("搜索...", fontSize = DesignTokens.FontBody) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = DesignTokens.FontBody),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium)) }
            )
            FilterButton(
                label = if (state.showFavoritesOnly) "收藏" else "筛选",
                isActive = state.showFavoritesOnly,
                onClick = { viewModel.toggleFavoritesOnly() }
            )
        }

        // Style filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = DesignTokens.SpacingLg),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
        ) {
            item {
                FilterButton(
                    label = "全部",
                    isActive = state.styleFilter == null,
                    onClick = { viewModel.setStyleFilter(null) }
                )
            }
            items(ArtStyle.values().toList()) { style ->
                FilterButton(
                    label = style.label,
                    isActive = state.styleFilter == style.label,
                    onClick = { viewModel.setStyleFilter(style.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        val artworks = viewModel.filteredArtworks
        if (artworks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text("暂无艺术作品", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("在创作标签中生成吧", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(DesignTokens.SpacingLg),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(artworks, key = { it.id }) { artwork ->
                    GalleryArtworkCard(
                        artwork = artwork,
                        onToggleFavorite = { viewModel.toggleFavorite(artwork.id) },
                        onCopyPrompt = { format ->
                            val prompt = viewModel.exportPrompt(artwork, format)
                            clipboardManager.setText(AnnotatedString(prompt))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(DesignTokens.CornerSmall))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.SpacingMd),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = DesignTokens.FontBody,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GalleryArtworkCard(
    artwork: MemoryArtwork,
    onToggleFavorite: () -> Unit,
    onCopyPrompt: (PromptFormat) -> Unit
) {
    var showPromptMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerSmall,
        innerPadding = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                MiniArtCanvas(artwork = artwork)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (artwork.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (artwork.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconLarge)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { showPromptMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
            }

            Column(modifier = Modifier.padding(DesignTokens.SpacingSm)) {
                Text(artwork.style, fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
                Text(
                    text = formatDate(artwork.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${artwork.composition.shapes.size} 个元素",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPromptMenu) {
        AlertDialog(
            onDismissRequest = { showPromptMenu = false },
            title = { Text("导出提示词", fontSize = DesignTokens.FontMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                    PromptFormat.values().forEach { format ->
                        val prompt = when (format) {
                            PromptFormat.MIDJOURNEY -> "/imagine prompt: ${artwork.aiPrompt.take(100)}..."
                            PromptFormat.STABLE_DIFFUSION -> "${artwork.aiPrompt.take(100)}..."
                            PromptFormat.DALLE -> "${artwork.aiPrompt.take(100)}..."
                        }
                        Button(
                            onClick = {
                                onCopyPrompt(format)
                                showPromptMenu = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(DesignTokens.CornerSmall)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium))
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                            Text("复制为 ${format.label}", fontSize = DesignTokens.FontBody)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPromptMenu = false }) {
                    Text("取消", fontSize = DesignTokens.FontBody)
                }
            }
        )
    }
}

private fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.CHINESE)
    return sdf.format(java.util.Date(millis))
}

private fun colorFromHex(hex: String): Long {
    val clean = hex.removePrefix("#")
    return when (clean.length) {
        6 -> (0xFF000000 or clean.toLong(16))
        8 -> clean.toLong(16)
        else -> 0xFF9E9E9E
    }
}

private fun DrawScope.drawArtwork(
    style: ArtStyle,
    artwork: MemoryArtwork?,
    config: MemoryArtConfig?,
    width: Float,
    height: Float
) {
    val shapes = artwork?.composition?.shapes ?: config?.shapes ?: return
    val seed = artwork?.id?.hashCode() ?: config?.seed?.toInt() ?: 0
    val random = Random(seed)

    when (style) {
        ArtStyle.WATERCOLOR -> drawWatercolor(shapes, width, height, random)
        ArtStyle.OIL -> drawOil(shapes, width, height, random)
        ArtStyle.SKETCH -> drawSketch(shapes, width, height, random)
        ArtStyle.ABSTRACT -> drawAbstract(shapes, width, height, random)
        ArtStyle.INK -> drawInk(shapes, width, height, random)
    }
}

private fun DrawScope.drawWatercolor(shapes: List<ArtShape>, width: Float, height: Float, random: Random) {
    val bgColor = Color.White.copy(alpha = 0.1f)
    drawRect(color = bgColor, topLeft = Offset.Zero, size = Size(width, height))

    shapes.sortedBy { it.size }.forEach { shape ->
        val x = shape.x * width
        val y = shape.y * height
        val sz = shape.size * width.coerceAtMost(height)
        val baseColor = Color(shape.color).copy(alpha = shape.alpha.coerceIn(0.1f, 0.35f))

        drawCircle(color = baseColor, radius = sz / 2, center = Offset(x, y))
        drawCircle(color = baseColor.copy(alpha = baseColor.alpha * 0.5f), radius = sz * 0.6f, center = Offset(x + random.nextFloat() * sz * 0.1f, y + random.nextFloat() * sz * 0.1f))
    }
}

private fun DrawScope.drawOil(shapes: List<ArtShape>, width: Float, height: Float, random: Random) {
    drawRect(color = Color(0xFFF5E6C8), topLeft = Offset.Zero, size = Size(width, height))

    shapes.forEach { shape ->
        val x = shape.x * width
        val y = shape.y * height
        val sz = shape.size * width.coerceAtMost(height)
        val color = Color(shape.color).copy(alpha = shape.alpha.coerceIn(0.6f, 0.95f))

        for (i in 0 until 3) {
            val ox = random.nextFloat() * sz * 0.1f - sz * 0.05f
            val oy = random.nextFloat() * sz * 0.1f - sz * 0.05f
            drawCircle(
                color = color.copy(alpha = color.alpha * (0.7f - i * 0.15f)),
                radius = sz / 2 * (1f - i * 0.15f),
                center = Offset(x + ox, y + oy)
            )
        }
    }
}

private fun DrawScope.drawSketch(shapes: List<ArtShape>, width: Float, height: Float, random: Random) {
    drawRect(color = Color(0xFFF5F0E8), topLeft = Offset.Zero, size = Size(width, height))

    shapes.forEach { shape ->
        val x = shape.x * width
        val y = shape.y * height
        val sz = shape.size * width.coerceAtMost(height)
        val color = Color(shape.color).copy(alpha = shape.alpha.coerceIn(0.4f, 0.8f))

        for (i in 0 until 4) {
            val angle = shape.rotation + i * 45f + random.nextFloat() * 10f
            val ex = x + cos(Math.toRadians(angle.toDouble())).toFloat() * sz
            val ey = y + sin(Math.toRadians(angle.toDouble())).toFloat() * sz
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(ex, ey),
                strokeWidth = (1f + random.nextFloat() * 2f)
            )
        }
    }
}

private fun DrawScope.drawAbstract(shapes: List<ArtShape>, width: Float, height: Float, random: Random) {
    drawRect(color = Color(0xFF1A1A2E), topLeft = Offset.Zero, size = Size(width, height))

    shapes.forEach { shape ->
        val x = shape.x * width
        val y = shape.y * height
        val sz = shape.size * width.coerceAtMost(height)
        val color = Color(shape.color).copy(alpha = shape.alpha.coerceIn(0.5f, 0.9f))

        when (shape.type) {
            ShapeType.CIRCLE, ShapeType.SPLASH -> {
                drawCircle(color = color, radius = sz / 2, center = Offset(x, y))
            }
            ShapeType.SQUARE -> {
                rotate(shape.rotation, pivot = Offset(x, y)) {
                    drawRect(color = color, topLeft = Offset(x - sz / 2, y - sz / 2), size = Size(sz, sz))
                }
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(x, y - sz / 2); lineTo(x - sz / 2, y + sz / 2); lineTo(x + sz / 2, y + sz / 2); close()
                }
                rotate(shape.rotation, pivot = Offset(x, y)) { drawPath(path, color) }
            }
            ShapeType.LINE -> {
                val ex = x + cos(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                val ey = y + sin(Math.toRadians(shape.rotation.toDouble())).toFloat() * sz
                drawLine(color = color, start = Offset(x, y), end = Offset(ex, ey), strokeWidth = 3f)
            }
            ShapeType.ARC -> {
                drawArc(color = color, startAngle = shape.rotation, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(x - sz / 2, y - sz / 2), size = Size(sz, sz), style = Stroke(width = 3f))
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawInk(shapes: List<ArtShape>, width: Float, height: Float, random: Random) {
    drawRect(color = Color(0xFFF5F0E8), topLeft = Offset.Zero, size = Size(width, height))

    val inkColor = Color(0xFF1A1A1A)

    shapes.sortedByDescending { it.size }.forEach { shape ->
        val x = shape.x * width
        val y = shape.y * height
        val sz = shape.size * width.coerceAtMost(height)
        val alpha = shape.alpha.coerceIn(0.15f, 0.7f)

        val path = Path().apply {
            moveTo(x, y)
            for (i in 0 until 6) {
                val angle = i * 60f + random.nextFloat() * 30f
                val r = sz / 2 * (0.3f + random.nextFloat() * 0.7f)
                val px = x + cos(Math.toRadians(angle.toDouble())).toFloat() * r
                val py = y + sin(Math.toRadians(angle.toDouble())).toFloat() * r
                quadraticBezierTo(px, py, x + (px - x) * 0.5f, y + (py - y) * 0.5f)
            }
            close()
        }
        drawPath(path, inkColor.copy(alpha = alpha))
        drawCircle(color = inkColor.copy(alpha = alpha * 0.3f), radius = sz * 0.15f, center = Offset(x, y))
    }
}
