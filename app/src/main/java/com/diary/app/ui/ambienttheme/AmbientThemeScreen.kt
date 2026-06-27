package com.diary.app.ui.ambienttheme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientThemeScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientThemeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val palettes by viewModel.availablePalettes.collectAsState()
    
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
                    text = "环境感知主题",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 当前状态
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text("当前状态", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusItem("时间", state.timeOfDay.label)
                        StatusItem("天气", getWeatherLabel(state.weatherCondition))
                        StatusItem("色盘", getPaletteName(state.currentPalette, palettes))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 说明
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "主题根据时间和天气自动变化",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "清晨用温暖的沙金色调，深夜切换到沉静的墨蓝色调，下雨天背景微妙地变灰。切换是${state.transitionDuration}秒的渐变过渡。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 时间段对照
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text("时间段对照", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    TimePaletteRow("黎明 5-6时", "dawn_pink", state.currentPalette, palettes)
                    TimePaletteRow("清晨 7-10时", "morning_gold", state.currentPalette, palettes)
                    TimePaletteRow("正午 11-13时", "bright_white", state.currentPalette, palettes)
                    TimePaletteRow("下午 14-17时", "warm_gold", state.currentPalette, palettes)
                    TimePaletteRow("傍晚 18-19时", "sunset_orange", state.currentPalette, palettes)
                    TimePaletteRow("夜晚 20-22时", "night_blue", state.currentPalette, palettes)
                    TimePaletteRow("深夜 23-4时", "deep_indigo", state.currentPalette, palettes)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 设置
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                innerPadding = 16.dp
            ) {
                Column {
                    Text("设置", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用环境感知", fontSize = 14.sp)
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = viewModel::toggleEnabled
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("过渡动画: ${state.transitionDuration}秒", fontSize = 14.sp)
                    Slider(
                        value = state.transitionDuration.toFloat(),
                        onValueChange = { viewModel.setTransitionDuration(it.toInt()) },
                        valueRange = 1f..10f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimePaletteRow(time: String, paletteId: String, currentPalette: String, palettes: List<AmbientPalette>) {
    val palette = palettes.find { it.id == paletteId }
    val isCurrent = paletteId == currentPalette
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontSize = 13.sp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = palette?.name ?: paletteId,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getWeatherLabel(condition: String): String {
    return when (condition) {
        "clear" -> "晴天"
        "cloudy" -> "多云"
        "overcast" -> "阴天"
        "rainy" -> "雨天"
        "snowy" -> "雪天"
        "windy" -> "大风"
        else -> "未知"
    }
}

private fun getPaletteName(id: String, palettes: List<AmbientPalette>): String {
    return palettes.find { it.id == id }?.name ?: id
}
