package com.diary.app.ui.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.health.DailyHealthData
import com.diary.app.health.HealthInsight
import com.diary.app.health.InsightType
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@Composable
fun HealthScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: HealthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onPermissionGranted()
    }

    GradientBackground {
        when {
            !state.isAvailable -> {
                EmptyState(
                    icon = Icons.Default.MonitorHeart,
                    title = "Health Connect 不可用",
                    subtitle = "请先安装 Health Connect 应用",
                    modifier = Modifier.fillMaxSize()
                )
            }
            !state.hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Favorite,
                        title = "需要健康数据权限",
                        subtitle = "授权后可查看步数、睡眠、心率等数据",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        permissionLauncher.launch(viewModel.getPermissionIntent())
                    }) {
                        Text("去授权")
                    }
                }
            }
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                HealthContent(
                    state = state,
                    viewModel = viewModel,
                    onRefresh = { viewModel.loadData() }
                )
            }
        }
    }
}

@Composable
private fun HealthContent(
    state: HealthUiState,
    viewModel: HealthViewModel,
    onRefresh: () -> Unit
) {
    val tabs = listOf("今日", "本周", "本月")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
    ) {
        // Header
        item {
            Text(
                text = "健康数据",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Tab row
        item {
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (state.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Content based on selected tab
        when (state.selectedTab) {
            0 -> {
                // Today
                state.todayData?.let { data ->
                    item {
                        TodayOverview(data, viewModel)
                    }
                }
            }
            1 -> {
                // Weekly
                if (state.weeklyData.isNotEmpty()) {
                    item {
                        WeeklyOverview(state.weeklyData, viewModel)
                    }
                }
            }
            2 -> {
                // Monthly
                if (state.monthlyData.isNotEmpty()) {
                    item {
                        MonthlyOverview(state.monthlyData, viewModel)
                    }
                }
            }
        }

        // Insights
        if (state.insights.isNotEmpty()) {
            item {
                SectionTitle("健康洞察")
                Spacer(modifier = Modifier.height(8.dp))
            }
            state.insights.forEach { insight ->
                item {
                    InsightCard(insight)
                }
            }
        }

        // Error
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun TodayOverview(data: DailyHealthData, viewModel: HealthViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Steps card
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricIcon(Icons.Default.DirectionsWalk, Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "步数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${data.steps}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "目标 8000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (data.steps.toFloat() / 8000f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    )
                }
            }
        }

        // Heart rate card
        if (data.heartRateAvg > 0) {
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricIcon(Icons.Default.Favorite, Color(0xFFE91E63))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "心率",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${data.heartRateAvg.toInt()} 次/分",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${data.heartRateMin}-${data.heartRateMax}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "最低-最高",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Grid: sleep, calories, distance, exercise
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallMetricCard(
                icon = Icons.Default.Bedtime,
                label = "睡眠",
                value = viewModel.formatSleepDuration(data.sleepMinutes),
                color = Color(0xFF5C6BC0),
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "消耗",
                value = "${data.caloriesBurned.toInt()} 千卡",
                color = Color(0xFFFF7043),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallMetricCard(
                icon = Icons.Default.Route,
                label = "距离",
                value = viewModel.formatDistance(data.distanceMeters),
                color = Color(0xFF26A69A),
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                icon = Icons.Default.FitnessCenter,
                label = "运动",
                value = "${data.exerciseMinutes} 分钟",
                color = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyOverview(data: List<DailyHealthData>, viewModel: HealthViewModel) {
    val avgSteps = data.map { it.steps }.average().toLong()
    val avgSleep = data.map { it.sleepMinutes }.average().toLong()
    val totalCalories = data.sumOf { it.caloriesBurned }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Steps trend chart
        GlassCard {
            Column {
                Text(
                    text = "本周步数趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyBarChart(
                    data = data.map { it.steps.toFloat() },
                    labels = data.map { "${it.date.dayOfMonth}" },
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "日均 $avgSteps 步",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "总计 ${data.sumOf { it.steps }} 步",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sleep trend
        GlassCard {
            Column {
                Text(
                    text = "本周睡眠趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyBarChart(
                    data = data.map { it.sleepMinutes.toFloat() },
                    labels = data.map { "${it.date.dayOfMonth}" },
                    color = Color(0xFF5C6BC0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "日均 ${viewModel.formatSleepDuration(avgSleep)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallMetricCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "总消耗",
                value = "${totalCalories.toInt()} 千卡",
                color = Color(0xFFFF7043),
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                icon = Icons.Default.DirectionsWalk,
                label = "日均步数",
                value = "$avgSteps",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthlyOverview(data: List<DailyHealthData>, viewModel: HealthViewModel) {
    val avgSteps = data.map { it.steps }.average().toLong()
    val avgSleep = data.map { it.sleepMinutes }.average().toLong()
    val activeDays = data.count { it.steps > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Steps trend
        GlassCard {
            Column {
                Text(
                    text = "本月步数趋势",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Show last 14 days for readability
                val recent = data.takeLast(14)
                WeeklyBarChart(
                    data = recent.map { it.steps.toFloat() },
                    labels = recent.map { "${it.date.dayOfMonth}" },
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "日均 $avgSteps 步",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "有效天数 $activeDays/${data.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallMetricCard(
                icon = Icons.Default.Bedtime,
                label = "日均睡眠",
                value = viewModel.formatSleepDuration(avgSleep),
                color = Color(0xFF5C6BC0),
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                icon = Icons.Default.DirectionsWalk,
                label = "日均步数",
                value = "$avgSteps",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Float>,
    labels: List<String>,
    color: Color
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            val fraction = value / maxVal
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = index * 80,
                    easing = FastOutSlowInEasing
                ),
                label = "bar"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight(animatedFraction.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.5f), color)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labels.getOrNull(index) ?: "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightCard(insight: HealthInsight) {
    val (icon, color) = when (insight.type) {
        InsightType.SLEEP_QUALITY -> Icons.Default.Bedtime to Color(0xFF5C6BC0)
        InsightType.HEART_RATE -> Icons.Default.Favorite to Color(0xFFE91E63)
        InsightType.ACTIVITY_LEVEL -> Icons.Default.DirectionsWalk to Color(0xFF4CAF50)
        InsightType.CONSISTENCY -> Icons.Default.MonitorHeart to Color(0xFFFF9800)
        InsightType.CORRELATION -> Icons.Default.MonitorHeart to Color(0xFF26A69A)
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = insight.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SmallMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = color
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
