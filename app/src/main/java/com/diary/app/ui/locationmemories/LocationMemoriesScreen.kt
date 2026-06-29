package com.diary.app.ui.locationmemories

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.diary.app.data.LocationMemory
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LocationMemoriesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: LocationMemoriesViewModel = viewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val geoEnabled by viewModel.geoEnabled.collectAsState()
    val notifyRadius by viewModel.notifyRadius.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isStorylineLoading by viewModel.isStorylineLoading.collectAsState()
    val moodAnalysis by viewModel.moodAnalysis.collectAsState()
    val storyline by viewModel.storyline.collectAsState()
    val diariesByLocation by viewModel.diariesByLocation.collectAsState()
    val showAddDialog by viewModel.isAddDialogVisible.collectAsState()
    var showStats by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = if (selectedLocation != null) selectedLocation!! else "地点触发回忆",
                onNavigateBack = {
                    if (selectedLocation != null) viewModel.selectLocation(null)
                    else onNavigateBack()
                },
                action = if (selectedLocation == null) {
                    {
                        IconButton(onClick = { viewModel.showAddDialog() }) {
                            Icon(Icons.Default.Add, "添加地点", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else null
            )

            if (selectedLocation == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    LocationMapView(memories = memories)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StatsPanel(
                            stats = viewModel.getLocationStats(),
                            moodDist = viewModel.getMoodDistribution(),
                            expanded = showStats,
                            onToggle = { showStats = !showStats }
                        )
                    }

                    item {
                        GeoSettingsCard(
                            geoEnabled = geoEnabled,
                            notifyRadius = notifyRadius,
                            onToggleGeo = { viewModel.toggleGeoEnabled() },
                            onRadiusChange = { viewModel.updateRadius(it) }
                        )
                    }

                    item {
                        MoodAnalysisCard(
                            analysis = moodAnalysis,
                            isLoading = isAiLoading,
                            onAnalyze = { viewModel.analyzeMoodCorrelation() },
                            diariesByLocation = diariesByLocation,
                            getMoodLabel = { viewModel.getMoodLabel(it) }
                        )
                    }

                    item {
                        LocationListCard(
                            memories = memories,
                            onDelete = { viewModel.deleteMemory(it) },
                            onSelect = { viewModel.selectLocation(it) }
                        )
                    }

                    item {
                        InstructionsCard()
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TimelineView(
                            storyline = storyline,
                            isLoading = isStorylineLoading
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, lat, lng ->
                viewModel.addLocation(name, lat, lng)
                viewModel.hideAddDialog()
            }
        )
    }
}

@Composable
private fun LocationMapView(memories: List<LocationMemory>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val aMap = remember { mapView.map }

    LaunchedEffect(memories) {
        if (memories.isNotEmpty()) {
            aMap.clear()
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val builder = com.amap.api.maps.model.LatLngBounds.builder()
            memories.forEach { mem ->
                val latLng = LatLng(mem.latitude, mem.longitude)
                aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(mem.locationName ?: "未知")
                        .snippet(dateFormat.format(Date(mem.createdAt)))
                )
                builder.include(latLng)
            }
            aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngBounds(builder.build(), 50))
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun StatsPanel(
    stats: List<LocationStats>,
    moodDist: Map<Int, Int>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val totalLocations = stats.size
    val totalDiaries = stats.sumOf { it.totalDiaries }
    val top5 = stats.sortedByDescending { it.visitCount }.take(5)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("统计概览", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$totalLocations", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("地点数", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$totalDiaries", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("日记数", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${stats.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("关联地点", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("最常去地点 Top 5", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            if (top5.isNotEmpty()) {
                val maxCount = top5.maxOf { it.visitCount }.coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((top5.size * 36 + 8).dp)
                ) {
                    val barHeight = size.height / top5.size - 4.dp.toPx()
                    val labelWidth = 80.dp.toPx()
                    val maxBarWidth = size.width - labelWidth - 8.dp.toPx()
                    val paint = Paint().apply {
                        color = android.graphics.Color.parseColor("#4CAF50")
                        textSize = 11.dp.toPx()
                        textAlign = Paint.Align.RIGHT
                        typeface = Typeface.DEFAULT
                    }
                    top5.forEachIndexed { i, stat ->
                        val y = i * (barHeight + 4.dp.toPx()) + 2.dp.toPx()
                        val barW = (stat.visitCount.toFloat() / maxCount) * maxBarWidth
                        drawRoundRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(labelWidth, y),
                            size = Size(barW.coerceAtLeast(4.dp.toPx()), barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            stat.name.take(4),
                            labelWidth - 4.dp.toPx(),
                            y + barHeight / 2 + paint.textSize / 3,
                            paint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${stat.visitCount}次",
                            size.width - 4.dp.toPx(),
                            y + barHeight / 2 + paint.textSize / 3,
                            paint.apply { textAlign = Paint.Align.LEFT; color = android.graphics.Color.GRAY }
                        )
                    }
                }
            } else {
                Text("暂无数据", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (moodDist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("心情分布", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                val moodLabels = mapOf(1 to "沮丧", 2 to "低落", 3 to "一般", 4 to "不错", 5 to "开心", 6 to "兴奋")
                val total = moodDist.values.sum().coerceAtLeast(1)
                moodDist.toSortedMap().forEach { (mood, count) ->
                    val pct = count * 100f / total
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(moodLabels[mood] ?: "未知", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = pct / 100f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${pct.roundToInt()}%", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeoSettingsCard(
    geoEnabled: Boolean,
    notifyRadius: Float,
    onToggleGeo: () -> Unit,
    onRadiusChange: (Float) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Text("定位服务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (geoEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("后台位置监控", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text("到达记忆地点时推送通知", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = geoEnabled,
                onCheckedChange = { onToggleGeo() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("提醒半径: ${notifyRadius.toInt()} 米", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodAnalysisCard(
    analysis: List<LocationMoodAnalysis>,
    isLoading: Boolean,
    onAnalyze: () -> Unit,
    diariesByLocation: Map<String, List<com.diary.app.data.DiaryPreview>>,
    getMoodLabel: (Int) -> String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("地点-情绪关联", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Button(
                onClick = onAnalyze,
                enabled = !isLoading && diariesByLocation.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.heightIn(max = 36.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isLoading) "分析中..." else "AI 分析", fontSize = DesignTokens.FontSmall)
            }
        }
        if (analysis.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HeatmapMatrix(
                analysis = analysis,
                diariesByLocation = diariesByLocation,
                getMoodLabel = getMoodLabel
            )
        } else if (!isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("点击 AI 分析探索地点与情绪之间的关联模式", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HeatmapMatrix(
    analysis: List<LocationMoodAnalysis>,
    diariesByLocation: Map<String, List<com.diary.app.data.DiaryPreview>>,
    getMoodLabel: (Int) -> String
) {
    val allMoods = diariesByLocation.values.flatten().mapNotNull { it.moodLevel }.distinct().sorted()
    val locations = analysis.map { it.location }
    if (locations.isEmpty() || allMoods.isEmpty()) return

    val cellSize = 36.dp
    val labelW = 60.dp
    val headerH = 24.dp

    val maxCount = locations.maxOfOrNull { loc ->
        val entries = diariesByLocation[loc].orEmpty()
        allMoods.maxOfOrNull { mood -> entries.count { it.moodLevel == mood } } ?: 1
    }?.coerceAtLeast(1) ?: 1

    val moodColors = mapOf(
        1 to Color(0xFF2196F3),
        2 to Color(0xFF03A9F4),
        3 to Color(0xFF8BC34A),
        4 to Color(0xFFFFEB3B),
        5 to Color(0xFFFF9800),
        6 to Color(0xFFF44336)
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellSize * (locations.size + 1) + headerH)
    ) {
        val cellPx = cellSize.toPx()
        val labelPx = labelW.toPx()
        val headerPx = headerH.toPx()
        val startX = labelPx + 8.dp.toPx()
        val cellW = (size.width - startX) / allMoods.size

        allMoods.forEachIndexed { col, mood ->
            val x = startX + col * cellW
            drawContext.canvas.nativeCanvas.drawText(
                getMoodLabel(mood),
                x + cellW / 2,
                headerPx / 2 + 4.dp.toPx(),
                Paint().apply {
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.GRAY
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        }

        locations.forEachIndexed { row, loc ->
            val y = headerPx + row * cellPx + 2.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(
                loc.take(4),
                labelPx - 4.dp.toPx(),
                y + cellPx / 2 + 4.dp.toPx(),
                Paint().apply {
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.RIGHT
                    color = android.graphics.Color.DKGRAY
                }
            )

            val entries = diariesByLocation[loc].orEmpty()
            allMoods.forEachIndexed { col, mood ->
                val count = entries.count { it.moodLevel == mood }
                val intensity = count.toFloat() / maxCount
                val x = startX + col * cellW
                val cellColor = if (count > 0) moodColors[mood]?.copy(alpha = intensity.coerceIn(0.15f, 0.95f)) ?: Color.LightGray else Color.LightGray.copy(alpha = 0.1f)
                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(x + 1.dp.toPx(), y + 1.dp.toPx()),
                    size = Size(cellW - 2.dp.toPx(), cellPx - 2.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun LocationListCard(
    memories: List<LocationMemory>,
    onDelete: (Long) -> Unit,
    onSelect: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Text(
            text = "已保存的地点 (${memories.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无记忆地点", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("写日记时添加地点即可自动记录", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            memories.forEach { memory ->
                LocationMemoryCard(memory = memory, onDelete = onDelete, onClick = { onSelect(memory.locationName ?: "(${memory.latitude}, ${memory.longitude})") })
            }
        }
    }
}

@Composable
private fun LocationMemoryCard(memory: LocationMemory, onDelete: (Long) -> Unit = {}, onClick: () -> Unit = {}) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(memory.locationName ?: "未知", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text("${memory.radiusMeters.toInt()}m  ·  ${dateFormat.format(Date(memory.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            IconButton(onClick = { onDelete(memory.id) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Text(
            text = "使用说明",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "• 写日记时添加地点信息，自动创建记忆点\n• 到达该地点附近时会收到通知\n• 可在通知中查看当时的日记\n• 关闭后台监控可节省电量",
            fontSize = DesignTokens.FontBody,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimelineView(
    storyline: List<StorylinePeriod>,
    isLoading: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Text("故事线", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("AI 生成故事线中...", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (storyline.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无故事线数据", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            storyline.forEachIndexed { index, period ->
                TimelineItem(
                    period = period,
                    isFirst = index == 0,
                    isLast = index == storyline.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    period: StorylinePeriod,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        GlassCard(
            modifier = Modifier.weight(1f).padding(bottom = 12.dp),
            cornerRadius = 12.dp,
            innerPadding = 12.dp
        ) {
            Text(period.period, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(4.dp))
            Text("心情趋势: ${period.moodTrend}", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (period.keyEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                period.keyEvents.forEach { event ->
                    Text("• $event", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                }
            }
            if (period.insight.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(period.insight, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.primary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加地点", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("地点名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("纬度") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("经度") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (name.isNotBlank() && lat != null && lng != null) {
                        onConfirm(name, lat, lng)
                    }
                },
                enabled = name.isNotBlank() && latText.toDoubleOrNull() != null && lngText.toDoubleOrNull() != null
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
