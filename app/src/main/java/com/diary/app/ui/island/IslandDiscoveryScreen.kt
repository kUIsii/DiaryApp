package com.diary.app.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.IslandDiscovery
import com.diary.app.data.RareElement
import com.diary.app.data.SeasonalScene
import java.text.SimpleDateFormat
import java.util.*

/**
 * 发现档案界面
 * 显示已发现/未发现的秘密
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandDiscoveryScreen(
    discoveries: List<IslandDiscovery>,
    onBack: () -> Unit
) {
    // 定义所有可能的发现项
    val allDiscoveries = remember {
        buildDiscoveryList()
    }

    // 分离已发现和未发现
    val discoveredKeys = remember(discoveries) {
        discoveries.map { it.discoveryKey }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "小岛的秘密档案",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // 统计卡片
            DiscoveryStatsCard(
                discoveredCount = discoveries.size,
                totalCount = allDiscoveries.size
            )

            // 发现列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 稀有元素
                item {
                    DiscoverySectionHeader(title = "稀有元素")
                }
                items(allDiscoveries.filter { it.type == "rare" }) { item ->
                    DiscoveryItem(
                        item = item,
                        isDiscovered = item.key in discoveredKeys,
                        discoveryTime = discoveries.find { it.discoveryKey == item.key }?.discoveredAt
                    )
                }

                // 季节性场景
                item {
                    DiscoverySectionHeader(title = "季节性场景")
                }
                items(allDiscoveries.filter { it.type == "seasonal" }) { item ->
                    DiscoveryItem(
                        item = item,
                        isDiscovered = item.key in discoveredKeys,
                        discoveryTime = discoveries.find { it.discoveryKey == item.key }?.discoveredAt
                    )
                }

                // 彩蛋
                item {
                    DiscoverySectionHeader(title = "隐藏彩蛋")
                }
                items(allDiscoveries.filter { it.type == "egg" }) { item ->
                    DiscoveryItem(
                        item = item,
                        isDiscovered = item.key in discoveredKeys,
                        discoveryTime = discoveries.find { it.discoveryKey == item.key }?.discoveredAt
                    )
                }

                // 底部提示
                item {
                    DiscoveryHint()
                }
            }
        }
    }
}

/**
 * 发现统计卡片
 */
@Composable
private fun DiscoveryStatsCard(
    discoveredCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "已发现",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$discoveredCount / $totalCount",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 进度条
            LinearProgressIndicator(
                progress = discoveredCount.toFloat() / totalCount.coerceAtLeast(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF1976D2),
                trackColor = Color(0xFFBBDEFB)
            )
        }
    }
}

/**
 * 发现分类标题
 */
@Composable
private fun DiscoverySectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF333333),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 发现项
 */
@Composable
private fun DiscoveryItem(
    item: DiscoveryItemData,
    isDiscovered: Boolean,
    discoveryTime: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDiscovered) 1f else 0.7f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDiscovered) Color.White else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标/状态
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDiscovered) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDiscovered) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDiscovered) Color(0xFF333333) else Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isDiscovered) {
                    // 已发现：显示发现时间
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val timeString = discoveryTime?.let { dateFormat.format(Date(it)) } ?: "未知"
                    Text(
                        text = "发现于 $timeString",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                } else {
                    // 未发现：显示提示
                    Text(
                        text = item.hint,
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

/**
 * 底部提示
 */
@Composable
private fun DiscoveryHint() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        )
    ) {
        Text(
            text = "提示：继续探索，更多秘密等待发现...",
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 发现项数据类
 */
private data class DiscoveryItemData(
    val key: String,
    val name: String,
    val hint: String,
    val type: String  // "rare", "seasonal", "egg"
)

/**
 * 构建完整的发现列表
 */
private fun buildDiscoveryList(): List<DiscoveryItemData> {
    val items = mutableListOf<DiscoveryItemData>()

    // 稀有元素
    RareElement.entries.forEach { element ->
        items.add(
            DiscoveryItemData(
                key = element.id,
                name = element.displayName,
                hint = "???",
                type = "rare"
            )
        )
    }

    // 季节性场景
    SeasonalScene.entries.forEach { scene ->
        items.add(
            DiscoveryItemData(
                key = scene.id,
                name = scene.displayName,
                hint = "???",
                type = "seasonal"
            )
        )
    }

    // 彩蛋（特殊处理，因为key包含日期）
    items.add(
        DiscoveryItemData(
            key = "easter_egg",
            name = "思念彩蛋",
            hint = "清明节 + 写下\"思念\"",
            type = "egg"
        )
    )

    return items
}
