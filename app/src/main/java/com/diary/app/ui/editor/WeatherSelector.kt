package com.diary.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WeatherOption(val name: String, val icon: String)

val weatherOptions = listOf(
    WeatherOption("晴天", "☀"),
    WeatherOption("多云", "⛅"),
    WeatherOption("阴天", "☁"),
    WeatherOption("雨天", "☔"),
    WeatherOption("雪天", "❄"),
    WeatherOption("大风", "💨")
)

@Composable
fun WeatherSelector(
    selectedWeather: String?,
    onWeatherSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        weatherOptions.forEach { option ->
            val isSelected = selectedWeather == option.name
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        onWeatherSelected(if (isSelected) null else option.name)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = option.icon, fontSize = 14.sp)
                    Text(
                        text = option.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
