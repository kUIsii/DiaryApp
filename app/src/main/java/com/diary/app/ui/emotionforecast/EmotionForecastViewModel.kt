package com.diary.app.ui.emotionforecast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class EmotionForecastData(
    val forecastLabel: String,
    val confidence: Float,
    val reasons: List<ForecastReasonItem>,
    val recentMoods: List<Int>,
    val trend: String
)

data class ForecastReasonItem(
    val reason: String,
    val impact: String
)

class EmotionForecastViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _forecast = MutableStateFlow<EmotionForecastData?>(null)
    val forecast: StateFlow<EmotionForecastData?> = _forecast.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        generateForecast()
    }

    fun generateForecast() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            val entries = dao.getAllEntriesOnce()
            if (entries.size < 3) {
                _forecast.value = null
                _errorMsg.value = "需要至少3篇日记才能生成情绪预报，当前${entries.size}篇"
                _isLoading.value = false
                return@launch
            }

            val recentEntries = entries.sortedByDescending { it.createdAt }.take(14)
            val moods = recentEntries.mapNotNull { it.moodLevel }
            if (moods.isEmpty()) {
                _forecast.value = null
                _errorMsg.value = "最近的日记未记录心情，记录心情后可以生成更准确的预报"
                _isLoading.value = false
                return@launch
            }

            val avgMood = moods.average()
            val recentMoods7 = moods.take(7)
            val recentAvg = recentMoods7.average()

            // Day of week effect
            val tomorrow = LocalDate.now().plusDays(1)
            val dayOfWeek = tomorrow.dayOfWeek
            val dayMoods = entries.filter { entry ->
                val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                entryDate.dayOfWeek == dayOfWeek
            }.mapNotNull { it.moodLevel }
            val dayAvg = if (dayMoods.isNotEmpty()) dayMoods.average() else avgMood

            // Trend
            val trend = when {
                recentAvg > avgMood + 0.5 -> "上升"
                recentAvg < avgMood - 0.5 -> "下降"
                else -> "稳定"
            }

            // Forecast label
            val combined = (recentAvg * 0.6 + dayAvg * 0.4)
            val forecastLabel = when {
                combined >= 5.0 -> "积极愉快"
                combined >= 4.0 -> "平静偏积极"
                combined >= 3.0 -> "平静中性"
                combined >= 2.0 -> "平静偏低落"
                else -> "需要关注"
            }

            val reasons = mutableListOf<ForecastReasonItem>()
            reasons.add(ForecastReasonItem(
                "最近7天情绪平均${"%.1f".format(recentAvg)}，趋势${trend}",
                if (recentAvg >= 3.5) "积极" else if (recentAvg >= 2.5) "中性" else "低落"
            ))
            if (dayMoods.size >= 2) {
                reasons.add(ForecastReasonItem(
                    "周${dayOfWeekChinese(dayOfWeek)}你通常心情${if (dayAvg >= 3.5) "不错" else "一般"}",
                    if (dayAvg >= 3.5) "期待" else "平稳"
                ))
            }
            val latestWeather = recentEntries.firstOrNull()?.weather
            if (latestWeather != null) {
                reasons.add(ForecastReasonItem(
                    "最近天气为$latestWeather",
                    when (latestWeather) {
                        "晴天" -> "正面影响"
                        "雨天" -> "可能低落"
                        else -> "中性影响"
                    }
                ))
            }

            _forecast.value = EmotionForecastData(
                forecastLabel = forecastLabel,
                confidence = (moods.size.toFloat() / 14f).coerceAtMost(1f),
                reasons = reasons,
                recentMoods = moods,
                trend = trend
            )
            _isLoading.value = false
        }
    }

    private fun dayOfWeekChinese(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
}
