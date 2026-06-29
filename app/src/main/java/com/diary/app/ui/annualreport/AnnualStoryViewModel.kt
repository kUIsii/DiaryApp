package com.diary.app.ui.annualreport

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class StoryLoadingState(
    val chaptersLoading: Boolean = true,
    val chaptersProgress: Int = 0,
    val chaptersTotal: Int = 0,
    val patternsLoading: Boolean = true,
    val crossYearLoading: Boolean = true,
    val blindSpotLoading: Boolean = true,
    val error: String? = null
)

class AnnualStoryViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    private val app = application as DiaryApplication
    private val aiService = app.aiService
    private val dao = app.database.diaryDao()
    private val annotationPrefs = application.getSharedPreferences("story_annotations", Context.MODE_PRIVATE)

    private val _story = MutableStateFlow(AnnualStory.empty(LocalDate.now().year))
    val story: StateFlow<AnnualStory> = _story.asStateFlow()

    private val _loadingState = MutableStateFlow(StoryLoadingState())
    val loadingState: StateFlow<StoryLoadingState> = _loadingState.asStateFlow()

    private val _selectedEntryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedEntryIds: StateFlow<Set<Long>> = _selectedEntryIds.asStateFlow()

    private val _expandedChapterIndex = MutableStateFlow<Int?>(null)
    val expandedChapterIndex: StateFlow<Int?> = _expandedChapterIndex.asStateFlow()

    private var report: AnnualReport? = null
    private var priorYearReport: AnnualReport? = null

    val aiEnabled: Boolean get() = aiService.isAiEnabled()

    fun loadStory(year: Int, currentReport: AnnualReport) {
        report = currentReport
        viewModelScope.launch {
            _loadingState.value = StoryLoadingState()
            _story.value = AnnualStory.empty(year)

            val contextBundle = buildAiContext(year, currentReport)

            val chapterCount = 5
            _loadingState.value = _loadingState.value.copy(
                chaptersTotal = chapterCount
            )

            val chapterDeferred = (0 until chapterCount).map { index ->
                async { generateChapter(index, contextBundle) }
            }
            val patternsDeferred = async { generatePatterns(contextBundle) }
            val crossYearDeferred = async { generateCrossYearInsights(year, contextBundle) }
            val blindSpotDeferred = async { detectBlindSpots(year, contextBundle) }

            val chapters = chapterDeferred.mapNotNull { it.await() }
            val patterns = patternsDeferred.await()
            val crossYear = crossYearDeferred.await()
            val blindSpots = blindSpotDeferred.await()

            val savedAnnotations = loadAnnotations()
            _story.value = AnnualStory(
                year = year,
                chapters = chapters,
                patterns = patterns,
                crossYearInsights = crossYear,
                userAnnotations = savedAnnotations,
                blindSpotNotes = blindSpots
            )

            _loadingState.value = StoryLoadingState(
                chaptersLoading = false,
                patternsLoading = false,
                crossYearLoading = false,
                blindSpotLoading = false
            )
        }
    }

    private suspend fun buildAiContext(year: Int, report: AnnualReport): AiContextBundle {
        val zone = ZoneId.systemDefault()
        val allPreviews = dao.getAllPreviewsOnce()
        val yearEntries = allPreviews
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().year == year }
            .sortedBy { it.createdAt }

        val sampleEntries = yearEntries.takeLast(10).map { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            val fullEntry = dao.getEntryByIdSafe(entry.id)
            SampleEntry(
                id = entry.id,
                title = entry.title,
                date = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                plainText = entry.plainText.take(500),
                moodLevel = entry.moodLevel,
                weather = entry.weather
            )
        }

        val allWords = yearEntries.flatMap { it.plainText.split(Regex("[\\s，。！？、；：,。!?;:]+")) }
            .filter { it.length in 2..6 }
            .groupBy { it }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }.take(20).map { it.key }

        val weathers = yearEntries.mapNotNull { it.weather }.distinct()

        val priorYearEntries = allPreviews
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().year == year - 1 }
        val priorYearExists = priorYearEntries.isNotEmpty()

        return AiContextBundle(
            year = year,
            totalEntries = report.totalEntries,
            totalWords = report.totalWords,
            monthlyMood = report.monthlyMood,
            monthlyCount = report.monthlyCount,
            topTags = report.topTags.map { it.name },
            topWords = allWords,
            sampleEntries = sampleEntries,
            longestSilenceDays = report.longestSilenceDays,
            silencePeriod = "${report.silenceStart} - ${report.silenceEnd}",
            nightEntryRatio = if (report.totalEntries > 0) report.nightEntries.toFloat() / report.totalEntries else 0f,
            mostActiveTime = report.mostActiveTime,
            weatherDistribution = weathers,
            priorYearExists = priorYearExists,
            priorYearTotalEntries = priorYearEntries.size,
            priorYearTotalWords = priorYearEntries.sumOf { it.plainText.length },
            priorYearTopTags = emptyList()
        )
    }

    private suspend fun generateChapter(index: Int, ctx: AiContextBundle): StoryChapter? {
        if (!aiEnabled) return null
        val chapterPrompts = listOf(
            "请分析以下日记数据，写出用户这一年的【情绪变化】故事章节，模仿用户的写作风格，2-3段。包含具体的日期和例子。",
            "请分析以下日记数据，写出用户这一年的【重要事件与转折点】故事章节，模仿用户的写作风格，2-3段。",
            "请分析以下日记数据，写出用户这一年的【人际与关系】故事章节，提到具体的人名和事件，2-3段。",
            "请分析以下日记数据，写出用户这一年的【成长与变化】故事章节，反思前后的对比，2-3段。",
            "请分析以下日记数据，写出用户这一年的【日常与习惯】故事章节，从细节中发现生活的美好，2-3段。"
        )
        val sampleText = ctx.sampleEntries.take(3).joinToString("\n\n") { entry ->
            "[${entry.date}] ${entry.title}\n${entry.plainText.take(300)}"
        }
        val parts = listOf(
            "全年共 ${ctx.totalEntries} 篇日记，${ctx.totalWords} 字",
            "每月情绪值：${ctx.monthlyMood.mapIndexed { i, v -> "${i + 1}月:${v?.let { String.format("%.1f", it) } ?: "无"}" }.joinToString(", ")}",
            "每月篇数：${ctx.monthlyCount.joinToString(", ")}",
            "最常用标签：${ctx.topTags.joinToString(", ")}",
            "最常用词：${ctx.topWords.take(10).joinToString(", ")}",
            "最常见天气：${ctx.weatherDistribution.joinToString(", ")}",
            "深夜写作占比：${String.format("%.0f", ctx.nightEntryRatio * 100)}%",
            "最大沉默期：${ctx.longestSilenceDays}天"
        )
        val dataSummary = parts.joinToString("\n")
        val prompt = """
${chapterPrompts[index]}

## 写作数据
$dataSummary

## 写作风格样本
$sampleText

## 输出格式
{
  "title": "章节标题（中文，4-8字）",
  "summary": "完整章节内容（2-3段中文，每段50-150字）",
  "entryIds": [相关日记的id列表],
  "emotionSparkline": [章节情绪曲线，5个0-1浮点数],
  "style": "描述用户写作风格的短语"
}

只返回JSON，不要其他文字。
""".trimIndent()

        return try {
            val request = aiRequest(
                userMessage = prompt,
                systemPrompt = "你是日记分析专家，擅长从日记中发现故事。用中文回复。",
                temperature = 0.7f,
                maxTokens = 1024
            )
            val result = aiService.chat(request, useCache = false)
            if (result.isFailure) return null
            val content = result.getOrNull()?.content ?: return null
            val json = extractJson(content)
            if (json == null) {
                StoryChapter(
                    title = "第${index + 1}章",
                    summary = content.take(500),
                    entryIds = emptyList(),
                    emotionSparkline = listOf(0.5f),
                    style = "自然"
                )
            } else {
                val chapter = gson.fromJson(json, StoryChapter::class.java)
                _loadingState.value = _loadingState.value.copy(
                    chaptersProgress = _loadingState.value.chaptersProgress + 1
                )
                chapter
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun generatePatterns(ctx: AiContextBundle): List<DiscoveredPattern> {
        if (!aiEnabled) return emptyList()
        val prompt = """
分析以下日记年度数据，发现3个用户可能自己没注意到的有趣模式。每个模式要具体、有洞察力。

数据：
- 全年共 ${ctx.totalEntries} 篇日记，${ctx.totalWords} 字
- 每月情绪值：${ctx.monthlyMood.mapIndexed { i, v -> "${i + 1}月:${v?.let { String.format("%.1f", it) } ?: "无"}" }.joinToString(", ")}
- 每月篇数：${ctx.monthlyCount.joinToString(", ")}
- 最常用标签：${ctx.topTags.joinToString(", ")}
- 最常用词：${ctx.topWords.take(15).joinToString(", ")}

样本日记：
${ctx.sampleEntries.take(5).joinToString("\n\n") { "[${it.date}] ${it.title}\n${it.plainText.take(200)}" }}

输出格式，只返回JSON数组：
[
  {
    "id": "pattern_1",
    "description": "模式描述（中文，具体指出数据），如'你每次换工作前，都会更多地写到"自由"这个词'",
    "type": "word_frequency|person_mention|topic|emotion",
    "relatedEntryIds": [],
    "significance": 0.8
  }
]
""".trimIndent()
        return try {
            val request = aiRequest(
                userMessage = prompt,
                systemPrompt = "你是数据分析师，从日记数据中发现有趣的模式。用中文回复。",
                temperature = 0.5f,
                maxTokens = 1024
            )
            val result = aiService.chat(request, useCache = false)
            if (result.isFailure) return emptyList()
            val content = result.getOrNull()?.content ?: return emptyList()
            val json = extractJson(content)
            if (json == null) return emptyList()
            val type = object : TypeToken<List<DiscoveredPattern>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun generateCrossYearInsights(year: Int, ctx: AiContextBundle): List<CrossYearInsight>? {
        if (!ctx.priorYearExists) return null
        val prompt = """
比较 ${year - 1} 年和 ${year} 年的日记数据，生成3个跨年洞察。

${year - 1}年数据：
- 总篇数：${ctx.priorYearTotalEntries}
- 总字数：${ctx.priorYearTotalWords}

${year}年数据：
- 总篇数：${ctx.totalEntries}
- 总字数：${ctx.totalWords}

输出格式，只返回JSON数组：
[
  {
    "dimension": "emotion|topic|volume|social",
    "currentYearValue": "今年值描述",
    "priorYearValue": "去年值描述",
    "changePercent": 15.5,
    "description": "描述变化"
  }
]
""".trimIndent()
        return try {
            val request = aiRequest(
                userMessage = prompt,
                systemPrompt = "你是数据对比分析师。用中文回复。",
                temperature = 0.5f,
                maxTokens = 1024
            )
            val result = aiService.chat(request, useCache = false)
            if (result.isFailure) return null
            val content = result.getOrNull()?.content ?: return null
            val json = extractJson(content) ?: return null
            val type = object : TypeToken<List<CrossYearInsight>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun detectBlindSpots(year: Int, ctx: AiContextBundle): List<BlindSpot> {
        if (!aiEnabled) return emptyList()
        val zone = ZoneId.systemDefault()
        val allPreviews = dao.getAllPreviewsOnce()
        val yearEntries = allPreviews
            .filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().year == year }
            .sortedBy { it.createdAt }

        val gaps = mutableListOf<Pair<LocalDate, LocalDate>>()
        val dates = yearEntries.map {
            Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        }.sorted().toSet().toList()
        for (i in 1 until dates.size) {
            val gap = java.time.temporal.ChronoUnit.DAYS.between(dates[i - 1], dates[i])
            if (gap >= 5) {
                gaps.add(dates[i - 1] to dates[i])
            }
        }

        if (gaps.isEmpty()) return emptyList()

        val beforeAfterEntries = gaps.flatMap { (start, end) ->
            val before = yearEntries.filter {
                val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
                d == start
            }.take(1)
            val after = yearEntries.filter {
                val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
                d == end
            }.take(1)
            before + after
        }.distinct().take(10)

        val sampleText = beforeAfterEntries.joinToString("\n\n") { entry ->
            val d = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            "[${d}] ${entry.title} - ${entry.plainText.take(200)}"
        }

        val gapDesc = gaps.joinToString("; ") { (s, e) ->
            "${s}到${e}（${java.time.temporal.ChronoUnit.DAYS.between(s, e)}天）"
        }

        val prompt = """
在${year}年的日记中，发现以下沉默期：
$gapDesc

沉默期前后的日记内容：
$sampleText

请推断每个沉默期可能的原因（生活事件、工作忙碌、情绪低谷等）。输出格式，只返回JSON数组：
[
  {
    "periodStart": "沉默开始日期 yyyy-MM-dd",
    "periodEnd": "沉默结束日期 yyyy-MM-dd",
    "inferredReason": "推断的原因（中文，30-80字）",
    "followUpEntryId": null,
    "confidence": 0.7
  }
]
""".trimIndent()
        return try {
            val request = aiRequest(
                userMessage = prompt,
                systemPrompt = "你是生活分析师，从日记沉默期推断用户的生活状态。用中文回复。",
                temperature = 0.5f,
                maxTokens = 1024
            )
            val result = aiService.chat(request, useCache = false)
            if (result.isFailure) return emptyList()
            val content = result.getOrNull()?.content ?: return emptyList()
            val json = extractJson(content) ?: return emptyList()

            val raw = try {
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                gson.fromJson<List<Map<String, Any?>>>(json, type)
            } catch (e: Exception) { null } ?: return emptyList()

            raw.map { map ->
                BlindSpot(
                    periodStart = try { LocalDate.parse(map["periodStart"] as String) } catch (e: Exception) { LocalDate.now() },
                    periodEnd = try { LocalDate.parse(map["periodEnd"] as String) } catch (e: Exception) { LocalDate.now() },
                    inferredReason = (map["inferredReason"] as? String) ?: "",
                    followUpEntryId = (map["followUpEntryId"] as? Double)?.toLong(),
                    confidence = (map["confidence"] as? Double)?.toFloat() ?: 0.5f
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addAnnotation(chapterTitle: String, paragraphIndex: Int, note: String) {
        val annotation = UserAnnotation(
            id = UUID.randomUUID().toString(),
            chapterTitle = chapterTitle,
            paragraphIndex = paragraphIndex,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        val current = _story.value
        _story.value = current.copy(
            userAnnotations = current.userAnnotations + annotation
        )
        saveAnnotations(_story.value.userAnnotations)
    }

    fun removeAnnotation(id: String) {
        val current = _story.value
        _story.value = current.copy(
            userAnnotations = current.userAnnotations.filter { it.id != id }
        )
        saveAnnotations(_story.value.userAnnotations)
    }

    fun toggleEntrySelection(entryId: Long) {
        val current = _selectedEntryIds.value.toMutableSet()
        if (current.contains(entryId)) current.remove(entryId) else current.add(entryId)
        _selectedEntryIds.value = current
    }

    fun setExpandedChapter(index: Int?) {
        _expandedChapterIndex.value = index
    }

    private fun saveAnnotations(annotations: List<UserAnnotation>) {
        val json = gson.toJson(annotations)
        annotationPrefs.edit().putString("annotations_${_story.value.year}", json).apply()
    }

    private fun loadAnnotations(): List<UserAnnotation> {
        val json = annotationPrefs.getString("annotations_${_story.value.year}", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserAnnotation>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) return text.substring(start, end + 1)
        val arrStart = text.indexOf('[')
        val arrEnd = text.lastIndexOf(']')
        if (arrStart >= 0 && arrEnd > arrStart) return text.substring(arrStart, arrEnd + 1)
        return null
    }
}
