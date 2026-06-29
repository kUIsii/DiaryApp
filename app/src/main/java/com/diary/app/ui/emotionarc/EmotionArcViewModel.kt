package com.diary.app.ui.emotionarc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.data.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class PeriodType { WEEK, MONTH, QUARTER }

data class DailyEmotion(
    val dateTimestamp: Long,
    val label: String,
    val emotion: Float,
    val entryCount: Int,
    val entryIds: List<Long>
)

data class EmotionArcUiState(
    val isLoading: Boolean = true,
    val isAiAnalyzing: Boolean = false,
    val error: String? = null,
    val selectedPeriod: PeriodType = PeriodType.WEEK,
    val dailyEmotions: List<DailyEmotion> = emptyList(),
    val title: String = "情绪弧线",
    val analysis: EmotionAnalysis? = null,
    val isAiEnabled: Boolean = false,
    val comparisonText: String? = null
)

class EmotionArcViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val analyzer = EmotionArcAnalyzer(aiService)

    private val _uiState = MutableStateFlow(EmotionArcUiState())
    val uiState: StateFlow<EmotionArcUiState> = _uiState.asStateFlow()

    private var allEntries: List<DiaryEntry> = emptyList()
    private var currentDiaryId: Long? = null
    private var cachedAnalysis: MutableMap<String, EmotionAnalysis> = mutableMapOf()

    fun loadData(diaryId: Long?) {
        currentDiaryId = diaryId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (diaryId != null && diaryId > 0) {
                val entry = dao.getEntryById(diaryId)
                if (entry == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "日记不存在")
                    return@launch
                }
                allEntries = dao.getEntriesByDateRange(entry.createdAt - 3L * 86400000L, entry.createdAt + 4L * 86400000L)
                if (allEntries.isEmpty()) allEntries = listOf(entry)
            } else {
                allEntries = dao.getAllPreviewsOnce().mapNotNull { dao.getEntryByIdSafe(it.id) }
            }
            if (allEntries.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "暂无日记数据")
                return@launch
            }

            allEntries = allEntries.sortedBy { it.createdAt }
            _uiState.value = _uiState.value.copy(
                title = if (diaryId != null && diaryId > 0) {
                    allEntries.find { it.id == diaryId }?.title?.ifBlank { "日记情绪" } ?: "情绪弧线"
                } else "情绪弧线",
                isAiEnabled = aiService.isAiEnabled()
            )
            updatePeriod(_uiState.value.selectedPeriod)
        }
    }

    fun updatePeriod(period: PeriodType) {
        val state = _uiState.value
        if (allEntries.isEmpty()) return

        _uiState.value = state.copy(selectedPeriod = period)

        val now = LocalDate.now(ZoneId.systemDefault())
        val periodStart = when (period) {
            PeriodType.WEEK -> now.minusDays(6)
            PeriodType.MONTH -> now.minusDays(29)
            PeriodType.QUARTER -> now.minusDays(89)
        }
        val periodStartMs = periodStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val periodEndMs = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val filtered = allEntries.filter { it.createdAt in periodStartMs until periodEndMs }

        val dailyEmotions = buildDailyEmotions(filtered)
        _uiState.value = _uiState.value.copy(dailyEmotions = dailyEmotions, isLoading = false)

        val cacheKey = "${periodStartMs}_${periodEndMs}"
        if (cachedAnalysis.containsKey(cacheKey)) {
            _uiState.value = _uiState.value.copy(analysis = cachedAnalysis[cacheKey], isAiAnalyzing = false)
        } else {
            triggerAnalysis(filtered, periodStartMs, periodEndMs)
        }
    }

    private fun buildDailyEmotions(entries: List<DiaryEntry>): List<DailyEmotion> {
        val grouped = entries.groupBy { e ->
            val ld = java.time.Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            ld
        }.toSortedMap()
        return grouped.map { (date, dayEntries) ->
            val moods = dayEntries.mapNotNull { it.moodLevel }
            val avg = if (moods.isNotEmpty()) moods.average().let { ((it - 1) / 5f).toFloat() } else 0.5f
            DailyEmotion(
                dateTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                label = "${date.monthValue}月${date.dayOfMonth}日",
                emotion = avg,
                entryCount = dayEntries.size,
                entryIds = dayEntries.map { it.id }
            )
        }
    }

    private fun triggerAnalysis(entries: List<DiaryEntry>, startMs: Long, endMs: Long) {
        _uiState.value = _uiState.value.copy(isAiAnalyzing = true)

        val cacheKey = "${startMs}_${endMs}"
        val cached = cachedAnalysis[cacheKey]
        if (cached != null) {
            _uiState.value = _uiState.value.copy(analysis = cached, isAiAnalyzing = false)
            return
        }

        viewModelScope.launch {
            val result = if (aiService.isAiEnabled()) {
                analyzer.analyze(entries, startMs, endMs)
            } else null

            val final = result ?: analyzer.analyzeLocal(entries, startMs, endMs)
            cachedAnalysis[cacheKey] = final
            _uiState.value = _uiState.value.copy(analysis = final, isAiAnalyzing = false)
        }
    }

    fun comparePeriods(daysAgo1: Int, daysAgo2: Int) {
        if (allEntries.isEmpty() || !aiService.isAiEnabled()) return

        _uiState.value = _uiState.value.copy(isAiAnalyzing = true)

        viewModelScope.launch {
            val now = LocalDate.now(ZoneId.systemDefault())
            val end1 = now.minusDays(daysAgo1.toLong()).plusDays(1)
            val start1 = end1.minusDays(29)
            val end2 = now.minusDays(daysAgo2.toLong()).plusDays(1)
            val start2 = end2.minusDays(29)

            val entries1 = allEntries.filter { e ->
                e.createdAt >= start1.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() &&
                e.createdAt < end1.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val entries2 = allEntries.filter { e ->
                e.createdAt >= start2.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() &&
                e.createdAt < end2.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            val label1 = "${start1.monthValue}月${start1.dayOfMonth}日-${end1.minusDays(1).monthValue}月${end1.minusDays(1).dayOfMonth}日"
            val label2 = "${start2.monthValue}月${start2.dayOfMonth}日-${end2.minusDays(1).monthValue}月${end2.minusDays(1).dayOfMonth}日"

            val comparison = analyzer.comparePeriods(label1, entries1, label2, entries2)
            _uiState.value = _uiState.value.copy(comparisonText = comparison, isAiAnalyzing = false)
        }
    }
}
