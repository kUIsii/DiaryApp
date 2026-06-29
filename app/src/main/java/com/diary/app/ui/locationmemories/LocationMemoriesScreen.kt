package com.diary.app.ui.locationmemories

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.LocationMemory

@Composable
fun LocationMemoriesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: LocationMemoriesViewModel = viewModel()
) {
    var geoEnabled by remember { mutableStateOf(true) }
    var notifyRadius by remember { mutableStateOf(100f) }
    val memories by viewModel.memories.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "地点触发回忆", onNavigateBack = onNavigateBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "到达曾写日记的地点时自动提醒",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "定位服务",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                                    Text("到达记忆地点时推送通知", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = geoEnabled,
                                    onCheckedChange = { geoEnabled = it },
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
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已保存的地点 (${memories.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
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
                                        Text("暂无记忆地点", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("写日记时添加地点即可自动记录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            } else {
                                memories.forEach { memory ->
                                    LocationMemoryCard(memory = memory, onDelete = { viewModel.deleteMemory(it) })
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        innerPadding = 16.dp
                    ) {
                        Column {
                            Text(
                                text = "使用说明",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• 写日记时添加地点信息，自动创建记忆点\n• 到达该地点附近时会收到通知\n• 可在通知中查看当时的日记\n• 关闭后台监控可节省电量",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LocationMemoryCard(memory: LocationMemory, onDelete: (Long) -> Unit = {}) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
