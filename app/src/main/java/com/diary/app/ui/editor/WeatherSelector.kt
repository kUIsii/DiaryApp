package com.diary.app.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.theme.WeatherCloudy
import com.diary.app.ui.theme.WeatherOvercast
import com.diary.app.ui.theme.WeatherRainy
import com.diary.app.ui.theme.WeatherStormy
import com.diary.app.ui.theme.WeatherSunny
import com.diary.app.ui.theme.WeatherWindy

data class WeatherOption(val name: String, val icon: ImageVector)

val weatherOptions = listOf(
    WeatherOption("晴天", Icons.Default.WbSunny),
    WeatherOption("多云", Icons.Default.Cloud),
    WeatherOption("阴天", Icons.Default.CloudQueue),
    WeatherOption("雨天", Icons.Default.Umbrella),
    WeatherOption("雷暴", Icons.Default.Thunderstorm),
    WeatherOption("大风", Icons.Default.Air)
)

private fun weatherThemeColor(name: String): Color = when (name) {
    "晴天" -> WeatherSunny
    "多云" -> WeatherCloudy
    "阴天" -> WeatherOvercast
    "雨天" -> WeatherRainy
    "雷暴" -> WeatherStormy
    "大风" -> WeatherWindy
    else -> WeatherSunny
}

fun getWeatherIcon(name: String?): ImageVector? {
    return weatherOptions.find { it.name == name }?.icon
}

@Composable
fun WeatherSelector(
    selectedWeather: String?,
    onWeatherSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weatherOptions.forEach { option ->
            WeatherItem(
                option = option,
                isSelected = selectedWeather == option.name,
                onClick = { onWeatherSelected(if (selectedWeather == option.name) null else option.name) }
            )
        }
    }
}

@Composable
private fun WeatherItem(
    option: WeatherOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 按压缩放动画
    var pressed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "weather_press_scale"
    )

    val themeColor = weatherThemeColor(option.name)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            // 外圈光环效果（仅选中）
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.1f))
                )
            }

            // 圆形背景
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.25f),
                                    themeColor.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.08f),
                                    themeColor.copy(alpha = 0.04f)
                                )
                            )
                        }
                    )
            ) {
                Icon(
                    option.icon,
                    contentDescription = option.name,
                    tint = if (isSelected) themeColor else themeColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(if (isSelected) 28.dp else 22.dp)
                )
            }
        }

        Text(
            text = option.name,
            fontSize = if (isSelected) 14.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
    }

    // 重置 pressed 状态
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}
