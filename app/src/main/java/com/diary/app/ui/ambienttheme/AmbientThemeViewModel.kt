package com.diary.app.ui.ambienttheme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

data class AmbientThemeState(
    val isEnabled: Boolean = true,
    val timeOfDay: TimeOfDay = TimeOfDay.AFTERNOON,
    val weatherCondition: String = "clear",
    val transitionDuration: Int = 5,  // 秒
    val currentPalette: String = "warm_gold"
)

enum class TimeOfDay(val label: String, val hourRange: IntRange) {
    DAWN("黎明", 5..6),
    MORNING("清晨", 7..10),
    NOON("正午", 11..13),
    AFTERNOON("下午", 14..17),
    EVENING("傍晚", 18..19),
    NIGHT("夜晚", 20..22),
    LATE_NIGHT("深夜", 23..4)
}

class AmbientThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    
    private val _state = MutableStateFlow(AmbientThemeState())
    val state: StateFlow<AmbientThemeState> = _state
    
    private val _availablePalettes = MutableStateFlow<List<AmbientPalette>>(getDefaultPalettes())
    val availablePalettes: StateFlow<List<AmbientPalette>> = _availablePalettes
    
    init {
        updateTimeOfDay()
        loadCurrentWeather()
    }
    
    fun toggleEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
    }
    
    fun setTransitionDuration(seconds: Int) {
        _state.value = _state.value.copy(transitionDuration = seconds)
    }
    
    private fun updateTimeOfDay() {
        val hour = LocalTime.now().hour
        val timeOfDay = TimeOfDay.values().find { hour in it.hourRange } ?: TimeOfDay.AFTERNOON
        val palette = getPaletteForTime(timeOfDay)
        _state.value = _state.value.copy(
            timeOfDay = timeOfDay,
            currentPalette = palette
        )
    }
    
    private fun loadCurrentWeather() {
        viewModelScope.launch {
            // 从现有的天气管理器获取缓存的天气
            try {
                val current = com.diary.app.weather.WeatherManager.getCachedWeather(app)
                if (current != null) {
                    val condition = mapWeatherToCondition(current.weather)
                    val palette = adjustPaletteForWeather(_state.value.currentPalette, condition)
                    _state.value = _state.value.copy(
                        weatherCondition = condition,
                        currentPalette = palette
                    )
                }
            } catch (e: Exception) {
                // 天气获取失败，保持当前主题
            }
        }
    }
    
    private fun mapWeatherToCondition(weather: String): String {
        return when {
            weather.contains("晴") -> "clear"
            weather.contains("多云") -> "cloudy"
            weather.contains("阴") -> "overcast"
            weather.contains("雨") -> "rainy"
            weather.contains("雪") -> "snowy"
            weather.contains("风") -> "windy"
            else -> "clear"
        }
    }
    
    private fun getPaletteForTime(timeOfDay: TimeOfDay): String {
        return when (timeOfDay) {
            TimeOfDay.DAWN -> "dawn_pink"
            TimeOfDay.MORNING -> "morning_gold"
            TimeOfDay.NOON -> "bright_white"
            TimeOfDay.AFTERNOON -> "warm_gold"
            TimeOfDay.EVENING -> "sunset_orange"
            TimeOfDay.NIGHT -> "night_blue"
            TimeOfDay.LATE_NIGHT -> "deep_indigo"
        }
    }
    
    private fun adjustPaletteForWeather(palette: String, condition: String): String {
        return when (condition) {
            "rainy" -> "rainy_grey"
            "snowy" -> "snowy_white"
            "overcast" -> "overcast_grey"
            else -> palette  // 晴天等不改变
        }
    }
    
    companion object {
        fun getDefaultPalettes(): List<AmbientPalette> = listOf(
            AmbientPalette("dawn_pink", "黎明粉", "#FFE4E1", "#FFB6C1"),
            AmbientPalette("morning_gold", "晨光金", "#FFF8DC", "#FFD700"),
            AmbientPalette("bright_white", "正午白", "#FFFFFF", "#F0F0F0"),
            AmbientPalette("warm_gold", "午后暖金", "#FAF0E6", "#DEB887"),
            AmbientPalette("sunset_orange", "傍晚橘", "#FFE4B5", "#FF8C00"),
            AmbientPalette("night_blue", "夜晚蓝", "#191970", "#000080"),
            AmbientPalette("deep_indigo", "深夜靛", "#0A0A2E", "#1A1A4E"),
            AmbientPalette("rainy_grey", "雨天灰", "#B0B0B0", "#808080"),
            AmbientPalette("snowy_white", "雪天白", "#FFFAFA", "#F5F5F5"),
            AmbientPalette("overcast_grey", "阴天灰", "#C0C0C0", "#A0A0A0")
        )
    }
}

data class AmbientPalette(
    val id: String,
    val name: String,
    val primaryColor: String,
    val accentColor: String
)
