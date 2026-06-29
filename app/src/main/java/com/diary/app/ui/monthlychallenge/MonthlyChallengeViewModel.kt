package com.diary.app.ui.monthlychallenge

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.ChallengeDailyLog
import com.diary.app.data.MonthlyChallenge
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ChallengeTemplate(
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val targetDays: Int
)

data class BadgeRecord(
    val challengeId: Long,
    val badgeType: String,
    val title: String,
    val earnedAt: Long,
    val progress: Float
)

data class ChallengeMeta(
    val template: ChallengeTemplate?,
    val selectedAt: Long
)

data class ChallengeStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val remainingDays: Int = 0
)

class MonthlyChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val app = application as DiaryApplication
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("monthly_challenge", Context.MODE_PRIVATE)

    private val _currentChallenge = MutableStateFlow<MonthlyChallenge?>(null)
    val currentChallenge: StateFlow<MonthlyChallenge?> = _currentChallenge.asStateFlow()

    private val _dailyLogs = MutableStateFlow<List<ChallengeDailyLog>>(emptyList())
    val dailyLogs: StateFlow<List<ChallengeDailyLog>> = _dailyLogs.asStateFlow()

    private val _challengeTemplates = MutableStateFlow<List<ChallengeTemplate>?>(null)
    val challengeTemplates: StateFlow<List<ChallengeTemplate>?> = _challengeTemplates.asStateFlow()

    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _badgeRecords = MutableStateFlow<List<BadgeRecord>>(emptyList())
    val badgeRecords: StateFlow<List<BadgeRecord>> = _badgeRecords.asStateFlow()

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration.asStateFlow()

    private val _latestBadge = MutableStateFlow<BadgeRecord?>(null)
    val latestBadge: StateFlow<BadgeRecord?> = _latestBadge.asStateFlow()

    private val _consecutiveMissedDays = MutableStateFlow(0)
    val consecutiveMissedDays: StateFlow<Int> = _consecutiveMissedDays.asStateFlow()

    private val _stats = MutableStateFlow(ChallengeStats())
    val stats: StateFlow<ChallengeStats> = _stats.asStateFlow()

    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<ChallengeTemplate?>(null)
    val selectedTemplate: StateFlow<ChallengeTemplate?> = _selectedTemplate.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var logsCollectionJob: Job? = null

    init {
        loadBadgeRecords()
        loadChallenge()
        loadChallengePool()
    }

    fun loadChallenge() {
        logsCollectionJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            val year = _selectedYear.value
            val month = _selectedMonth.value
            val challenge = dao.getMonthlyChallenge(year, month)
            _currentChallenge.value = challenge
            _dailyLogs.value = emptyList()
            if (challenge != null) {
                logsCollectionJob = launch {
                    dao.getChallengeDailyLogs(challenge.id).collect { logs ->
                        _dailyLogs.value = logs
                        computeStats()
                        computeConsecutiveMissed()
                    }
                }
            } else {
                _dailyLogs.value = emptyList()
                _consecutiveMissedDays.value = 0
                _stats.value = ChallengeStats()
            }
            _isLoading.value = false
        }
    }

    fun loadChallengePool() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val year = now.year
            val month = now.monthValue
            val generatedKey = "challenges_generated_${year}_${month}"
            val poolKey = "challenge_pool_${year}_${month}"

            if (prefs.getBoolean(generatedKey, false)) {
                val json = prefs.getString(poolKey, null)
                if (json != null) {
                    val type = object : TypeToken<List<ChallengeTemplate>>() {}.type
                    _challengeTemplates.value = gson.fromJson(json, type)
                    return@launch
                }
            }

            val templates = generateWithAI()
            if (templates != null && templates.size >= 3) {
                val json = gson.toJson(templates.take(3))
                prefs.edit().putString(poolKey, json).putBoolean(generatedKey, true).apply()
                _challengeTemplates.value = templates.take(3)
            } else {
                val presets = getPresetTemplates()
                val json = gson.toJson(presets)
                prefs.edit().putString(poolKey, json).putBoolean(generatedKey, true).apply()
                _challengeTemplates.value = presets
            }
        }
    }

    private suspend fun generateWithAI(): List<ChallengeTemplate>? {
        if (!app.aiService.isAiEnabled()) return null

        return try {
            val now = LocalDate.now()
            val threeMonthsAgo = now.minusMonths(3)
            val startOfRange = threeMonthsAgo.withDayOfMonth(1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfRange = now.withDayOfMonth(now.lengthOfMonth())
                .atTime(23, 59, 59)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val entries = dao.getEntriesByDateRange(startOfRange, endOfRange)
            val count = entries.size
            val avgWords = if (entries.isNotEmpty()) entries.map { it.plainText.length }.average().toInt() else 0
            val moods = entries.mapNotNull { it.moodLevel }
            val avgMood = if (moods.isNotEmpty()) moods.average().let { String.format("%.1f", it) } else "N/A"

            val userMessage = "近3月日记数据：总条目数${count}，平均字数${avgWords}字，平均心情值${avgMood}（1-6越高越开心），当前月份${now.monthValue}月。请根据数据生成3个个性化月度挑战。"
            val systemPrompt = "你是日记应用的挑战设计师。根据用户的日记数据，生成3个个性化的月度挑战。每个挑战包含：title（标题）、description（描述）、category（分类：写作/健康/创意/效率/社交/阅读/生活）、difficulty（难度：简单/中等/困难）、targetDays（目标完成天数，15-30天）。以JSON数组格式返回，不要markdown包装。"

            val request = aiRequest(userMessage = userMessage, systemPrompt = systemPrompt, temperature = 0.8f, maxTokens = 1024)
            val result = app.aiService.chat(request)

            result.getOrNull()?.content?.let { content ->
                val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val type = object : TypeToken<List<ChallengeTemplate>>() {}.type
                val parsed: List<ChallengeTemplate> = gson.fromJson(cleaned, type)
                if (parsed.isNotEmpty()) parsed else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getPresetTemplates(): List<ChallengeTemplate> {
        return listOf(
            ChallengeTemplate("每日一张照片配文字", "用镜头捕捉日常，用文字定格瞬间。", "生活", "简单", 20),
            ChallengeTemplate("感恩日记", "每天记录三件值得感恩的小事。", "写作", "简单", 15),
            ChallengeTemplate("情绪记录", "记录每日情绪变化，觉察内心波动。", "心理", "简单", 25),
            ChallengeTemplate("晨间三件事", "每天早晨写下当天最重要的三件事。", "效率", "中等", 20),
            ChallengeTemplate("睡前反思", "睡前回顾一天，记录收获与感悟。", "写作", "中等", 25),
            ChallengeTemplate("自然漫步", "每天外出散步15分钟，记录自然见闻。", "健康", "简单", 15),
            ChallengeTemplate("每周一封信", "每周给重要的人写一封信或长消息。", "社交", "困难", 8),
            ChallengeTemplate("美食记录", "记录每日饮食，品味生活滋味。", "生活", "简单", 20),
            ChallengeTemplate("每日金句摘抄", "每天摘抄一句打动你的话。", "阅读", "简单", 30),
            ChallengeTemplate("梦境记录", "清晨记录梦境，探索潜意识。", "创意", "中等", 15),
            ChallengeTemplate("每日一画", "用涂鸦或速写记录一天的心情。", "创意", "困难", 20),
            ChallengeTemplate("人际小温暖", "每天做一件让他人感到温暖的小事。", "社交", "简单", 20),
            ChallengeTemplate("五分钟冥想日记", "每日冥想后记录内心感受。", "健康", "中等", 25),
            ChallengeTemplate("发现城市角落", "探索家附近一个没去过的地方。", "探索", "中等", 15),
            ChallengeTemplate("每日自我对话", "写下对自己的承诺与鼓励。", "心理", "困难", 30)
        )
    }

    fun selectChallenge(template: ChallengeTemplate) {
        viewModelScope.launch {
            val now = LocalDate.now()
            val challenge = MonthlyChallenge(
                title = template.title,
                description = template.description,
                year = now.year,
                month = now.monthValue,
                targetDays = template.targetDays,
                status = "active"
            )
            val insertedId = dao.insertMonthlyChallenge(challenge)
            val meta = ChallengeMeta(template, System.currentTimeMillis())
            prefs.edit().putString("challenge_meta_${insertedId}", gson.toJson(meta)).apply()
            loadChallenge()
        }
    }

    fun toggleDay(date: Long) {
        if (_selectedYear.value != LocalDate.now().year || _selectedMonth.value != LocalDate.now().monthValue) return
        viewModelScope.launch {
            val challenge = _currentChallenge.value ?: return@launch
            val existing = dao.getChallengeDailyLog(challenge.id, date)
            if (existing != null) {
                dao.updateChallengeDailyLog(existing.copy(completed = !existing.completed))
            } else {
                dao.insertChallengeDailyLog(ChallengeDailyLog(
                    challengeId = challenge.id,
                    date = date,
                    completed = true
                ))
            }
            val logs = dao.getChallengeDailyLogs(challenge.id).first()
            val completedCount = logs.count { it.completed }
            dao.updateMonthlyChallenge(challenge.copy(completedDays = completedCount))
            checkBadges(logs)
        }
    }

    fun changeMonth(delta: Int) {
        val yearMonth = YearMonth.of(_selectedYear.value, _selectedMonth.value).plusMonths(delta.toLong())
        _selectedYear.value = yearMonth.year
        _selectedMonth.value = yearMonth.monthValue
        _isLoading.value = true
        loadChallenge()
    }

    fun goToCurrentMonth() {
        val now = LocalDate.now()
        _selectedYear.value = now.year
        _selectedMonth.value = now.monthValue
        _isLoading.value = true
        loadChallenge()
        if (_challengeTemplates.value == null) {
            loadChallengePool()
        }
    }

    fun showSelectChallengeDialog(template: ChallengeTemplate) {
        _selectedTemplate.value = template
        _showConfirmDialog.value = true
    }

    fun dismissConfirmDialog() {
        _showConfirmDialog.value = false
        _selectedTemplate.value = null
    }

    fun confirmSelectChallenge() {
        _selectedTemplate.value?.let { selectChallenge(it) }
        _showConfirmDialog.value = false
        _selectedTemplate.value = null
    }

    fun dismissCelebration() {
        _showCelebration.value = false
        _latestBadge.value = null
    }

    private fun loadBadgeRecords() {
        val json = prefs.getString("badge_records", null)
        if (json != null) {
            val type = object : TypeToken<List<BadgeRecord>>() {}.type
            _badgeRecords.value = gson.fromJson(json, type)
        }
    }

    private fun saveBadgeRecords() {
        prefs.edit().putString("badge_records", gson.toJson(_badgeRecords.value)).apply()
    }

    private fun checkBadges(logs: List<ChallengeDailyLog>) {
        val challenge = _currentChallenge.value ?: return
        val completed = logs.count { it.completed }
        val progress = completed.toFloat() / challenge.targetDays.toFloat()

        val existingTypes = _badgeRecords.value
            .filter { it.challengeId == challenge.id }
            .map { it.badgeType }
            .toSet()

        val badgeConfigs = listOf(
            Triple("bronze", 0.6f, "铜牌挑战者"),
            Triple("silver", 0.8f, "银牌挑战者"),
            Triple("gold", 1.0f, "金牌挑战者")
        )

        for ((type, threshold, title) in badgeConfigs) {
            if (progress >= threshold && type !in existingTypes) {
                val badge = BadgeRecord(
                    challengeId = challenge.id,
                    badgeType = type,
                    title = title,
                    earnedAt = System.currentTimeMillis(),
                    progress = progress
                )
                val updated = _badgeRecords.value.toMutableList().apply { add(badge) }
                _badgeRecords.value = updated
                saveBadgeRecords()
                _latestBadge.value = badge
                _showCelebration.value = true
            }
        }
    }

    private fun computeStats() {
        val logs = _dailyLogs.value
        val challenge = _currentChallenge.value ?: return

        val completedDates = logs.filter { it.completed }
            .map { java.time.Instant.ofEpochMilli(it.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            .sorted()

        val now = LocalDate.now()
        var currentStreak = 0
        val today = if (_selectedYear.value == now.year && _selectedMonth.value == now.monthValue) now
                    else LocalDate.of(_selectedYear.value, _selectedMonth.value, 1).withDayOfMonth(
                        _selectedMonth.value.let { m ->
                            YearMonth.of(_selectedYear.value, m).lengthOfMonth()
                        }
                    )
        val refDay = today
        for (i in 0 until refDay.lengthOfMonth()) {
            val date = refDay.minusDays(i.toLong())
            if (date.monthValue != refDay.monthValue || date.year != refDay.year) break
            if (completedDates.any { it == date }) {
                currentStreak++
            } else {
                break
            }
        }

        var longestStreak = 0
        var tempStreak = 0
        val daysInMonth = YearMonth.of(_selectedYear.value, _selectedMonth.value).lengthOfMonth()
        val firstDay = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1)
        for (day in 1..daysInMonth) {
            val date = firstDay.withDayOfMonth(day)
            if (completedDates.any { it == date }) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 0
            }
        }

        val remainingDays = maxOf(0, challenge.targetDays - challenge.completedDays)

        _stats.value = ChallengeStats(currentStreak, longestStreak, remainingDays)
    }

    private fun computeConsecutiveMissed() {
        val logs = _dailyLogs.value
        if (_currentChallenge.value == null) {
            _consecutiveMissedDays.value = 0
            return
        }

        val now = LocalDate.now()
        var count = 0
        for (i in 1..30) {
            val date = now.minusDays(i.toLong())
            if (date.monthValue != now.monthValue || date.year != now.year) break
            val hasCompleted = logs.any { log ->
                log.completed && java.time.Instant.ofEpochMilli(log.date)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
            }
            if (hasCompleted) break
            count++
        }
        _consecutiveMissedDays.value = count
    }
}
