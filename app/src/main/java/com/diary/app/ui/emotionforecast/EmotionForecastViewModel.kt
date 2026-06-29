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
import java.time.temporal.ChronoUnit

data class EmotionForecastData(
    val forecastLabel: String,
    val confidence: Float,
    val reasons: List<ForecastReasonItem>,
    val recentMoods: List<Int>,
    val trend: String,
    val dailyMoods: List<DailyMood> = emptyList(),
    val weeklySummary: String = "",
    val triggers: List<TriggerItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val calendarData: List<CalendarDay> = emptyList()
)

data class DailyMood(
    val date: LocalDate,
    val moodLevel: Int,
    val note: String = ""
)

data class TriggerItem(
    val trigger: String,
    val correlation: String
)

data class CalendarDay(
    val date: LocalDate,
    val moodLevel: Int?
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

            val tomorrow = LocalDate.now().plusDays(1)
            val dayOfWeek = tomorrow.dayOfWeek
            val dayMoods = entries.filter { entry ->
                val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                entryDate.dayOfWeek == dayOfWeek
            }.mapNotNull { it.moodLevel }
            val dayAvg = if (dayMoods.isNotEmpty()) dayMoods.average() else avgMood

            val trend = when {
                recentAvg > avgMood + 0.5 -> "上升"
                recentAvg < avgMood - 0.5 -> "下降"
                else -> "稳定"
            }

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
                    "最近天气为${latestWeather}",
                    when (latestWeather) {
                        "晴天" -> "正面影响"
                        "雨天" -> "可能低落"
                        else -> "中性影响"
                    }
                ))
            }

            val today = LocalDate.now()
            val dailyMoods = (0L..6L).map { daysAgo ->
                val date = today.minusDays(daysAgo)
                val dayEntries = entries.filter { entry ->
                    val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    entryDate == date
                }
                val dayMood = dayEntries.mapNotNull { it.moodLevel }.average().toInt().coerceIn(1, 6)
                DailyMood(date = date, moodLevel = if (dayEntries.isNotEmpty()) dayMood else 3)
            }.reversed()

            val weekEntries = entries.filter { entry ->
                val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                ChronoUnit.DAYS.between(entryDate, today) <= 7
            }
            val weekMoods = weekEntries.mapNotNull { it.moodLevel }
            val weekAvg = if (weekMoods.isNotEmpty()) weekMoods.average() else 3.0
            val maxW = weekMoods.maxOrNull()
            val minW = weekMoods.minOrNull()
            val weeklySummary = "本周情绪平均${"%.1f".format(weekAvg)}分，" +
                    "整体${if (weekAvg >= 4.0) "积极向上" else if (weekAvg >= 3.0) "平稳" else "偏低"}。" +
                    "本周共${weekEntries.size}篇日记。" +
                    "${if (maxW != null && minW != null) "最高${maxW}分，最低${minW}分。" else ""}"

            val triggers = mutableListOf<TriggerItem>()
            val weatherMoods = entries.filter { it.weather != null && it.moodLevel != null }
            val weatherGroups = weatherMoods.groupBy { it.weather }
            weatherGroups.forEach { (weather, group) ->
                val avg = group.mapNotNull { it.moodLevel }.average()
                if (group.size >= 2) {
                    triggers.add(TriggerItem(
                        trigger = "天气：${weather}",
                        correlation = if (avg >= 4.0) "偏高" else if (avg <= 2.5) "偏低" else "中性"
                    ))
                }
            }
            val morningEntries = entries.filter {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.createdAt }
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 6..11
            }.mapNotNull { it.moodLevel }
            if (morningEntries.size >= 2) {
                val morningAvg = morningEntries.average()
                triggers.add(TriggerItem(
                    trigger = "早晨写作",
                    correlation = if (morningAvg >= 4.0) "偏高" else "中性"
                ))
            }

            val suggestions = mutableListOf<String>()
            when {
                weekAvg < 2.5 -> {
                    suggestions.add("出去走走，呼吸新鲜空气")
                    suggestions.add("和朋友聊聊最近的感受")
                    suggestions.add("尝试写一些开心的事")
                }
                weekAvg < 3.5 -> {
                    suggestions.add("听一首喜欢的音乐")
                    suggestions.add("回顾最近让你开心的事")
                    suggestions.add("尝试新的兴趣爱好")
                }
                weekAvg >= 4.5 -> {
                    suggestions.add("保持积极状态，记录美好瞬间")
                    suggestions.add("可以把快乐分享给身边的人")
                }
                else -> {
                    suggestions.add("保持当前节奏，继续记录生活")
                    suggestions.add("尝试探索新的写作主题")
                }
            }
            if (entries.any { it.weather == "雨天" }) {
                suggestions.add("雨天适合泡杯热茶，静心写作")
            }

            val calendarData = (0L..59L).map { daysAgo ->
                val date = today.minusDays(daysAgo)
                val dayEntries = entries.filter { entry ->
                    val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    entryDate == date
                }
                val mood = dayEntries.mapNotNull { it.moodLevel }.maxOrNull()
                CalendarDay(date = date, moodLevel = mood)
            }.reversed()

            _forecast.value = EmotionForecastData(
                forecastLabel = forecastLabel,
                confidence = (moods.size.toFloat() / 14f).coerceAtMost(1f),
                reasons = reasons,
                recentMoods = moods,
                trend = trend,
                dailyMoods = dailyMoods,
                weeklySummary = weeklySummary,
                triggers = triggers,
                suggestions = suggestions,
                calendarData = calendarData
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
