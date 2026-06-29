package com.diary.app.ui.personalyearbook

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.data.DiaryEntry
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class PersonalYearbookViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val analyzer = YearbookAiAnalyzer(aiService)
    private val gson = Gson()
    private val cachePrefs = application.getSharedPreferences("yearbook_cache", Context.MODE_PRIVATE)
    private val zone = ZoneId.systemDefault()

    private val _yearbook = MutableStateFlow<YearbookData?>(null)
    val yearbook: StateFlow<YearbookData?> = _yearbook

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult

    private val _aiAnalysisPhase = MutableStateFlow("")
    val aiAnalysisPhase: StateFlow<String> = _aiAnalysisPhase

    private val _showSkeleton = MutableStateFlow(false)
    val showSkeleton: StateFlow<Boolean> = _showSkeleton

    private val _timelineEvents = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<TimelineEvent>> = _timelineEvents

    fun generate(year: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            _showSkeleton.value = false
            _aiAnalysisPhase.value = ""

            val entries = dao.getAllEntriesOnce()
            val yearEntries = entries.filter {
                val date = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
                date.year == year
            }

            if (yearEntries.isEmpty()) {
                _isGenerating.value = false
                return@launch
            }

            val stats = computeStats(year, yearEntries)
            _timelineEvents.value = extractTimelineEvents(yearEntries)

            val cacheKey = "yearbook_ai_$year"
            val cacheJson = cachePrefs.getString(cacheKey, null)

            if (cacheJson != null) {
                try {
                    val cached = gson.fromJson(cacheJson, AiCacheData::class.java)
                    val newestTime = yearEntries.maxOf { it.createdAt }
                    if (cached.lastEntryTime >= newestTime) {
                        _yearbook.value = YearbookData(
                            year = year, arcs = cached.arcs,
                            monthHighlights = cached.monthHighlights,
                            metaphor = cached.metaphor,
                            metaphorEvolution = cached.metaphorEvolution,
                            topPhotos = cached.topPhotos,
                            stats = stats
                        )
                        _isGenerating.value = false
                        return@launch
                    }
                } catch (_: Exception) { }
            }

            if (aiService.isAiEnabled()) {
                _showSkeleton.value = true

                _aiAnalysisPhase.value = "正在分析叙事脉络..."
                val arcs = withContext(Dispatchers.IO) { analyzer.extractNarrativeArcs(yearEntries) }

                _aiAnalysisPhase.value = "正在选取每月亮点..."
                val highlights = withContext(Dispatchers.IO) { analyzer.selectMonthHighlights(yearEntries, year) }

                _aiAnalysisPhase.value = "正在生成年度隐喻..."
                val (metaphor, metaphorEvo) = withContext(Dispatchers.IO) { analyzer.generateMetaphor(yearEntries, arcs) }

                _aiAnalysisPhase.value = "正在挑选精选照片..."
                val topPhotos = withContext(Dispatchers.IO) { analyzer.curatePhotos(yearEntries, dao) }

                val evolution = metaphorEvo.ifEmpty {
                    listOf(
                        MetaphorPhase("年初", "崭新起点"),
                        MetaphorPhase("年中", "起伏前行"),
                        MetaphorPhase("年末", "沉淀收获")
                    )
                }

                val aiData = AiCacheData(
                    arcs = arcs, monthHighlights = highlights,
                    metaphor = metaphor, metaphorEvolution = evolution,
                    topPhotos = topPhotos, lastEntryTime = yearEntries.maxOf { it.createdAt }
                )
                cachePrefs.edit().putString(cacheKey, gson.toJson(aiData)).apply()

                _yearbook.value = YearbookData(
                    year = year, arcs = arcs, monthHighlights = highlights,
                    metaphor = metaphor, metaphorEvolution = evolution,
                    topPhotos = topPhotos, stats = stats
                )
            } else {
                _yearbook.value = YearbookData(
                    year = year, arcs = emptyList(), monthHighlights = emptyList(),
                    metaphor = "记录本身就是最好的总结", metaphorEvolution = emptyList(),
                    topPhotos = emptyList(), stats = stats
                )
            }

            _showSkeleton.value = false
            _isGenerating.value = false
        }
    }

    private fun computeStats(year: Int, yearEntries: List<DiaryEntry>): YearbookStats {
        val totalEntries = yearEntries.size
        val totalWords = yearEntries.sumOf { it.plainText.length }

        val monthlyDistribution = MutableList(12) { 0 }
        yearEntries.forEach {
            val month = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().monthValue
            monthlyDistribution[month - 1]++
        }

        val moodDistribution = yearEntries.mapNotNull { it.moodLevel }.groupingBy { it }.eachCount()
        val topMood = moodDistribution.maxByOrNull { it.value }?.key

        val monthNames = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
        val bestMonthIndex = monthlyDistribution.indices.maxByOrNull { monthlyDistribution[it] } ?: 0
        val bestMonth = monthNames[bestMonthIndex]

        val daysOfYear = yearEntries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().dayOfYear
        }.distinct().sorted()

        var longestStreak = 0
        var currentStreak = 0
        var prev = -2
        for (day in daysOfYear) {
            if (day == prev + 1) {
                currentStreak++
            } else {
                currentStreak = 1
            }
            longestStreak = maxOf(longestStreak, currentStreak)
            prev = day
        }

        return YearbookStats(
            totalEntries = totalEntries, totalWords = totalWords,
            topMood = topMood, bestMonth = bestMonth,
            longestStreak = longestStreak,
            monthlyDistribution = monthlyDistribution,
            moodDistribution = moodDistribution
        )
    }

    private fun extractTimelineEvents(entries: List<DiaryEntry>): List<TimelineEvent> {
        val eventKeywords = mapOf(
            "旅行" to "trip", "旅游" to "trip", "出差" to "trip",
            "升职" to "achievement", "加薪" to "achievement", "获奖" to "achievement", "完成" to "achievement",
            "搬家" to "life_change", "毕业" to "life_change", "入职" to "life_change", "离职" to "life_change",
            "生日" to "celebration", "聚会" to "celebration", "结婚" to "celebration",
            "生病" to "health", "医院" to "health",
            "分手" to "relationship", "吵架" to "relationship", "和好" to "relationship"
        )
        return entries.mapNotNull { entry ->
            val match = eventKeywords.entries.firstOrNull { (kw, _) ->
                entry.plainText.contains(kw) || entry.title.contains(kw)
            }
            match?.let { (_, type) ->
                val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().toString()
                TimelineEvent(entry.id, date, entry.title.ifEmpty { entry.plainText.take(30) }, type)
            }
        }.take(20)
    }

    fun exportPDF() {
        val data = _yearbook.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportResult.value = null
            withContext(Dispatchers.IO) {
                PdfExporter.export(getApplication<DiaryApplication>(), data) { success, message ->
                    _exportResult.value = if (success) message else "导出失败: $message"
                    _isExporting.value = false
                }
            }
        }
    }

    fun sharePDF(context: Context) {
        val filePath = _exportResult.value ?: return
        val file = java.io.File(filePath)
        if (!file.exists()) return

        val uri = PdfExporter.getShareUri(context, filePath)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "分享年鉴 PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    private data class AiCacheData(
        val arcs: List<NarrativeArc>,
        val monthHighlights: List<MonthHighlight>,
        val metaphor: String,
        val metaphorEvolution: List<MetaphorPhase>,
        val topPhotos: List<String>,
        val lastEntryTime: Long
    )
}
