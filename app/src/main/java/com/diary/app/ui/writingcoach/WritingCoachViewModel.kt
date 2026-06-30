package com.diary.app.ui.writingcoach

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.diary.app.data.WritingCoach
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeRange(val label: String) {
    THIS_MONTH("本月"),
    LAST_MONTH("上个月"),
    THIS_QUARTER("本季度")
}

enum class TrendMetric(val label: String) {
    WORDS("字数"),
    VOCAB("词汇丰富度"),
    SENTENCE("句长")
}

data class AiAnalysisResult(
    val writingStyle: String,
    val emotionTrack: String,
    val themePreference: String,
    val summary: String
)

data class WritingMonthlyStats(
    val yearMonth: String,
    val avgWordCount: Float,
    val avgSentenceLength: Float,
    val vocabularyRichness: Float,
    val totalEntries: Int
)

data class WritingWeeklyStats(
    val weekKey: String,
    val weekStart: String,
    val avgWordCount: Float,
    val avgSentenceLength: Float,
    val vocabularyRichness: Float
)

data class AiSuggestion(
    val text: String,
    val isAiGenerated: Boolean
)

data class WritingCoachGrowth(
    val dailyProgressText: String,
    val weeklyProgressText: String,
    val writingFrequencyText: String,
    val analysisSourceText: String,
    val nextActionTitle: String,
    val nextActionDescription: String
)

private data class AiSuggestionCache(
    val suggestions: List<String>,
    val timestamp: Long
)

private data class AiAnalysisCache(
    val result: AiAnalysisResult,
    val timestamp: Long
)

inline fun <reified T> SharedPreferences.getObject(key: String): T? {
    val json = getString(key, null) ?: return null
    return try { Gson().fromJson(json, object : TypeToken<T>() {}.type) } catch (e: Exception) { null }
}

inline fun <reified T> SharedPreferences.putObject(key: String, value: T) {
    edit().putString(key, Gson().toJson(value)).apply()
}

fun buildWritingCoachGrowth(
    analysis: WritingCoach.WritingAnalysis?,
    currentStats: WritingMonthlyStats?,
    dailyWordGoal: Int,
    weeklyDayGoal: Int,
    todayWordCount: Int,
    thisWeekWritingDays: Int,
    aiEnabled: Boolean,
    aiAnalysisResult: AiAnalysisResult?,
    aiSuggestions: List<AiSuggestion>
): WritingCoachGrowth {
    val analysisSafe = analysis ?: WritingCoach.WritingAnalysis()
    val dailyProgress = "${todayWordCount}/${dailyWordGoal} 字"
    val weeklyProgress = "${thisWeekWritingDays}/${weeklyDayGoal} 天"
    val writingFrequency = when {
        analysisSafe.totalEntries == 0 -> "还没有写作数据"
        thisWeekWritingDays >= weeklyDayGoal -> "节奏稳定，正在形成习惯"
        todayWordCount >= dailyWordGoal -> "今日目标已完成"
        else -> "继续保持每天一点点"
    }
    val analysisSource = when {
        aiAnalysisResult != null && aiEnabled -> "AI 结果已同步到本地教练"
        aiSuggestions.any { it.isAiGenerated } && aiEnabled -> "AI 建议和本地建议已合并"
        aiEnabled -> "AI 可用，正在等待新分析"
        else -> "AI 关闭，当前使用本地分析"
    }
    val nextActionTitle = when {
        todayWordCount < dailyWordGoal -> "补今天的字数"
        thisWeekWritingDays < weeklyDayGoal -> "补本周写作天数"
        analysisSafe.suggestions.isNotEmpty() -> "按建议微调下一篇"
        else -> "开始一次新分析"
    }
    val nextActionDescription = when {
        todayWordCount < dailyWordGoal ->
            "先写到 ${dailyWordGoal - todayWordCount} 字，完成今天的最小闭环。"
        thisWeekWritingDays < weeklyDayGoal ->
            "本周还差 ${weeklyDayGoal - thisWeekWritingDays} 天，安排一次 10 分钟写作。"
        analysisSafe.suggestions.isNotEmpty() ->
            "从本地分析里挑一条建议，直接应用到下一篇日记。"
        else ->
            "你已经有稳定习惯了，可以刷新 AI 分析看看新的侧面。"
    }
    return WritingCoachGrowth(
        dailyProgressText = dailyProgress,
        weeklyProgressText = weeklyProgress,
        writingFrequencyText = writingFrequency,
        analysisSourceText = analysisSource,
        nextActionTitle = nextActionTitle,
        nextActionDescription = nextActionDescription
    )
}

class WritingCoachViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("writing_coach", Context.MODE_PRIVATE)
    private val app = application as DiaryApplication
    private val gson = Gson()

    private var allEntries: List<DiaryEntry> = emptyList()

    private val _analysis = MutableStateFlow<WritingCoach.WritingAnalysis?>(null)
    val analysis: StateFlow<WritingCoach.WritingAnalysis?> = _analysis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    private val _currentStats = MutableStateFlow<WritingMonthlyStats?>(null)
    val currentStats: StateFlow<WritingMonthlyStats?> = _currentStats

    private val _previousStats = MutableStateFlow<WritingMonthlyStats?>(null)
    val previousStats: StateFlow<WritingMonthlyStats?> = _previousStats

    private val _aiAnalysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysisResult: StateFlow<AiAnalysisResult?> = _aiAnalysisResult

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading

    val aiEnabled: Boolean get() = app.aiService.isAiEnabled()

    private val _dailyWordGoal = MutableStateFlow(200)
    val dailyWordGoal: StateFlow<Int> = _dailyWordGoal

    private val _weeklyDayGoal = MutableStateFlow(5)
    val weeklyDayGoal: StateFlow<Int> = _weeklyDayGoal

    private val _todayWordCount = MutableStateFlow(0)
    val todayWordCount: StateFlow<Int> = _todayWordCount

    private val _thisWeekWritingDays = MutableStateFlow(0)
    val thisWeekWritingDays: StateFlow<Int> = _thisWeekWritingDays

    private val _trendChartData = MutableStateFlow<List<WritingWeeklyStats>>(emptyList())
    val trendChartData: StateFlow<List<WritingWeeklyStats>> = _trendChartData

    private val _selectedTrendMetric = MutableStateFlow(TrendMetric.WORDS)
    val selectedTrendMetric: StateFlow<TrendMetric> = _selectedTrendMetric

    private val _hourDistribution = MutableStateFlow(List(24) { 0 })
    val hourDistribution: StateFlow<List<Int>> = _hourDistribution

    private val _aiSuggestions = MutableStateFlow<List<AiSuggestion>>(emptyList())
    val aiSuggestions: StateFlow<List<AiSuggestion>> = _aiSuggestions

    private val _growth = MutableStateFlow(
        buildWritingCoachGrowth(
            analysis = null,
            currentStats = null,
            dailyWordGoal = _dailyWordGoal.value,
            weeklyDayGoal = _weeklyDayGoal.value,
            todayWordCount = _todayWordCount.value,
            thisWeekWritingDays = _thisWeekWritingDays.value,
            aiEnabled = aiEnabled,
            aiAnalysisResult = null,
            aiSuggestions = emptyList()
        )
    )
    val growth: StateFlow<WritingCoachGrowth> = _growth

    init {
        loadGoals()
        refreshGrowth()
        analyze()
    }

    private fun loadGoals() {
        _dailyWordGoal.value = prefs.getInt("writing_goal_daily_words", 200)
        _weeklyDayGoal.value = prefs.getInt("writing_goal_weekly_days", 5)
    }

    fun setDailyWordGoal(value: Int) {
        _dailyWordGoal.value = value
        prefs.edit().putInt("writing_goal_daily_words", value).apply()
        refreshGrowth()
    }

    fun setWeeklyDayGoal(value: Int) {
        _weeklyDayGoal.value = value
        prefs.edit().putInt("writing_goal_weekly_days", value).apply()
        refreshGrowth()
    }

    fun selectTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        computeTimeRangeStats()
        computeHourDistribution()
        refreshGrowth()
    }

    fun selectTrendMetric(metric: TrendMetric) {
        _selectedTrendMetric.value = metric
    }

    fun analyze() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allEntries = dao.getAllEntriesOnce()
                _analysis.value = WritingCoach.analyzeWritingPatterns(allEntries)
                computeTodayStats()
                computeWeekStats()
                computeTimeRangeStats()
                computeWeeklyChartData()
                computeHourDistribution()
                loadCachedAiAnalysis()
                generateAiSuggestions()
                mergeSuggestions(null)
                refreshGrowth()
            } catch (e: Exception) {
                _analysis.value = null
                refreshGrowth()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestAiAnalysis() {
        if (!aiEnabled) return
        val lastTime = prefs.getLong("last_ai_analysis_time", 0L)
        if (System.currentTimeMillis() - lastTime < 60_000) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val stats = _analysis.value ?: return@launch
                val prompt = buildString {
                    appendLine("我有${stats.totalEntries}篇日记，平均每篇${stats.avgWordCount.toInt()}字，平均句长${stats.avgSentenceLength.toInt()}字，词汇丰富度${(stats.vocabularyRichness * 100).toInt()}%。")
                    appendLine("写作时间模式：${stats.writingTimePattern}。")
                    if (stats.emotionDistribution.isNotEmpty()) {
                        appendLine("情绪分布：${stats.emotionDistribution.entries.joinToString("，") { "${it.key}${it.value}篇" }}。")
                    }
                    if (stats.topRepeatedWords.isNotEmpty()) {
                        appendLine("高频词：${stats.topRepeatedWords.take(5).joinToString("、") { "${it.first}(${it.second}次)" }}。")
                    }
                }
                val request = aiRequest(
                    userMessage = prompt,
                    systemPrompt = "你是一个写作教练。根据用户的日记数据，分析其写作风格、情感变化和主题偏好。用中文回答。要求输出格式：写作风格：xxx\n情感轨迹：xxx\n主题偏好：xxx\n一句话点评：xxx"
                )
                val result = app.aiService.chat(request, useCache = false)
                result.onSuccess { response ->
                    val parsed = parseAiAnalysisResult(response.content)
                    _aiAnalysisResult.value = parsed
                    prefs.edit().putLong("last_ai_analysis_time", System.currentTimeMillis()).apply()
                    val cache = AiAnalysisCache(parsed, System.currentTimeMillis())
                    prefs.edit().putString("ai_analysis_cache", gson.toJson(cache)).apply()
                    refreshGrowth()
                }
            } catch (_: Exception) {
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun loadCachedAiAnalysis() {
        val json = prefs.getString("ai_analysis_cache", null) ?: return
        try {
            val cache = gson.fromJson(json, AiAnalysisCache::class.java)
            if (System.currentTimeMillis() - cache.timestamp < 24 * 60 * 60 * 1000) {
                _aiAnalysisResult.value = cache.result
                refreshGrowth()
            }
        } catch (_: Exception) {}
    }

    private fun parseAiAnalysisResult(response: String): AiAnalysisResult {
        val lines = response.trim().split("\n").map { it.trim() }
        val style = lines.find { it.startsWith("写作风格") }?.substringAfter(":")?.substringAfter("：")?.trim() ?: ""
        val emotion = lines.find { it.startsWith("情感轨迹") }?.substringAfter(":")?.substringAfter("：")?.trim() ?: ""
        val theme = lines.find { it.startsWith("主题偏好") }?.substringAfter(":")?.substringAfter("：")?.trim() ?: ""
        val summary = lines.find { it.startsWith("一句话点评") }?.substringAfter(":")?.substringAfter("：")?.trim() ?: ""
        return AiAnalysisResult(style, emotion, theme, summary)
    }

    private fun computeTodayStats() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val todayEntries = allEntries.filter { it.createdAt >= todayStart }
        _todayWordCount.value = todayEntries.sumOf { it.plainText.split(Regex("\\s+")).size }
    }

    private fun computeWeekStats() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        val weekEntries = allEntries.filter { it.createdAt >= weekStart }
        val daySet = weekEntries.mapTo(mutableSetOf()) {
            val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            c.get(Calendar.DAY_OF_YEAR)
        }
        _thisWeekWritingDays.value = daySet.size
    }

    private fun computeTimeRangeStats() {
        val range = _selectedTimeRange.value
        val (start, end) = getTimeRangeTimestamps(range)
        val filtered = allEntries.filter { it.createdAt in start..end }
        val duration = end - start
        val prevEnd = start - 1
        val prevStart = prevEnd - duration
        val prevFiltered = allEntries.filter { it.createdAt in prevStart..prevEnd }
        _currentStats.value = computeStatsForEntries(filtered)
        _previousStats.value = computeStatsForEntries(prevFiltered)
        val cacheKey = "writing_stats_${getYearMonthString(range)}"
        if (filtered.isNotEmpty()) {
            val stats = _currentStats.value
            if (stats != null) {
                prefs.putObject(cacheKey, stats)
            }
        }
    }

    private fun computeStatsForEntries(entries: List<DiaryEntry>): WritingMonthlyStats {
        if (entries.isEmpty()) return WritingMonthlyStats("", 0f, 0f, 0f, 0)
        val analysis = WritingCoach.analyzeWritingPatterns(entries)
        return WritingMonthlyStats(
            yearMonth = "",
            avgWordCount = analysis.avgWordCount,
            avgSentenceLength = analysis.avgSentenceLength,
            vocabularyRichness = analysis.vocabularyRichness,
            totalEntries = analysis.totalEntries
        )
    }

    private fun computeWeeklyChartData() {
        val cal = Calendar.getInstance()
        val weeks = mutableListOf<WritingWeeklyStats>()
        for (i in 0 until 12) {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val weekEnd = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val weekStart = cal.timeInMillis
            val year = cal.get(Calendar.YEAR)
            val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
            val weekKey = "${year}_${String.format("%02d", weekNum)}"
            val weekLabel = "${year % 100}W${weekNum}"
            val weekEntries = allEntries.filter { it.createdAt in weekStart..weekEnd }
            if (weekEntries.isNotEmpty()) {
                val analysis = WritingCoach.analyzeWritingPatterns(weekEntries)
                weeks.add(
                    WritingWeeklyStats(
                        weekKey = weekKey,
                        weekStart = weekLabel,
                        avgWordCount = analysis.avgWordCount,
                        avgSentenceLength = analysis.avgSentenceLength,
                        vocabularyRichness = analysis.vocabularyRichness
                    )
                )
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        _trendChartData.value = weeks.reversed()
    }

    private fun computeHourDistribution() {
        val range = _selectedTimeRange.value
        val (start, end) = getTimeRangeTimestamps(range)
        val filtered = allEntries.filter { it.createdAt in start..end }
        val hours = MutableList(24) { 0 }
        filtered.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            val h = c.get(Calendar.HOUR_OF_DAY)
            hours[h] = hours[h] + 1
        }
        _hourDistribution.value = hours
    }

    private fun generateAiSuggestions() {
        if (!aiEnabled) return
        viewModelScope.launch {
            val cachedJson = prefs.getString("ai_suggestions_cache", null)
            if (cachedJson != null) {
                try {
                    val cache = gson.fromJson(cachedJson, AiSuggestionCache::class.java)
                    if (System.currentTimeMillis() - cache.timestamp < 6 * 60 * 60 * 1000) {
                        mergeSuggestions(cache.suggestions)
                        return@launch
                    }
                } catch (_: Exception) {}
            }
            val stats = _analysis.value ?: return@launch
            val prompt = buildString {
                appendLine("基于以下日记统计数据，给出5条写作改进建议：")
                appendLine("总篇数：${stats.totalEntries}")
                appendLine("平均字数：${stats.avgWordCount.toInt()}")
                appendLine("平均句长：${stats.avgSentenceLength.toInt()}")
                appendLine("词汇丰富度：${(stats.vocabularyRichness * 100).toInt()}%")
                if (stats.topRepeatedWords.isNotEmpty()) {
                    appendLine("高频词：${stats.topRepeatedWords.take(3).joinToString("、") { "${it.first}(${it.second}次)" }}")
                }
                appendLine("写作时间模式：${stats.writingTimePattern}")
                appendLine("请输出5条建议，每条一行，以-开头。")
            }
            val request = aiRequest(
                userMessage = prompt,
                systemPrompt = "你是一个写作教练。根据用户的日记统计数据，给出具体、可操作的改进建议。用中文回答。"
            )
            val result = app.aiService.chat(request, useCache = false)
            result.onSuccess { response ->
                val list = response.content.trim().split("\n")
                    .map { it.trim().removePrefix("-").removePrefix(" ").trim() }
                    .filter { it.isNotBlank() }
                if (list.isNotEmpty()) {
                    val entry = AiSuggestionCache(list, System.currentTimeMillis())
                    prefs.edit().putString("ai_suggestions_cache", gson.toJson(entry)).apply()
                    mergeSuggestions(list)
                    refreshGrowth()
                }
            }
        }
    }

    private fun mergeSuggestions(aiList: List<String>?) {
        val aiSuggestions = aiList ?: emptyList()
        val localSuggestions = _analysis.value?.suggestions ?: emptyList()
        val merged = mutableListOf<AiSuggestion>()
        aiSuggestions.forEach { text ->
            if (merged.none { it.text == text } && merged.size < 5) {
                merged.add(AiSuggestion(text, true))
            }
        }
        localSuggestions.forEach { text ->
            if (merged.none { it.text == text } && merged.size < 5) {
                merged.add(AiSuggestion(text, false))
            }
        }
        _aiSuggestions.value = merged
        refreshGrowth()
    }

    fun refreshGrowth() {
        _growth.value = buildWritingCoachGrowth(
            analysis = _analysis.value,
            currentStats = _currentStats.value,
            dailyWordGoal = _dailyWordGoal.value,
            weeklyDayGoal = _weeklyDayGoal.value,
            todayWordCount = _todayWordCount.value,
            thisWeekWritingDays = _thisWeekWritingDays.value,
            aiEnabled = aiEnabled,
            aiAnalysisResult = _aiAnalysisResult.value,
            aiSuggestions = _aiSuggestions.value
        )
    }

    private fun getTimeRangeTimestamps(range: TimeRange): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        return when (range) {
            TimeRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to now
            }
            TimeRange.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val thisMonthStart = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                val lastMonthStart = cal.timeInMillis
                lastMonthStart to (thisMonthStart - 1)
            }
            TimeRange.THIS_QUARTER -> {
                val month = cal.get(Calendar.MONTH)
                val qStart = when (month) {
                    in 0..2 -> Calendar.JANUARY
                    in 3..5 -> Calendar.APRIL
                    in 6..8 -> Calendar.JULY
                    else -> Calendar.OCTOBER
                }
                cal.set(Calendar.MONTH, qStart)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to now
            }
        }
    }

    private fun getYearMonthString(range: TimeRange): String {
        val cal = Calendar.getInstance()
        return when (range) {
            TimeRange.THIS_MONTH -> String.format("%d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            TimeRange.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                String.format("%d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            }
            TimeRange.THIS_QUARTER -> {
                val m = cal.get(Calendar.MONTH)
                val q = m / 3 + 1
                String.format("%dQ%d", cal.get(Calendar.YEAR), q)
            }
        }
    }
}
