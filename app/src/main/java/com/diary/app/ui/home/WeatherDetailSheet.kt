package com.diary.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.weatherIconForType
import com.diary.app.weather.CurrentWeather
import com.diary.app.weather.WeatherManager

// Weather-based color palettes
private object WeatherColors {
    fun getGradient(weatherType: String): List<Color> {
        return when (weatherType) {
            "晴天" -> listOf(Color(0xFF4A90E2), Color(0xFF74B9FF), Color(0xFFA8D8FF))
            "多云" -> listOf(Color(0xFF636E72), Color(0xFFB2BEC3), Color(0xFFDFE6E9))
            "阴天" -> listOf(Color(0xFF2D3436), Color(0xFF636E72), Color(0xFFB2BEC3))
            "雨天" -> listOf(Color(0xFF2C3E50), Color(0xFF3498DB), Color(0xFF74B9FF))
            "雷暴" -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
            "雪天" -> listOf(Color(0xFFDFE6E9), Color(0xFFB2BEC3), Color(0xFF74B9FF))
            "大风" -> listOf(Color(0xFF636E72), Color(0xFFB2BEC3), Color(0xFFDFE6E9))
            else -> listOf(Color(0xFF4A90E2), Color(0xFF74B9FF), Color(0xFFA8D8FF))
        }
    }

    fun getIconColor(weatherType: String): Color {
        return when (weatherType) {
            "晴天" -> Color(0xFFFFD700)
            "多云" -> Color(0xFFB2BEC3)
            "阴天" -> Color(0xFF636E72)
            "雨天" -> Color(0xFF74B9FF)
            "雷暴" -> Color(0xFFFDCB6E)
            "雪天" -> Color(0xFFDFE6E9)
            "大风" -> Color(0xFFB2BEC3)
            else -> Color(0xFF74B9FF)
        }
    }

    fun getWeatherTip(weatherType: String): String {
        return when (weatherType) {
            "晴天" -> "今天阳光明媚，适合户外活动"
            "多云" -> "天气不错，可以出去走走"
            "阴天" -> "阴天可能较凉，记得带件外套"
            "雨天" -> "记得带伞，注意路面湿滑"
            "雷暴" -> "雷暴天气，尽量避免外出"
            "雪天" -> "路滑注意安全，注意保暖"
            "大风" -> "风大注意安全，远离高空坠物"
            else -> "今天天气不错"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailSheet(
    weather: CurrentWeather,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weatherType = WeatherManager.mapAmapWeatherToType(weather.weather)
    val gradientColors = WeatherColors.getGradient(weatherType)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }

                // Header with location and time
                WeatherDetailHeader(weather, weatherType)

                Spacer(modifier = Modifier.height(24.dp))

                // Weather tip card
                WeatherTipCard(weatherType)

                Spacer(modifier = Modifier.height(20.dp))

                // Current conditions grid
                WeatherCurrentConditions(weather)

                // Hourly forecast
                if (weather.hourlyForecast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    WeatherHourlySection(weather)
                }

                // Daily forecast
                if (weather.dailyForecast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    WeatherDailySection(weather)
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailHeader(weather: CurrentWeather, weatherType: String) {
    val iconColor = WeatherColors.getIconColor(weatherType)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Compress,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = weather.city,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weather icon with glow effect
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = weatherIconForType(weatherType),
                contentDescription = weather.weather,
                modifier = Modifier.size(48.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Temperature
        Text(
            text = "${weather.temperature}°",
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            letterSpacing = (-2).sp
        )

        // Weather description
        Text(
            text = weather.weather,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f)
        )

        // Feels like temperature
        if (weather.feelsLike.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "体感温度 ${weather.feelsLike}°C",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Update time
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "更新于 ${weather.reportTime}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun WeatherTipCard(weatherType: String) {
    val tip = WeatherColors.getWeatherTip(weatherType)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tip,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WeatherCurrentConditions(weather: CurrentWeather) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // First row: humidity and wind
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                label = "湿度",
                value = weather.humidity,
                iconColor = Color(0xFF74B9FF)
            )
            WeatherStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Air,
                label = "风向",
                value = weather.windDirection,
                iconColor = Color(0xFFB2BEC3)
            )
        }
        // Second row: wind power and report time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Thermostat,
                label = "风力",
                value = weather.windPower,
                iconColor = Color(0xFFE17055)
            )
            WeatherStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Compress,
                label = "紫外线",
                value = if (weather.uvIndex.isNotBlank()) "指数 ${weather.uvIndex}" else "-",
                iconColor = Color(0xFFFDCB6E)
            )
        }
    }
}

@Composable
private fun WeatherStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun WeatherHourlySection(weather: CurrentWeather) {
    Column {
        Text(
            text = "逐小时预报",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weather.hourlyForecast.forEach { forecast ->
                WeatherHourlyItem(forecast)
            }
        }
    }
}

@Composable
private fun WeatherHourlyItem(forecast: com.diary.app.weather.HourlyForecast) {
    val weatherType = WeatherManager.mapAmapWeatherToType(forecast.weather)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = forecast.time,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = weatherIconForType(weatherType),
            contentDescription = forecast.weather,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${forecast.temperature}°",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun WeatherDailySection(weather: CurrentWeather) {
    Column {
        Text(
            text = "未来几天",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            innerPadding = 4.dp
        ) {
            Column {
                weather.dailyForecast.forEachIndexed { index, forecast ->
                    WeatherDailyItem(forecast)
                    if (index < weather.dailyForecast.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDailyItem(forecast: com.diary.app.weather.DailyForecast) {
    val weatherType = WeatherManager.mapAmapWeatherToType(forecast.weather)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day of week
        Text(
            text = forecast.dayOfWeek,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(40.dp)
        )

        // Weather icon
        Icon(
            imageVector = weatherIconForType(weatherType),
            contentDescription = forecast.weather,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Weather description
        Text(
            text = forecast.weather,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )

        // Temperature range
        Text(
            text = "${forecast.tempMin}° / ${forecast.tempMax}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
