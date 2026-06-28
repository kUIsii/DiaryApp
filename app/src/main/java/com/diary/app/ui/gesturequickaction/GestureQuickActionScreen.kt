package com.diary.app.ui.gesturequickaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureQuickActionScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: GestureQuickActionViewModel = viewModel()
) {
    val mappings by viewModel.mappings.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "手势快捷操作", onNavigateBack = onNavigateBack)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("自定义手势与操作的映射关系", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp))

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 8.dp) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        viewModel.gestureOptions.forEach { gesture ->
                            GestureActionRow(gesture = gesture, selectedAction = mappings[gesture] ?: "无操作",
                                onActionSelected = { action -> viewModel.setAction(gesture, action) })
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
                    Column {
                        Text("可用手势", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• 双指点击：在首页用双指点击屏幕\n• 首页下拉：在首页顶部向下滑动\n• 长按日期：在日历页面长按日期\n• 左滑/右滑条目：在日记列表中滑动",
                            fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureActionRow(gesture: String, selectedAction: String, onActionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(gesture, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedAction, onValueChange = {}, readOnly = true, singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(140.dp),
                textStyle = MaterialTheme.typography.bodyMedium, shape = RoundedCornerShape(12.dp))
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val actions = listOf("新建日记", "快速签到", "打开那年今日", "收藏", "打开搜索", "打开统计", "随机回顾", "无操作")
                actions.forEach { action ->
                    DropdownMenuItem(text = { Text(action) }, onClick = { onActionSelected(action); expanded = false })
                }
            }
        }
    }
}
