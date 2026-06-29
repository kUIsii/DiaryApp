package com.diary.app.ui.eastereggs

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.diary.app.data.EasterEgg
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class EasterEggDefinition(
    val id: String,
    val title: String,
    val description: String,
    val rarity: String = "普通"
)

data class CustomEggCondition(
    val eggId: String,
    val title: String,
    val description: String,
    val type: String,
    val topic: String? = null,
    val requiredCount: Int = 1
)

class EasterEggsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val sp = application.getSharedPreferences("easter_eggs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val app = application as DiaryApplication

    private val _discoveredEggs = MutableStateFlow<List<EasterEgg>>(emptyList())
    val discoveredEggs: StateFlow<List<EasterEgg>> = _discoveredEggs.asStateFlow()

    private val _showCelebration = MutableStateFlow<EasterEgg?>(null)
    val showCelebration: StateFlow<EasterEgg?> = _showCelebration.asStateFlow()

    val allEggs = listOf(
        EasterEggDefinition("streak_100", "百日征程", "连续写作100天", "传说"),
        EasterEggDefinition("streak_30", "月度坚持", "连续写作30天", "稀有"),
        EasterEggDefinition("streak_7", "七日不间断", "连续写作7天", "普通"),
        EasterEggDefinition("night_owl", "深夜笔者", "凌晨0-4点写日记", "稀有"),
        EasterEggDefinition("early_bird", "晨曦记录者", "清晨5-7点写日记", "普通"),
        EasterEggDefinition("first_entry", "第一步", "写下第一篇日记", "普通"),
        EasterEggDefinition("entries_10", "十篇里程碑", "累计写10篇日记", "普通"),
        EasterEggDefinition("entries_50", "五十篇达人", "累计写50篇日记", "稀有"),
        EasterEggDefinition("entries_100", "百篇里程碑", "累计写100篇日记", "传说"),
        EasterEggDefinition("long_diary", "千字长文", "单篇日记超过1000字", "普通"),
        EasterEggDefinition("full_moon", "满月之夜", "农历十五深夜写日记", "稀有"),
        EasterEggDefinition("all_moods", "情绪调色盘", "使用过全部6种心情", "传说"),
        EasterEggDefinition("night_walker", "夜行侠", "深夜连续写作7天", "传说"),
        EasterEggDefinition("weekend_warrior", "周末勇士", "在周末累计写10篇日记", "稀有"),
        EasterEggDefinition("four_seasons", "四季更迭", "在春夏秋冬都写过日记", "传说"),
        EasterEggDefinition("moon_guardian", "月之守护者", "满月之夜连续写作30天", "传说"),
        EasterEggDefinition("mood_master", "百感交集", "使用全部心情并写满50篇", "传说"),
        EasterEggDefinition("life_artist", "生活美学家", "写满10篇千字日记", "稀有")
    )

    init {
        loadEggs()
        viewModelScope.launch {
            checkTriggers()
            generateCustomEggs()
        }
    }

    fun loadEggs() {
        viewModelScope.launch {
            dao.getAllEasterEggs().collect { list ->
                _discoveredEggs.value = list
            }
        }
    }

    fun dismissCelebration() {
        _showCelebration.value = null
    }

    private suspend fun checkTriggers() {
        val entries = dao.getAllEntriesOnce()
        if (entries.isEmpty()) return

        val now = System.currentTimeMillis()
        val sorted = entries.sortedBy { it.createdAt }
        var discoveredIds = _discoveredEggs.value.map { it.eggId }.toSet()
        var lastNewEgg: EasterEgg? = null

        suspend fun tryInsert(eggId: String, title: String, description: String, triggeredAt: Long = now) {
            if (eggId in discoveredIds) return
            val egg = EasterEgg(eggId = eggId, title = title, description = description, triggeredAt = triggeredAt)
            dao.insertEasterEgg(egg)
            discoveredIds = discoveredIds + eggId
            lastNewEgg = egg
        }

        if ("first_entry" !in discoveredIds && sorted.isNotEmpty()) {
            tryInsert("first_entry", "第一步", "写下了第一篇日记", sorted.first().createdAt)
        }

        val count = entries.size
        if (count >= 10) tryInsert("entries_10", "十篇里程碑", "累计写了10篇日记")
        if (count >= 50) tryInsert("entries_50", "五十篇达人", "累计写了50篇日记")
        if (count >= 100) tryInsert("entries_100", "百篇里程碑", "累计写了100篇日记")

        if ("long_diary" !in discoveredIds && entries.any { it.plainText.length >= 1000 }) {
            tryInsert("long_diary", "千字长文", "写了一篇超过1000字的日记")
        }

        entries.forEach { entry ->
            val hour = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalTime().hour
            if ("night_owl" !in discoveredIds && hour in 0..3) {
                tryInsert("night_owl", "深夜笔者", "在凌晨写日记")
            }
            if ("early_bird" !in discoveredIds && hour in 5..6) {
                tryInsert("early_bird", "晨曦记录者", "在清晨写日记")
            }
        }

        if (sorted.size >= 7) {
            val maxStreak = calculateMaxStreak(sorted.map { it.createdAt })
            if (maxStreak >= 7) tryInsert("streak_7", "七日不间断", "连续写作7天")
            if (maxStreak >= 30) tryInsert("streak_30", "月度坚持", "连续写作30天")
            if (maxStreak >= 100) tryInsert("streak_100", "百日征程", "连续写作100天")
        }

        if ("full_moon" !in discoveredIds) {
            entries.forEach { entry ->
                val instant = Instant.ofEpochMilli(entry.createdAt)
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val dayOfMonth = localDate.dayOfMonth
                val hour = instant.atZone(ZoneId.systemDefault()).toLocalTime().hour
                if (dayOfMonth in 14..16 && (hour in 22..23 || hour in 0..3)) {
                    tryInsert("full_moon", "满月之夜", "在月圆之夜写日记")
                }
            }
        }

        val moodsUsed = entries.mapNotNull { it.moodLevel }.toSet()
        if (moodsUsed.size >= 6) tryInsert("all_moods", "情绪调色盘", "使用过全部6种心情")

        if ("night_walker" !in discoveredIds && "night_owl" in discoveredIds && "streak_7" in discoveredIds) {
            tryInsert("night_walker", "夜行侠", "深夜连续写作7天")
        }
        if ("weekend_warrior" !in discoveredIds && "entries_10" in discoveredIds) {
            val weekendCount = entries.count { entry ->
                val dow = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek.value
                dow == 6 || dow == 7
            }
            if (weekendCount >= 10) tryInsert("weekend_warrior", "周末勇士", "在周末累计写10篇日记")
        }
        if ("four_seasons" !in discoveredIds) {
            val seasons = entries.map { entry ->
                val month = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate().monthValue
                when (month) { in 3..5 -> "spring"; in 6..8 -> "summer"; in 9..11 -> "autumn"; else -> "winter" }
            }.toSet()
            if (seasons.size >= 4) tryInsert("four_seasons", "四季更迭", "在春夏秋冬都写过日记")
        }
        if ("moon_guardian" !in discoveredIds && "full_moon" in discoveredIds && "streak_30" in discoveredIds) {
            tryInsert("moon_guardian", "月之守护者", "满月之夜连续写作30天")
        }
        if ("mood_master" !in discoveredIds && "all_moods" in discoveredIds && "entries_50" in discoveredIds) {
            tryInsert("mood_master", "百感交集", "使用全部心情并写满50篇")
        }
        if ("life_artist" !in discoveredIds && "long_diary" in discoveredIds) {
            val longCount = entries.count { it.plainText.length >= 1000 }
            if (longCount >= 10) tryInsert("life_artist", "生活美学家", "写满10篇千字日记")
        }

        checkCustomConditions(discoveredIds, entries)?.let { egg ->
            dao.insertEasterEgg(egg)
            lastNewEgg = egg
        }

        if (lastNewEgg != null) {
            _showCelebration.value = lastNewEgg
        }
    }

    private suspend fun checkCustomConditions(discoveredIds: Set<String>, entries: List<DiaryEntry>): EasterEgg? {
        val json = sp.getString("custom_egg_conditions", null) ?: return null
        val type = object : TypeToken<List<CustomEggCondition>>() {}.type
        val conditions: List<CustomEggCondition> = try {
            gson.fromJson(json, type)
        } catch (_: Exception) { return null }
        for (c in conditions) {
            if (c.eggId in discoveredIds) continue
            val matched = when (c.type) {
                "WRITE_ABOUT_TOPIC_N_TIMES" -> {
                    val topic = c.topic ?: continue
                    entries.count { it.plainText.contains(topic, ignoreCase = true) } >= c.requiredCount
                }
                else -> false
            }
            if (matched) {
                return EasterEgg(eggId = c.eggId, title = c.title, description = c.description)
            }
        }
        return null
    }

    private fun generateCustomEggs() {
        viewModelScope.launch {
            if (!app.aiService.isAiEnabled()) return@launch
            val lastAnalysis = sp.getLong("last_custom_egg_analysis", 0L)
            if (System.currentTimeMillis() - lastAnalysis < 30L * 24 * 60 * 60 * 1000) return@launch
            val entries = dao.getAllEntriesOnce()
            if (entries.isEmpty()) return@launch
            val totalCount = entries.size
            val avgWords = if (entries.isNotEmpty()) entries.sumOf { it.plainText.length } / entries.size else 0
            val hourCounts = entries.groupBy {
                Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalTime().hour
            }
            val commonHour = hourCounts.maxByOrNull { it.value.size }?.key
            val moodDist = entries.groupBy { it.moodLevel }.mapValues { it.value.size }
            val prompt = "分析以下日记数据：总篇数=$totalCount、平均字数=$avgWords、常见写作时间=${commonHour}时、心情分布=$moodDist。生成3个个性化彩蛋建议，要求独特且有纪念意义。输出JSON: [{eggId, title, description, triggerCondition}]"
            val result = app.aiService.chat(aiRequest(userMessage = prompt, maxTokens = 1024))
            result.onSuccess { response ->
                try {
                    val conditions = parseCustomConditions(response.content)
                    saveCustomConditions(conditions)
                    sp.edit().putLong("last_custom_egg_analysis", System.currentTimeMillis()).apply()
                } catch (_: Exception) { }
            }
        }
    }

    private fun parseCustomConditions(json: String): List<CustomEggCondition> {
        val type = object : TypeToken<List<CustomEggCondition>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    }

    private fun saveCustomConditions(conditions: List<CustomEggCondition>) {
        sp.edit().putString("custom_egg_conditions", gson.toJson(conditions)).apply()
    }

    private fun calculateMaxStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val dates = timestamps.map {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
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
