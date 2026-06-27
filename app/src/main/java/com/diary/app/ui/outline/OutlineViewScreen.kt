package com.diary.app.ui.outline

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineViewScreen(
    onNavigateBack: () -> Unit
) {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "大纲视图",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "长篇日记的大纲视图",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自动识别标题层级，在左侧显示目录树。点击可快速跳转到对应段落。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "示例大纲",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlineItem("今天的工作", 0, true)
                    OutlineItem("上午的会议", 1, false)
                    OutlineItem("下午的编码", 1, false)
                    OutlineItem("晚上的思考", 0, true)
                    OutlineItem("关于未来", 1, false)
                    OutlineItem("关于当下", 1, false)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "统计",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlineStat("总字数", "2,345")
                    OutlineStat("段落数", "12")
                    OutlineStat("标题数", "6")
                    OutlineStat("预计阅读", "8分钟")
                }
            }
        }
    }
}

@Composable
private fun OutlineItem(title: String, level: Int, expanded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (level == 0) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            fontSize = if (level == 0) 14.sp else 13.sp,
            fontWeight = if (level == 0) FontWeight.Medium else FontWeight.Normal,
            color = if (level == 0) MaterialTheme.colorScheme.onBackground 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutlineStat(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
