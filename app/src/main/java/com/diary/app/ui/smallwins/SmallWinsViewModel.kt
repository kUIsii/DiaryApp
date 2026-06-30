package com.diary.app.ui.smallwins

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.data.SmallWin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class SmallWinsAnalytics(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalWins: Int = 0,
    val averagePerDay: Float = 0f,
    val averagePerActiveDay: Float = 0f,
    val daysActive: Int = 0,
    val thisWeekCount: Int = 0,
    val lastWeekCount: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap()
)

data class WritingBridgeSeed(
    val title: String,
    val prompt: String,
    val summary: String
)

class SmallWinsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as DiaryApplication).database.diaryDao()
    private val app = application as DiaryApplication

    val allSmallWins: StateFlow<List<SmallWin>> = dao.getAllSmallWins()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val todayDate: Long get() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

    private val _refreshTicker = MutableStateFlow(0L)

    val todaySmallWins: StateFlow<List<SmallWin>> = _refreshTicker.flatMapLatest {
        dao.getSmallWinsByDate(todayDate)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayDate)
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val historyWins: StateFlow<List<SmallWin>> = _selectedDate.flatMapLatest { date ->
        dao.getSmallWinsByDate(date)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val analytics: StateFlow<SmallWinsAnalytics> = allSmallWins.map { wins ->
        computeAnalytics(wins)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SmallWinsAnalytics())

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _writingBridgeSeed = MutableStateFlow<WritingBridgeSeed?>(null)
    val writingBridgeSeed: StateFlow<WritingBridgeSeed?> = _writingBridgeSeed.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val isAiEnabled: Boolean
        get() = app.aiService.isAiEnabled()

    init {
        _selectedDate.value = todayDate
    }

    fun refreshToday() {
        _refreshTicker.value = System.currentTimeMillis()
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun navigateDay(direction: Int) {
        val current = millisToLocalDate(_selectedDate.value)
        val newDate = current.plusDays(direction.toLong())
        _selectedDate.value = newDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
    }

    fun addSmallWin(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val smallWin = SmallWin(
                content = content.trim(),
                recordDate = todayDate,
                createdAt = System.currentTimeMillis()
            )
            dao.insertSmallWin(smallWin)
            _inputText.value = ""
            _aiSummary.value = null
        }
    }

    fun deleteSmallWin(id: Long) {
        viewModelScope.launch {
            dao.deleteSmallWin(id)
        }
    }

    fun editSmallWin(id: Long, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            val win = allSmallWins.value.find { it.id == id } ?: return@launch
            dao.deleteSmallWin(id)
            dao.insertSmallWin(win.copy(content = newContent.trim(), createdAt = System.currentTimeMillis()))
        }
    }

    fun generateSummary() {
        if (!isAiEnabled || _isAiLoading.value) return
        _isAiLoading.value = true
        viewModelScope.launch {
            val wins = allSmallWins.value
            val prompt = buildPrompt(wins)
            val request = AiRequest(
                messages = listOf(
                    AiMessage("system", "你是一个积极心理学教练，擅长帮助用户发现生活中的小确幸。请用温暖鼓励的语气回应用户。"),
                    AiMessage("user", prompt)
                ),
                temperature = 0.7f,
                maxTokens = 400
            )
            val result = app.aiService.chat(request)
            result.onSuccess { response ->
                _aiSummary.value = response.content
            }.onFailure {
                _aiSummary.value = "暂时无法生成总结，请稍后再试。"
            }
            _isAiLoading.value = false
        }
    }

    fun resetSummary() {
        _aiSummary.value = null
    }

    fun buildWritingBridgeSeed(): WritingBridgeSeed? {
        val wins = todaySmallWins.value.ifEmpty { historyWins.value }
        if (wins.isEmpty()) return null
        val latest = wins.sortedByDescending { it.createdAt }.first()
        val dateText = millisToLocalDate(latest.recordDate).format(DateTimeFormatter.ofPattern("M月d日"))
        val summary = buildString {
            appendLine("今天的小确幸可以转成一段写作素材：")
            wins.take(3).forEach { appendLine("- ${it.content}") }
        }.trim()
        val prompt = buildString {
            appendLine("请把下面的小确幸整理成一条可以直接写日记的提示，要求具体、温暖、能落笔。")
            appendLine("日期：$dateText")
            appendLine("内容：")
            wins.take(5).forEach { appendLine("- ${it.content}") }
            appendLine("请给出：")
            appendLine("1. 一句可直接开写的起笔")
            appendLine("2. 2-3 个可追问的问题")
            appendLine("3. 一个适合写成段落的角度")
        }
        return WritingBridgeSeed(
            title = "把小确幸写成日记",
            prompt = prompt,
            summary = summary
        )
    }

    fun prepareWritingBridge() {
        _writingBridgeSeed.value = buildWritingBridgeSeed()
    }

    fun clearWritingBridgeSeed() {
        _writingBridgeSeed.value = null
    }

    fun getShareText(): String {
        val a = analytics.value
        val todayWins = todaySmallWins.value
        val recentWins = allSmallWins.value.sortedByDescending { it.recordDate }.take(10)
        val sb = StringBuilder()
        sb.appendLine("小确幸摘要")
        sb.appendLine("==========")
        sb.appendLine("今日记录: ${todayWins.size} 件")
        todayWins.take(5).forEach { sb.appendLine("  - ${it.content}") }
        if (todayWins.size > 5) sb.appendLine("  - 等共 ${todayWins.size} 件")
        sb.appendLine()
        sb.appendLine("总计: ${a.totalWins} 件")
        sb.appendLine("当前连续记录: ${a.currentStreak} 天")
        sb.appendLine("最长连续记录: ${a.longestStreak} 天")
        sb.appendLine("活跃日均: ${"%.1f".format(a.averagePerActiveDay)} 件 (活跃 ${a.daysActive} 天)")
        sb.appendLine("全部日均: ${"%.1f".format(a.averagePerDay)} 件")
        sb.appendLine("本周: ${a.thisWeekCount} 件 (上周: ${a.lastWeekCount} 件)")
        if (recentWins.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("可转写作素材:")
            recentWins.take(3).forEach { win -> sb.appendLine("  - ${win.content}") }
        }
        return sb.toString()
    }

    private fun buildPrompt(wins: List<SmallWin>): String {
        val sb = StringBuilder()
        sb.appendLine("以下是我的小确幸记录，请帮我生成一份温暖的周/月度总结：")
        sb.appendLine()
        val todayWins = wins.filter { it.recordDate == todayDate }
        if (todayWins.isNotEmpty()) {
            sb.appendLine("【今日小确幸】")
            todayWins.forEach { sb.appendLine("- ${it.content}") }
            sb.appendLine()
        }
        sb.appendLine("【近期小确幸】")
        wins.sortedByDescending { it.recordDate }.take(20).forEach { sb.appendLine("- ${it.content}") }
        return sb.toString()
    }

    private fun computeAnalytics(wins: List<SmallWin>): SmallWinsAnalytics {
        if (wins.isEmpty()) return SmallWinsAnalytics()

        val totalWins = wins.size
        val groupedByDate = wins.groupBy { it.recordDate }
        val sortedDates = groupedByDate.keys.sortedDescending()
        val daysActive = sortedDates.size

        val currentStreak = computeCurrentStreak(sortedDates)
        val longestStreak = computeLongestStreak(sortedDates)

        val firstWinDate = wins.minOf { millisToLocalDate(it.recordDate) }
        val totalDaysSinceFirstWin = firstWinDate.until(LocalDate.now(), ChronoUnit.DAYS) + 1
        val denominator = minOf(totalDaysSinceFirstWin, 30L).coerceAtLeast(1)
        val averagePerDay = totalWins.toFloat() / denominator
        val averagePerActiveDay = if (daysActive > 0) totalWins.toFloat() / daysActive else 0f

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeekStart = thisWeekMonday.atStartOfDay(zone).toEpochSecond() * 1000
        val nextWeekStart = thisWeekMonday.plusDays(7).atStartOfDay(zone).toEpochSecond() * 1000
        val lastWeekStart = thisWeekMonday.minusDays(7).atStartOfDay(zone).toEpochSecond() * 1000

        val thisWeekCount = wins.count {
            it.recordDate >= thisWeekStart && it.recordDate < nextWeekStart
        }
        val lastWeekCount = wins.count {
            it.recordDate >= lastWeekStart && it.recordDate < thisWeekStart
        }

        val categoryDistribution = mutableMapOf<String, Int>()
        wins.forEach { win ->
            parseCategories(win.content).forEach { cat ->
                categoryDistribution[cat] = categoryDistribution.getOrDefault(cat, 0) + 1
            }
        }

        return SmallWinsAnalytics(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalWins = totalWins,
            averagePerDay = averagePerDay,
            averagePerActiveDay = averagePerActiveDay,
            daysActive = daysActive,
            thisWeekCount = thisWeekCount,
            lastWeekCount = lastWeekCount,
            categoryDistribution = categoryDistribution
        )
    }

    private fun computeCurrentStreak(sortedDates: List<Long>): Int {
        val today = LocalDate.now()
        if (sortedDates.isEmpty()) return 0
        var streak = 0
        var expected = today
        for (millis in sortedDates) {
            val date = millisToLocalDate(millis)
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date < expected) {
                break
            }
        }
        return streak
    }

    private fun computeLongestStreak(sortedDates: List<Long>): Int {
        if (sortedDates.size <= 1) return sortedDates.size
        val dates = sortedDates.map { millisToLocalDate(it) }
        var maxStreak = 1
        var currentStreak = 1
        for (i in 0 until dates.size - 1) {
            val diff = dates[i].toEpochDay() - dates[i + 1].toEpochDay()
            if (diff == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else if (diff > 0) {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    private fun millisToLocalDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        val CATEGORIES = listOf("all", "health", "work", "family", "growth", "fun", "food", "other")
        private val CATEGORY_TAGS = listOf("health", "work", "family", "growth", "fun", "food")

        fun parseCategories(content: String): List<String> {
            val regex = Regex("#([a-zA-Z\\u4e00-\\u9fa9]+)")
            val found = regex.findAll(content)
                .map { it.groupValues[1] }
                .filter { it in CATEGORY_TAGS }
                .toList()
            return if (found.isEmpty()) listOf("other") else found
        }

        fun matchesCategory(content: String, category: String): Boolean {
            if (category == "all") return true
            val regex = Regex("#([a-zA-Z\\u4e00-\\u9fa9]+)")
            val tags = regex.findAll(content).map { it.groupValues[1] }.toSet()
            if (category == "other") return tags.none { it in CATEGORY_TAGS }
            return category in tags
        }

        fun categoryDisplayName(cat: String): String {
            return when (cat) {
                "all" -> "全部"
                "health" -> "健康"
                "work" -> "工作"
                "family" -> "家庭"
                "growth" -> "成长"
                "fun" -> "娱乐"
                "food" -> "美食"
                "other" -> "其他"
                else -> cat
            }
        }
    }
}
