package com.diary.app.ui.stats

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.TagUsage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MoodStat(
    val level: Int,
    val count: Int,
    val label: String,
    val color: Color,
)

data class WeatherStat(
    val type: String,
    val count: Int,
)

data class StatsState(
    val totalEntries: Int = 0,
    val currentStreak: Int = 0,
    val thisMonthEntries: Int = 0,
    val moodDistribution: List<MoodStat> = emptyList(),
    val weatherDistribution: List<WeatherStat> = emptyList(),
    val tagUsage: List<TagUsage> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val state: StateFlow<StatsState> = combine(
        dao.getAllEntries(),
        dao.getTagUsage()
    ) { entries, tagUsage ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()

        val dates = entries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }.toSet()

        val streak = calculateStreak(dates)

        val thisMonth = entries.count {
            val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            date.year == now.year && date.monthValue == now.monthValue
        }

        val moodDistribution = (1..6).map { level ->
            val count = entries.count { it.moodLevel == level }
            MoodStat(
                level = level,
                count = count,
                label = moodLabels[level]!!,
                color = moodColors[level]!!,
            )
        }

        val weatherDistribution = entries.filter { !it.weather.isNullOrBlank() }
            .groupBy { it.weather!! }
            .map { (type, list) -> WeatherStat(type, list.size) }
            .sortedByDescending { it.count }

        StatsState(
            totalEntries = entries.size,
            currentStreak = streak,
            thisMonthEntries = thisMonth,
            moodDistribution = moodDistribution,
            weatherDistribution = weatherDistribution,
            tagUsage = tagUsage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    private fun calculateStreak(dates: Set<LocalDate>): Int {
        var streak = 0
        var current = LocalDate.now()
        while (dates.contains(current)) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    companion object {
        val moodLabels = mapOf(
            1 to "沮丧",
            2 to "低落",
            3 to "平静",
            4 to "开心",
            5 to "愉快",
            6 to "兴奋",
        )

        val moodColors = mapOf(
            1 to Color(0xFFE74C3C),
            2 to Color(0xFFE67E22),
            3 to Color(0xFFF39C12),
            4 to Color(0xFF9CCC65),
            5 to Color(0xFF66BB6A),
            6 to Color(0xFF2E7D32),
        )
    }
}
