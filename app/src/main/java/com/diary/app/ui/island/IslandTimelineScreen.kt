package com.diary.app.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.IslandTimelineEvent
import com.diary.app.data.TimelineEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 事件类型对应的颜色和图标
private data class EventTypeStyle(
    val color: Color,
    val icon: ImageVector,
    val label: String
)

private fun TimelineEventType.toStyle(): EventTypeStyle = when (this) {
    TimelineEventType.FIRST_ENTRY -> EventTypeStyle(
        color = Color(0xFF66BB6A),
        icon = Icons.Default.AutoStories,
        label = "首次记录"
    )
    TimelineEventType.LEVEL_UP -> EventTypeStyle(
        color = Color(0xFFFFD54F),
        icon = Icons.Default.Star,
        label = "等级提升"
    )
    TimelineEventType.DECORATION_UNLOCK -> EventTypeStyle(
        color = Color(0xFF4FC3F7),
        icon = Icons.Default.Park,
        label = "装饰解锁"
    )
    TimelineEventType.ANIMAL_ARRIVE -> EventTypeStyle(
        color = Color(0xFFFF8A65),
        icon = Icons.Default.Pets,
        label = "动物到来"
    )
    TimelineEventType.COMBO_ACTIVATE -> EventTypeStyle(
        color = Color(0xFFAB47BC),
        icon = Icons.Default.Favorite,
        label = "组合激活"
    )
    TimelineEventType.RARE_DISCOVERY -> EventTypeStyle(
        color = Color(0xFFFF7043),
        icon = Icons.Default.Landscape,
        label = "隐藏发现"
    )
    TimelineEventType.SEASON_CHANGE -> EventTypeStyle(
        color = Color(0xFF26A69A),
        icon = Icons.Default.WbSunny,
        label = "季节变化"
    )
}

private fun eventTypeFromString(name: String): TimelineEventType? {
    return try {
        TimelineEventType.valueOf(name)
    } catch (_: Exception) {
        null
    }
}

/**
 * 小岛历史时间线界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandTimelineScreen(
    onBack: () -> Unit,
    viewModel: IslandViewModel
) {
    val filteredEvents by viewModel.filteredTimelineEvents.collectAsState()
    val currentFilter by viewModel.timelineFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "小岛历史",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选栏
            FilterChipsRow(
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setTimelineFilter(it) }
            )

            // 事件列表
            if (filteredEvents.isEmpty()) {
                EmptyTimelineHint()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(filteredEvents) { index, event ->
                        TimelineEventItem(
                            event = event,
                            isLast = index == filteredEvents.lastIndex
                        )
                    }
                    // 底部留白
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    currentFilter: TimelineEventType?,
    onFilterSelected: (TimelineEventType?) -> Unit
) {
    val allTypes = listOf(null) + TimelineEventType.entries

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allTypes.forEach { type ->
            val style = type?.toStyle()
            val label = style?.label ?: "全部"
            val isSelected = currentFilter == type

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(type) },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = if (style != null && isSelected) {
                    {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = (style?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                    selectedLabelColor = style?.color ?: MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = style?.color ?: MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun TimelineEventItem(
    event: IslandTimelineEvent,
    isLast: Boolean
) {
    val type = eventTypeFromString(event.eventType)
    val style = type?.toStyle() ?: EventTypeStyle(
        color = Color.Gray,
        icon = Icons.Default.AutoFixHigh,
        label = "未知"
    )

    val timeFormatter = rememberTimeFormatter()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧：时间 + 竖线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            Text(
                text = timeFormatter.format(Date(event.eventTime)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 时间线圆点
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(style.color)
            )
            // 竖线（最后一个不显示）
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧：事件卡片
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(style.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 消息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = style.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun rememberTimeFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("MM/dd", Locale.getDefault())
    }
}

@Composable
private fun EmptyTimelineHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有历史记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "写下第一篇日记，开启小岛的旅程",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                textAlign = TextAlign.Center
            )
        }
    }
}
