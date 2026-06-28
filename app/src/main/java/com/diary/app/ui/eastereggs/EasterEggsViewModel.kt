package com.diary.app.ui.eastereggs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.EasterEgg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EasterEggDefinition(
    val id: String,
    val title: String,
    val description: String
)

class EasterEggsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _discoveredEggs = MutableStateFlow<List<EasterEgg>>(emptyList())
    val discoveredEggs: StateFlow<List<EasterEgg>> = _discoveredEggs.asStateFlow()

    val allEggs = listOf(
        EasterEggDefinition("streak_100", "百日征程", "连续写作100天"),
        EasterEggDefinition("streak_30", "月度坚持", "连续写作30天"),
        EasterEggDefinition("streak_7", "七日不间断", "连续写作7天"),
        EasterEggDefinition("night_owl", "深夜笔者", "凌晨0-4点写日记"),
        EasterEggDefinition("early_bird", "晨曦记录者", "清晨5-7点写日记"),
        EasterEggDefinition("first_entry", "第一步", "写下第一篇日记"),
        EasterEggDefinition("entries_10", "十篇里程碑", "累计写10篇日记"),
        EasterEggDefinition("entries_50", "五十篇达人", "累计写50篇日记"),
        EasterEggDefinition("entries_100", "百篇里程碑", "累计写100篇日记"),
        EasterEggDefinition("long_diary", "千字长文", "单篇日记超过1000字"),
        EasterEggDefinition("full_moon", "满月之夜", "农历十五深夜写日记"),
        EasterEggDefinition("all_moods", "情绪调色盘", "使用过全部6种心情")
    )

    init {
        loadEggs()
        checkTriggers()
    }

    fun loadEggs() {
        viewModelScope.launch {
            dao.getAllEasterEggs().collect { list ->
                _discoveredEggs.value = list
            }
        }
    }

    private fun checkTriggers() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            if (entries.isEmpty()) return@launch

            val discoveredIds = _discoveredEggs.value.map { it.eggId }.toSet()
            val now = System.currentTimeMillis()
            val sorted = entries.sortedBy { it.createdAt }

            // 第一篇日记
            if ("first_entry" !in discoveredIds && sorted.isNotEmpty()) {
                dao.insertEasterEgg(EasterEgg(eggId = "first_entry", title = "第一步", description = "写下了第一篇日记", triggeredAt = sorted.first().createdAt))
            }

            // 篇数里程碑
            val count = entries.size
            if ("entries_10" !in discoveredIds && count >= 10) {
                dao.insertEasterEgg(EasterEgg(eggId = "entries_10", title = "十篇里程碑", description = "累计写了10篇日记", triggeredAt = now))
            }
            if ("entries_50" !in discoveredIds && count >= 50) {
                dao.insertEasterEgg(EasterEgg(eggId = "entries_50", title = "五十篇达人", description = "累计写了50篇日记", triggeredAt = now))
            }
            if ("entries_100" !in discoveredIds && count >= 100) {
                dao.insertEasterEgg(EasterEgg(eggId = "entries_100", title = "百篇里程碑", description = "累计写了100篇日记", triggeredAt = now))
            }

            // 千字长文
            if ("long_diary" !in discoveredIds && entries.any { it.plainText.length >= 1000 }) {
                dao.insertEasterEgg(EasterEgg(eggId = "long_diary", title = "千字长文", description = "写了一篇超过1000字的日记", triggeredAt = now))
            }

            // 时间段彩蛋
            entries.forEach { entry ->
                val hour = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalTime().hour
                if ("night_owl" !in discoveredIds && hour in 0..3) {
                    dao.insertEasterEgg(EasterEgg(eggId = "night_owl", title = "深夜笔者", description = "在凌晨写日记", triggeredAt = now))
                }
                if ("early_bird" !in discoveredIds && hour in 5..6) {
                    dao.insertEasterEgg(EasterEgg(eggId = "early_bird", title = "晨曦记录者", description = "在清晨写日记", triggeredAt = now))
                }
            }

            // 连续天数彩蛋: streak_7, streak_30, streak_100
            if (sorted.size >= 7) {
                val maxStreak = calculateMaxStreak(sorted.map { it.createdAt })
                if ("streak_7" !in discoveredIds && maxStreak >= 7) {
                    dao.insertEasterEgg(EasterEgg(eggId = "streak_7", title = "七日不间断", description = "连续写作7天", triggeredAt = now))
                }
                if ("streak_30" !in discoveredIds && maxStreak >= 30) {
                    dao.insertEasterEgg(EasterEgg(eggId = "streak_30", title = "月度坚持", description = "连续写作30天", triggeredAt = now))
                }
                if ("streak_100" !in discoveredIds && maxStreak >= 100) {
                    dao.insertEasterEgg(EasterEgg(eggId = "streak_100", title = "百日征程", description = "连续写作100天", triggeredAt = now))
                }
            }

            // 满月之夜（基于简单的近似检测：农历15日附近）
            if ("full_moon" !in discoveredIds) {
                entries.forEach { entry ->
                    val instant = java.time.Instant.ofEpochMilli(entry.createdAt)
                    val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val dayOfMonth = localDate.dayOfMonth
                    val hour = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime().hour
                    // 近似在每月14-16日且在深夜22点后
                    if (dayOfMonth in 14..16 && hour in 22..23 || hour in 0..3) {
                        dao.insertEasterEgg(EasterEgg(eggId = "full_moon", title = "满月之夜", description = "在月圆之夜写日记", triggeredAt = now))
                    }
                }
            }

            // 心情全收集
            val moodsUsed = entries.mapNotNull { it.moodLevel }.toSet()
            if ("all_moods" !in discoveredIds && moodsUsed.size >= 6) {
                dao.insertEasterEgg(EasterEgg(eggId = "all_moods", title = "情绪调色盘", description = "使用过全部6种心情", triggeredAt = now))
            }
        }
    }

    private fun calculateMaxStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val dates = timestamps.map {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }.distinct().sorted()
        var maxStreak = 1
        var currentStreak = 1
        for (i in 1 until dates.size) {
            if (dates[i].toEpochDay() - dates[i - 1].toEpochDay() == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else if (dates[i] != dates[i - 1]) {
                currentStreak = 1
            }
        }
        return maxStreak
    }
}
