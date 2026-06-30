package com.diary.app.ui.outline

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class OutlineItem(
    val title: String,
    val level: Int,
    val charOffset: Int
)

data class OutlineData(
    val items: List<OutlineItem>,
    val totalWords: Int,
    val paragraphCount: Int,
    val estimatedReadMinutes: Int
)

enum class OutlineMode { SINGLE, TIME_RANGE, THEME }

data class AiTopic(
    val name: String,
    val entryCount: Int,
    val representativeTitle: String,
    val insight: String
)

data class SentimentPoint(
    val paragraphIndex: Int,
    val score: Float
)

data class WordFreq(
    val word: String,
    val count: Int
)

class OutlineViewViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val aiService = app.aiService
    private val sessionStore = app.readingSessionStore
    private val gson = Gson()
    private val sp = application.getSharedPreferences("outline_ai_cache", Context.MODE_PRIVATE)
    private val tagSp = application.getSharedPreferences("outline_ai_tags", Context.MODE_PRIVATE)

    private val _outline = MutableStateFlow<OutlineData?>(null)
    val outline: StateFlow<OutlineData?> = _outline.asStateFlow()

    private val _bodyContent = MutableStateFlow<String?>(null)
    val bodyContent: StateFlow<String?> = _bodyContent.asStateFlow()

    private val _mode = MutableStateFlow(OutlineMode.SINGLE)
    val mode: StateFlow<OutlineMode> = _mode.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()
    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _aiTopics = MutableStateFlow<List<AiTopic>?>(null)
    val aiTopics: StateFlow<List<AiTopic>?> = _aiTopics.asStateFlow()
    private val _aiTopicsLoading = MutableStateFlow(false)
    val aiTopicsLoading: StateFlow<Boolean> = _aiTopicsLoading.asStateFlow()

    private val _aiTags = MutableStateFlow<List<String>>(emptyList())
    val aiTags: StateFlow<List<String>> = _aiTags.asStateFlow()
    private val _aiTagsLoading = MutableStateFlow(false)
    val aiTagsLoading: StateFlow<Boolean> = _aiTagsLoading.asStateFlow()

    private val _sentimentPoints = MutableStateFlow<List<SentimentPoint>>(emptyList())
    val sentimentPoints: StateFlow<List<SentimentPoint>> = _sentimentPoints.asStateFlow()

    private val _wordFrequencies = MutableStateFlow<List<WordFreq>>(emptyList())
    val wordFrequencies: StateFlow<List<WordFreq>> = _wordFrequencies.asStateFlow()

    private val _paragraphLengths = MutableStateFlow<List<Int>>(emptyList())
    val paragraphLengths: StateFlow<List<Int>> = _paragraphLengths.asStateFlow()

    private val _highlightParagraph = MutableStateFlow<Int?>(null)
    val highlightParagraph: StateFlow<Int?> = _highlightParagraph.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _comparisonIds = MutableStateFlow<List<Long>>(emptyList())
    val comparisonIds: StateFlow<List<Long>> = _comparisonIds.asStateFlow()
    private val _comparisonOutlines = MutableStateFlow<List<OutlineData>>(emptyList())
    val comparisonOutlines: StateFlow<List<OutlineData>> = _comparisonOutlines.asStateFlow()
    private val _comparisonBodies = MutableStateFlow<List<String>>(emptyList())
    val comparisonBodies: StateFlow<List<String>> = _comparisonBodies.asStateFlow()

    private var loadJob: Job? = null
    private var currentDiaryId: Long? = null

    fun loadDiary(diaryId: Long) {
        currentDiaryId = diaryId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val entry = dao.getEntryById(diaryId) ?: return@launch
            val preview = dao.getPreviewById(diaryId)
            _bodyContent.value = entry.plainText
            processText(entry.plainText)
            loadAiTags(diaryId, entry.plainText)
            preview?.let { sessionStore.setEntry(it) }
        }
    }

    fun loadCurrentSessionDiaryIfNeeded() {
        val sessionDiaryId = sessionStore.session.value.diaryId ?: return
        if (currentDiaryId == null) {
            loadDiary(sessionDiaryId)
        }
    }

    private fun processText(text: String) {
        if (text.isBlank()) {
            _outline.value = null
            _sentimentPoints.value = emptyList()
            _wordFrequencies.value = emptyList()
            _paragraphLengths.value = emptyList()
            return
        }
        val items = mutableListOf<OutlineItem>()
        val lines = text.split("\n")
        var offset = 0
        lines.forEach { line ->
            val trimmed = line.trim()
            val lineFeedLen = if (line.endsWith("\r")) 2 else 1
            if (trimmed.isEmpty()) { offset += line.length + lineFeedLen; return@forEach }
            when {
                trimmed.startsWith("# ") -> items.add(OutlineItem(trimmed.removePrefix("# ").trim(), 0, offset))
                trimmed.startsWith("## ") -> items.add(OutlineItem(trimmed.removePrefix("## ").trim(), 1, offset))
                trimmed.startsWith("### ") -> items.add(OutlineItem(trimmed.removePrefix("### ").trim(), 2, offset))
                (trimmed.endsWith("：") || (trimmed.endsWith(":") && !trimmed.contains("://"))) -> {
                    if (trimmed.length < 30) items.add(OutlineItem(trimmed, 0, offset))
                }
            }
            offset += line.length + lineFeedLen
        }
        val words = text.length
        val paragraphs = text.split(Regex("\n\\s*\n")).filter { it.isNotBlank() }
        val readMinutes = (words / 300).coerceAtLeast(1)
        _outline.value = OutlineData(items, words, paragraphs.size, readMinutes)
        computeSentiment(paragraphs)
        computeWordFrequencies(text)
        _paragraphLengths.value = paragraphs.map { it.length }
    }

    private fun computeSentiment(paragraphs: List<String>) {
        val dict = mapOf(
            "开心" to 1f, "快乐" to 1f, "高兴" to 1f, "幸福" to 1f, "满足" to 0.5f,
            "喜悦" to 1f, "兴奋" to 1f, "激动" to 1f, "愉快" to 1f, "美好" to 0.5f,
            "感动" to 0.5f, "感恩" to 0.5f, "希望" to 0.5f, "期待" to 0.5f,
            "难过" to -1f, "悲伤" to -1f, "伤心" to -1f, "痛苦" to -1f, "绝望" to -1.5f,
            "沮丧" to -1f, "失落" to -1f, "郁闷" to -1f, "焦虑" to -1f, "恐惧" to -1f,
            "害怕" to -1f, "愤怒" to -1f, "生气" to -1f, "烦躁" to -0.5f, "压力" to -0.5f,
            "孤独" to -1f, "迷茫" to -0.5f, "疲惫" to -0.5f
        )
        _sentimentPoints.value = paragraphs.mapIndexed { index, p ->
            val score = dict.entries.sumOf { (word, s) ->
                if (p.contains(word)) s.toDouble() else 0.0
            }.toFloat().coerceIn(-1f, 1f)
            SentimentPoint(index, score)
        }
    }

    private fun computeWordFrequencies(text: String) {
        val stopWords = setOf("的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
            "没有", "看", "好", "自己", "这", "他", "她", "它", "们", "那", "什么",
            "为", "对", "与", "从", "但", "而", "或", "被", "把", "让", "给", "能",
            "可以", "因为", "所以", "如果", "虽然", "但是", "已经", "还", "又", "再",
            "更", "最", "太", "多", "少", "大", "小", "得", "地", "过", "个", "之",
            "以", "及", "等", "中", "里", "时", "后", "前", "来")
        val cleaned = text.replace(Regex("[#*\\d\\s《》（）()\\[\\]【】\"'「」『』\\-—,.;:！？，。；：、…·]"), "")
        val wordCounts = mutableMapOf<String, Int>()
        var i = 0
        while (i < cleaned.length - 1) {
            val bigram = cleaned.substring(i, i + 2)
            if (bigram.all { it in '\u4e00'..'\u9fff' } && bigram !in stopWords) {
                wordCounts[bigram] = (wordCounts[bigram] ?: 0) + 1
            }
            i++
        }
        _wordFrequencies.value = wordCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { WordFreq(it.key, it.value) }
    }

    fun setMode(mode: OutlineMode) {
        _mode.value = mode
        when (mode) {
            OutlineMode.SINGLE -> currentDiaryId?.let { loadDiary(it) }
            OutlineMode.TIME_RANGE -> {}
            OutlineMode.THEME -> loadThemeAnalysis()
        }
    }

    fun setStartDate(millis: Long?) { _startDate.value = millis }
    fun setEndDate(millis: Long?) { _endDate.value = millis }

    fun loadTimeRangeAnalysis() {
        val start = _startDate.value ?: return
        val end = _endDate.value ?: return
        viewModelScope.launch {
            val entries = dao.getEntriesByDateRange(start, end)
            if (entries.isEmpty()) return@launch
            _aiTopicsLoading.value = true
            val summaries = entries.joinToString("\n---\n") { "${it.title}：${it.plainText.take(200)}" }
            val cacheKey = hashKey("range_${start}_${end}")
            val cached = sp.getString(cacheKey, null)
            val cachedTime = sp.getLong("${cacheKey}_time", 0)
            if (cached != null && System.currentTimeMillis() - cachedTime < 86400000L) {
                val type = object : TypeToken<List<AiTopic>>() {}.type
                val topics: List<AiTopic> = gson.fromJson(cached, type) ?: emptyList()
                _aiTopics.value = topics
                _aiTopicsLoading.value = false
                computeAggregatedStats(entries.map { it.plainText })
                return@launch
            }
            if (!aiService.isAiEnabled()) { _aiTopicsLoading.value = false; return@launch }
            val prompt = "以下是用户一段时间内的日记摘要。请提取3-8个核心主题，每个主题给出：主题名、涉及日记数、代表日记标题、AI解读。输出JSON。"
            val request = aiRequest(userMessage = summaries, systemPrompt = prompt, maxTokens = 800)
            aiService.chat(request).onSuccess { response ->
                val cleaned = response.content.trim()
                val startIdx = cleaned.indexOf('[')
                val endIdx = cleaned.lastIndexOf(']') + 1
                if (startIdx >= 0 && endIdx > startIdx) {
                    val json = cleaned.substring(startIdx, endIdx)
                    val type = object : TypeToken<List<AiTopic>>() {}.type
                    val topics: List<AiTopic> = gson.fromJson(json, type) ?: emptyList()
                    _aiTopics.value = topics
                    sp.edit().putString(cacheKey, json).putLong("${cacheKey}_time", System.currentTimeMillis()).apply()
                }
                _aiTopicsLoading.value = false
            }.onFailure { _aiTopicsLoading.value = false }
            computeAggregatedStats(entries.map { it.plainText })
        }
    }

    private fun loadThemeAnalysis() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            if (entries.isEmpty()) return@launch
            _aiTopicsLoading.value = true
            val summaries = entries.take(50).joinToString("\n---\n") { "${it.title}：${it.plainText.take(200)}" }
            if (!aiService.isAiEnabled()) { _aiTopicsLoading.value = false; return@launch }
            val prompt = "分析以下日记，提取全局3-8个核心主题，每个主题给出：主题名、涉及日记数、代表日记标题、AI解读。输出JSON。"
            val request = aiRequest(userMessage = summaries, systemPrompt = prompt, maxTokens = 800)
            aiService.chat(request).onSuccess { response ->
                val cleaned = response.content.trim()
                val startIdx = cleaned.indexOf('[')
                val endIdx = cleaned.lastIndexOf(']') + 1
                if (startIdx >= 0 && endIdx > startIdx) {
                    val json = cleaned.substring(startIdx, endIdx)
                    val type = object : TypeToken<List<AiTopic>>() {}.type
                    _aiTopics.value = gson.fromJson(json, type)
                }
                _aiTopicsLoading.value = false
            }.onFailure { _aiTopicsLoading.value = false }
            computeAggregatedStats(entries.map { it.plainText })
        }
    }

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun computeAggregatedStats(texts: List<String>) {
        val combined = texts.joinToString("\n")
        computeWordFrequencies(combined)
        val allParagraphs = texts.flatMap { it.split(Regex("\n\\s*\n")).filter { it.isNotBlank() } }
        _sentimentPoints.value = allParagraphs.mapIndexed { index, p ->
            val dict = mapOf("开心" to 1f, "快乐" to 1f, "幸福" to 1f, "满足" to 0.5f, "难过" to -1f, "悲伤" to -1f, "痛苦" to -1f, "焦虑" to -1f)
            val score = dict.entries.sumOf { (word, s) ->
                if (p.contains(word)) s.toDouble() else 0.0
            }.toFloat().coerceIn(-1f, 1f)
            SentimentPoint(index, score)
        }
        _paragraphLengths.value = allParagraphs.map { it.length }
    }

    private fun loadAiTags(entryId: Long, text: String) {
        val cached = tagSp.getString("$entryId", null)
        if (cached != null) {
            val type = object : TypeToken<List<String>>() {}.type
            _aiTags.value = gson.fromJson(cached, type)
            return
        }
        viewModelScope.launch {
            if (!aiService.isAiEnabled()) return@launch
            _aiTagsLoading.value = true
            val prompt = "根据以下日记内容，生成3-5个中文主题标签，用JSON数组返回。"
            val request = aiRequest(userMessage = text.take(1000), systemPrompt = prompt)
            aiService.chat(request).onSuccess { response ->
                val cleaned = response.content.trim()
                val s = cleaned.indexOf('[')
                val e = cleaned.lastIndexOf(']') + 1
                if (s >= 0 && e > s) {
                    val json = cleaned.substring(s, e)
                    val type = object : TypeToken<List<String>>() {}.type
                    val tags: List<String> = gson.fromJson(json, type) ?: emptyList()
                    _aiTags.value = tags
                    tagSp.edit().putString("$entryId", json).apply()
                }
                _aiTagsLoading.value = false
            }.onFailure { _aiTagsLoading.value = false }
        }
    }

    fun scrollToParagraph(charOffset: Int) {
        val body = _bodyContent.value ?: return
        val paragraphs = body.split(Regex("\n\\s*\n"))
        var current = 0
        for ((index, p) in paragraphs.withIndex()) {
            if (charOffset in current..(current + p.length)) {
                _highlightParagraph.value = index
                sessionStore.setParagraph(index)
                return
            }
            current += p.length + 2
        }
    }

    fun clearHighlight() {
        _highlightParagraph.value = null
    }

    fun addComparisonEntry(entryId: Long) {
        val current = _comparisonIds.value.toMutableList()
        if (current.size >= 5 || entryId in current) return
        current.add(entryId)
        _comparisonIds.value = current
        loadComparisonData()
    }

    fun removeComparisonEntry(entryId: Long) {
        val current = _comparisonIds.value.toMutableList()
        current.remove(entryId)
        _comparisonIds.value = current
        loadComparisonData()
    }

    private fun loadComparisonData() {
        val ids = _comparisonIds.value
        viewModelScope.launch {
            val outlines = mutableListOf<OutlineData>()
            val bodies = mutableListOf<String>()
            for (id in ids) {
                val entry = dao.getEntryById(id) ?: continue
                bodies.add(entry.plainText)
                outlines.add(parseOutlineText(entry.plainText))
            }
            _comparisonOutlines.value = outlines
            _comparisonBodies.value = bodies
        }
    }

    private fun parseOutlineText(text: String): OutlineData {
        val items = mutableListOf<OutlineItem>()
        val lines = text.split("\n")
        var offset = 0
        lines.forEach { line ->
            val trimmed = line.trim()
            val lf = if (line.endsWith("\r")) 2 else 1
            if (trimmed.isEmpty()) { offset += line.length + lf; return@forEach }
            when {
                trimmed.startsWith("# ") -> items.add(OutlineItem(trimmed.removePrefix("# ").trim(), 0, offset))
                trimmed.startsWith("## ") -> items.add(OutlineItem(trimmed.removePrefix("## ").trim(), 1, offset))
                trimmed.startsWith("### ") -> items.add(OutlineItem(trimmed.removePrefix("### ").trim(), 2, offset))
                (trimmed.endsWith("：") || (trimmed.endsWith(":") && !trimmed.contains("://"))) -> {
                    if (trimmed.length < 30) items.add(OutlineItem(trimmed, 0, offset))
                }
            }
            offset += line.length + lf
        }
        val words = text.length
        val paras = text.split(Regex("\n\\s*\n")).filter { it.isNotBlank() }
        return OutlineData(items, words, paras.size, (words / 300).coerceAtLeast(1))
    }

    fun toggleExportDialog() { _showExportDialog.value = !_showExportDialog.value }
    fun dismissExportDialog() { _showExportDialog.value = false }

    fun exportText(context: Context) {
        val body = _bodyContent.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "导出文本"))
        _showExportDialog.value = false
    }

    fun exportMarkdown(context: Context) {
        val body = _bodyContent.value ?: return
        val outline = _outline.value
        val sb = StringBuilder()
        outline?.items?.forEach { item ->
            val prefix = if (item.level == 0) "# " else if (item.level == 1) "## " else "### "
            sb.appendLine("$prefix${item.title}")
        }
        sb.appendLine()
        sb.append(body)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "导出Markdown"))
        _showExportDialog.value = false
    }

    fun generateContrastSummary(): String {
        val outlines = _comparisonOutlines.value
        if (outlines.size < 2) return ""
        val maxW = outlines.maxOf { it.totalWords }
        val minW = outlines.minOf { it.totalWords }
        val avgW = outlines.map { it.totalWords }.average().toInt()
        return "共对比${outlines.size}篇日记，最长${maxW}字，最短${minW}字，平均${avgW}字"
    }

    fun clearComparison() {
        _comparisonIds.value = emptyList()
        _comparisonOutlines.value = emptyList()
        _comparisonBodies.value = emptyList()
    }
}
