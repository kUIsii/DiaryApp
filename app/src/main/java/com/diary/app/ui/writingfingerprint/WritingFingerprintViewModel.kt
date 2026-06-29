package com.diary.app.ui.writingfingerprint

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.data.DiaryEntry
import com.diary.app.data.Tag
import com.diary.app.data.WritingFingerprint
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeRange(val label: String, val days: Int) {
    WEEK_7("\u8FD17\u5929", 7),
    MONTH_30("\u8FD130\u5929", 30),
    QUARTER_90("\u8FD190\u5929", 90),
    ALL("\u5168\u90E8", 0)
}

data class StylePeriod(
    val startDate: Long,
    val endDate: Long,
    val characteristics: List<String>,
    val sampleEntryId: Long,
    val label: String
)

data class WritingHealthScore(
    val score: Int,
    val consistency: Float,
    val diversity: Float,
    val emotionalDepth: Float,
    val selfReflection: Float,
    val tips: List<String>
)

data class WritingFingerprintAnalysis(
    val id: String,
    val analyzedAt: Long,
    val dimensions: Map<String, Float>,
    val stylePeriods: List<StylePeriod>,
    val persona: String,
    val healthScore: WritingHealthScore?,
    val totalEntries: Int,
    val timeRange: TimeRange,
    val comparativeInsight: String? = null,
    val previousDimensions: Map<String, Float>? = null,
    val aiEnabled: Boolean = false
)

class WritingFingerprintViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("writing_fingerprint", Context.MODE_PRIVATE)

    private val _analysis = MutableStateFlow<WritingFingerprintAnalysis?>(null)
    val analysis: StateFlow<WritingFingerprintAnalysis?> = _analysis.asStateFlow()

    private val _fingerprints = MutableStateFlow<List<WritingFingerprint>>(emptyList())
    val fingerprints: StateFlow<List<WritingFingerprint>> = _fingerprints.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow(TimeRange.ALL)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _availableTags = MutableStateFlow<List<Tag>>(emptyList())
    val availableTags: StateFlow<List<Tag>> = _availableTags.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val dimensionKeys = listOf(
        "\u8BCD\u6C47\u4E30\u5BCC\u5EA6",
        "\u53E5\u5F0F\u590D\u6742\u5EA6",
        "\u60C5\u611F\u8868\u8FBE",
        "\u65F6\u95F4\u89C6\u89D2",
        "\u4E3B\u9898\u504F\u597D",
        "\u4FEE\u8F9E\u4F7F\u7528"
    )

    private val emotionWords = setOf(
        "\u5F00\u5FC3", "\u5FEB\u4E50", "\u9AD8\u5174", "\u559C\u60A6", "\u5174\u594B",
        "\u6124\u6012", "\u751F\u6C14", "\u7B11", "\u54ED", "\u60B2\u4F24",
        "\u96BE\u8FC7", "\u5FC3\u75DB", "\u5FC3\u70E6", "\u7126\u8651", "\u5B89\u5FC3",
        "\u5E78\u798F", "\u5BC2\u5BDE", "\u5B64\u72EC", "\u6EE1\u8DB3", "\u5931\u843D",
        "\u7D27\u5F20", "\u5BB3\u6015", "\u611F\u52A8", "\u6E29\u6696", "\u601D\u5FF5",
        "\u61CA\u6094", "\u5ACC\u5F03", "\u538C\u6076", "\u559C\u7231", "\u75B2\u60EB"
    )

    private val pastMarkers = setOf(
        "\u4E86", "\u66FE", "\u5DF2", "\u8FC7", "\u4EE5\u524D", "\u8FC7\u53BB",
        "\u66FE\u7ECF", "\u5DF2\u7ECF", "\u5C06\u8981", "\u5F53\u65F6", "\u90A3\u5929"
    )
    private val presentMarkers = setOf(
        "\u5728", "\u6B63\u5728", "\u73B0\u5728", "\u4ECA\u5929", "\u76EE\u524D",
        "\u5982\u4ECA", "\u5F53\u4E0B", "\u8FD9\u4F1A"
    )
    private val futureMarkers = setOf(
        "\u5C06", "\u4F1A", "\u8981", "\u4EE5\u540E", "\u672A\u6765",
        "\u5C06\u6765", "\u5E0C\u671B", "\u6253\u7B97", "\u51C6\u5907"
    )

    private val selfReflectionMarkers = setOf(
        "\u89C9\u5F97", "\u611F\u89C9", "\u601D\u8003", "\u53CD\u601D", "\u660E\u767D",
        "\u61C2\u5F97", "\u8BA4\u8BC6\u5230", "\u53D1\u73B0", "\u6211\u60F3", "\u6211\u89C9",
        "\u4E3A\u4EC0\u4E48", "\u5982\u679C", "\u5E0C\u671B\u81EA\u5DF1"
    )

    init {
        loadTags()
        analyze()
    }

    fun isAiAvailable(): Boolean = aiService.isAiEnabled()

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        analyze()
    }

    fun setComparativeTag(tagId: Long?) {
        _selectedTagId.value = tagId
        if (tagId != null) runComparativeAnalysis() else analyze()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _availableTags.value = dao.getAllTagsOnce()
        }
    }

    private fun getCachedAnalysis(range: TimeRange): WritingFingerprintAnalysis? {
        val key = "analysis_${range.name}"
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, WritingFingerprintAnalysis::class.java)
        } catch (_: Exception) { null }
    }

    private fun cacheAnalysis(range: TimeRange, analysis: WritingFingerprintAnalysis) {
        val key = "analysis_${range.name}"
        prefs.edit().putString(key, gson.toJson(analysis)).apply()
    }

    fun analyze() {
        viewModelScope.launch {
            val allEntries = dao.getAllEntriesOnce()
            if (allEntries.isEmpty()) {
                _analysis.value = null
                return@launch
            }

            val range = _selectedTimeRange.value
            val now = System.currentTimeMillis()
            val cutoff = if (range.days > 0) now - range.days * 86400000L else 0L
            val entries = allEntries.filter { it.createdAt >= cutoff }

            if (entries.isEmpty()) {
                _analysis.value = null
                return@launch
            }

            val prevCutoff = if (range.days > 0) cutoff - range.days * 86400000L else 0L
            val prevEntries = allEntries.filter { it.createdAt >= prevCutoff && it.createdAt < cutoff }

            val dimensions = computeDimensions(entries)
            val prevDimensions = if (prevEntries.isNotEmpty()) computeDimensions(prevEntries) else null
            val periods = generateStylePeriods(allEntries)
            val health = computeHealthScore(allEntries)
            val persona = generatePersona(dimensions)

            val id = "fp_${now}"
            val analysis = WritingFingerprintAnalysis(
                id = id,
                analyzedAt = now,
                dimensions = dimensions,
                stylePeriods = periods,
                persona = persona,
                healthScore = health,
                totalEntries = entries.size,
                timeRange = range,
                comparativeInsight = null,
                previousDimensions = prevDimensions,
                aiEnabled = false
            )

            _analysis.value = analysis
            cacheAnalysis(range, analysis)

            if (aiService.isAiEnabled()) {
                enhanceWithAi(entries, allEntries, analysis)
            }
        }
    }

    private fun computeDimensions(entries: List<DiaryEntry>): Map<String, Float> {
        if (entries.isEmpty()) return dimensionKeys.associateWith { 0f }

        var totalSentences = 0
        var totalChars = 0
        val allWords = mutableListOf<String>()
        var totalPunctuation = 0

        var emotionWordCount = 0
        var totalWordsInDim = 0
        var pastCount = 0
        var presentCount = 0
        var futureCount = 0
        var selfReflectionCount = 0

        val wordFreq = mutableMapOf<String, Int>()

        for (entry in entries) {
            val text = entry.plainText
            if (text.isBlank()) continue

            totalChars += text.length

            val sentences = text.split(Regex("[.!?\u3002\uFF01\uFF1F\\n]+")).filter { it.isNotBlank() }
            totalSentences += sentences.size

            totalPunctuation += text.count { it in "\uFF0C,\u3002.\uFF01\uFF1F!?\u3001\uFF1B;" }

            val words = extractChineseWords(text)
            allWords.addAll(words)

            for (word in words) {
                wordFreq[word] = (wordFreq[word] ?: 0) + 1
                totalWordsInDim++
                if (word in emotionWords) emotionWordCount++
                if (word in selfReflectionMarkers) selfReflectionCount++
            }

            for (ch in text) {
                val seq = ch.toString()
                if (seq in pastMarkers) pastCount++
                if (seq in presentMarkers) presentCount++
                if (seq in futureMarkers) futureCount++
            }
        }

        if (totalWordsInDim == 0) return dimensionKeys.associateWith { 0f }

        val uniqueWords = wordFreq.size.toFloat()
        val totalWordsCount = allWords.size.toFloat()
        val vocabRichness = if (totalWordsCount > 0) (uniqueWords / totalWordsCount).coerceIn(0f, 1f) else 0f

        val avgSentenceLen = if (totalSentences > 0) (totalChars.toFloat() / totalSentences).coerceIn(0f, 100f) / 100f else 0f

        val emotionExpr = (emotionWordCount.toFloat() / totalWordsInDim).coerceIn(0f, 1f) * 2f
        val emotionalExpr = emotionExpr.coerceIn(0f, 1f)

        val totalTime = (pastCount + presentCount + futureCount).coerceAtLeast(1)
        val pastRatio = pastCount.toFloat() / totalTime
        val presentRatio = presentCount.toFloat() / totalTime
        val futureRatio = futureCount.toFloat() / totalTime
        val maxTimeRatio = maxOf(pastRatio, presentRatio, futureRatio)
        val temporalPerspective = (maxTimeRatio * 2f - 0.5f).coerceIn(0f, 1f)

        val topicWords = wordFreq.entries
            .filter { it.key.length >= 2 }
            .sortedByDescending { it.value }
            .take(20)
        val topicDiversity = (topicWords.size.toFloat() / 20f).coerceIn(0f, 1f)

        val colonRatio = textColonRatio(entries)
        val rhetorical = ((totalPunctuation.toFloat() / totalChars.coerceAtLeast(1)) * 10f + colonRatio * 5f)
            .coerceIn(0f, 1f)

        return mapOf(
            "\u8BCD\u6C47\u4E30\u5BCC\u5EA6" to vocabRichness,
            "\u53E5\u5F0F\u590D\u6742\u5EA6" to avgSentenceLen,
            "\u60C5\u611F\u8868\u8FBE" to emotionalExpr,
            "\u65F6\u95F4\u89C6\u89D2" to temporalPerspective,
            "\u4E3B\u9898\u504F\u597D" to topicDiversity,
            "\u4FEE\u8F9E\u4F7F\u7528" to rhetorical
        )
    }

    private fun textColonRatio(entries: List<DiaryEntry>): Float {
        var colonCount = 0
        var totalChars = 0
        for (entry in entries) {
            val text = entry.plainText
            colonCount += text.count { it == '\uFF1A' || it == ':' }
            totalChars += text.length
        }
        return if (totalChars > 0) colonCount.toFloat() / totalChars else 0f
    }

    private fun generateStylePeriods(allEntries: List<DiaryEntry>): List<StylePeriod> {
        if (allEntries.size < 3) {
            return if (allEntries.isEmpty()) emptyList()
            else listOf(
                StylePeriod(
                    startDate = allEntries.minOf { it.createdAt },
                    endDate = allEntries.maxOf { it.createdAt },
                    characteristics = listOf("\u5168\u90E8\u65E5\u8BB0"),
                    sampleEntryId = allEntries.maxByOrNull { it.plainText.length }?.id ?: allEntries.first().id,
                    label = "\u5168\u90E8\u65F6\u671F"
                )
            )
        }

        val sorted = allEntries.sortedBy { it.createdAt }
        val periodSize = (sorted.size / 3).coerceAtLeast(1)
        val periods = mutableListOf<StylePeriod>()

        for (i in 0 until 3) {
            val start = i * periodSize
            val end = if (i == 2) sorted.size else (i + 1) * periodSize
            val slice = sorted.subList(start, end)
            if (slice.isEmpty()) continue

            val dims = computeDimensions(slice)
            val topDim = dims.maxByOrNull { it.value }
            val label = when (topDim?.key) {
                "\u8BCD\u6C47\u4E30\u5BCC\u5EA6" -> "\u8BCD\u6C47\u4E30\u5BCC\u671F"
                "\u53E5\u5F0F\u590D\u6742\u5EA6" -> "\u590D\u6742\u53E5\u5F0F\u671F"
                "\u60C5\u611F\u8868\u8FBE" -> "\u60C5\u611F\u4E30\u76DB\u671F"
                "\u65F6\u95F4\u89C6\u89D2" -> "\u65F6\u95F4\u53D8\u5316\u671F"
                "\u4E3B\u9898\u504F\u597D" -> "\u4E3B\u9898\u591A\u6837\u671F"
                "\u4FEE\u8F9E\u4F7F\u7528" -> "\u4FEE\u8F9E\u4E30\u5BCC\u671F"
                else -> "\u7A33\u5B9A\u5199\u4F5C\u671F"
            }

            val sortedDims = dims.entries.sortedByDescending { it.value }.take(3)
            val characteristics = sortedDims.map { (k, v) ->
                "$k: ${"%.0f".format(v * 100)}%"
            }

            periods.add(
                StylePeriod(
                    startDate = slice.first().createdAt,
                    endDate = slice.last().createdAt,
                    characteristics = characteristics,
                    sampleEntryId = slice.maxByOrNull { it.plainText.length }?.id ?: slice.first().id,
                    label = label
                )
            )
        }

        return periods
    }

    private fun computeHealthScore(allEntries: List<DiaryEntry>): WritingHealthScore {
        if (allEntries.size < 3) {
            return WritingHealthScore(
                score = 50, consistency = 0.5f, diversity = 0.5f,
                emotionalDepth = 0.5f, selfReflection = 0.5f,
                tips = listOf("\u7EE7\u7EED\u5199\u4E0B\u53BB\uFF0C\u8BB0\u5F55\u66F4\u591A\u53CD\u601D")
            )
        }

        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 86400000L
        val weekEntries = allEntries.filter { it.createdAt >= weekAgo }

        if (weekEntries.isEmpty()) {
            return WritingHealthScore(
                score = 30, consistency = 0.3f, diversity = 0.3f,
                emotionalDepth = 0.3f, selfReflection = 0.3f,
                tips = listOf("\u8FD9\u5468\u8FD8\u6CA1\u6709\u5199\u65E5\u8BB0\uFF0C\u52A0\u6CB9\u5427")
            )
        }

        val dims = computeDimensions(weekEntries)
        val allDims = computeDimensions(allEntries)

        val consistency = (1f - dimensionKeys.map { k ->
            kotlin.math.abs((dims[k] ?: 0f) - (allDims[k] ?: 0f))
        }.average().toFloat()).coerceIn(0f, 1f)

        val diversity = (dims["\u8BCD\u6C47\u4E30\u5BCC\u5EA6"] ?: 0.5f).coerceIn(0f, 1f)

        val emotionalDepth = (dims["\u60C5\u611F\u8868\u8FBE"] ?: 0.5f).coerceIn(0f, 1f)

        var selfRefCount = 0
        var totalCheckWords = 0
        for (entry in weekEntries) {
            val words = extractChineseWords(entry.plainText)
            for (w in words) {
                if (w in selfReflectionMarkers) selfRefCount++
            }
            totalCheckWords += words.size
        }
        val selfReflection = if (totalCheckWords > 0)
            (selfRefCount.toFloat() / totalCheckWords * 5f).coerceIn(0f, 1f) else 0.3f

        val score = ((consistency * 25f + diversity * 25f + emotionalDepth * 25f + selfReflection * 25f).toInt())
            .coerceIn(0, 100)

        val tips = mutableListOf<String>()
        if (consistency < 0.4f) tips.add("\u98CE\u683C\u6CE2\u52A8\u8F83\u5927\uFF0C\u5C1D\u8BD5\u4FDD\u6301\u7A33\u5B9A\u7684\u5199\u4F5C\u8282\u594F")
        if (diversity < 0.4f) tips.add("\u7528\u8BCD\u53EF\u4EE5\u66F4\u4E30\u5BCC\u4E00\u4E9B\uFF0C\u5C1D\u8BD5\u65B0\u7684\u8868\u8FBE")
        if (emotionalDepth < 0.4f) tips.add("\u591A\u63CF\u8FF0\u5185\u5FC3\u611F\u53D7\uFF0C\u8BA9\u65E5\u8BB0\u66F4\u6709\u6E29\u5EA6")
        if (selfReflection < 0.4f) tips.add("\u5C1D\u8BD5\u56DE\u987E\u548C\u53CD\u601D\uFF0C\u8FD9\u4F1A\u8BA9\u5199\u4F5C\u66F4\u6DF1\u5165")
        if (tips.isEmpty()) tips.add("\u7EE7\u7EED\u4FDD\u6301\uFF0C\u4F60\u7684\u5199\u4F5C\u72B6\u6001\u5F88\u597D")

        return WritingHealthScore(
            score = score,
            consistency = consistency.coerceIn(0f, 1f),
            diversity = diversity.coerceIn(0f, 1f),
            emotionalDepth = emotionalDepth.coerceIn(0f, 1f),
            selfReflection = selfReflection.coerceIn(0f, 1f),
            tips = tips
        )
    }

    private fun generatePersona(dimensions: Map<String, Float>): String {
        val vocab = dimensions["\u8BCD\u6C47\u4E30\u5BCC\u5EA6"] ?: 0.5f
        val sentence = dimensions["\u53E5\u5F0F\u590D\u6742\u5EA6"] ?: 0.5f
        val emotion = dimensions["\u60C5\u611F\u8868\u8FBE"] ?: 0.5f
        val time = dimensions["\u65F6\u95F4\u89C6\u89D2"] ?: 0.5f
        val topic = dimensions["\u4E3B\u9898\u504F\u597D"] ?: 0.5f
        val rhetoric = dimensions["\u4FEE\u8F9E\u4F7F\u7528"] ?: 0.5f

        val traits = mutableListOf<String>()

        when {
            vocab > 0.6f -> traits.add("\u8BCD\u6C47\u4E30\u5BCC\uFF0C\u64C5\u957F\u7CBE\u51C6\u8868\u8FBE")
            vocab < 0.3f -> traits.add("\u7528\u8BCD\u7B80\u6D01\u76F4\u63A5")
            else -> traits.add("\u8BCD\u6C47\u8F83\u4E3A\u5747\u8861")
        }

        when {
            sentence > 0.6f -> traits.add("\u559C\u6B22\u5199\u590D\u6742\u53E5\u5F0F\uFF0C\u601D\u7EF4\u5BC6\u96C6")
            sentence < 0.3f -> traits.add("\u77ED\u53E5\u4E3A\u4E3B\uFF0C\u8282\u594F\u660E\u5FEB")
            else -> traits.add("\u53E5\u5F0F\u957F\u77ED\u76F8\u95F4")
        }

        when {
            emotion > 0.5f -> traits.add("\u60C5\u611F\u4E30\u6C9B\uFF0C\u64C5\u4E8E\u8868\u8FBE\u5185\u5FC3\u611F\u53D7")
            else -> traits.add("\u60C5\u611F\u8868\u8FBE\u8F83\u4E3A\u542B\u853D")
        }

        when {
            rhetoric > 0.5f -> traits.add("\u559C\u6B22\u7528\u6BD4\u55BB\u548C\u4FEE\u8F9E")
            else -> traits.add("\u5199\u4F5C\u98CE\u683C\u5E73\u5B9E")
        }

        when {
            time > 0.5f -> traits.add("\u65F6\u95F4\u89C6\u89D2\u591A\u5143\uFF0C\u64C5\u4E8E\u7A7F\u68AD\u56DE\u5FC6\u4E0E\u5C55\u671B")
            else -> traits.add("\u4E3B\u8981\u5173\u6CE8\u5F53\u4E0B")
        }

        return "\u4F60\u662F\u4E00\u4E2A${traits.joinToString("\uFF0C")}\u7684\u4EBA\u3002"
    }

    private fun enhanceWithAi(
        entries: List<DiaryEntry>,
        allEntries: List<DiaryEntry>,
        currentAnalysis: WritingFingerprintAnalysis
    ) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            try {
                val sampleTexts = entries.take(10).joinToString("\n---\n") { it.plainText.take(300) }

                val systemPrompt = "\u4F60\u662F\u4E00\u4E2A\u4E13\u4E1A\u7684\u5199\u4F5C\u5206\u6790\u5E08\u3002\u8BF7\u5206\u6790\u4EE5\u4E0B\u65E5\u8BB0\u5185\u5BB9\uFF0C\u7528\u4E00\u6BB5\u8BDD\u63CF\u8FF0\u5199\u4F5C\u98CE\u683C\u7279\u70B9\uFF0C\u5305\u62EC\u7528\u8BCD\u3001\u53E5\u5F0F\u3001\u60C5\u611F\u3001\u4E3B\u9898\u7B49\u65B9\u9762\u3002\u8BED\u6C14\u81EA\u7136\uFF0C\u50CF\u670B\u53CB\u5728\u8C08\u8BBA\u4F60\u7684\u5199\u4F5C\u3002\u4E0D\u8981\u7528\u5F15\u53F7\u3001\u7834\u6298\u53F7\u7B49\u7279\u6B8A\u7B26\u53F7\u3002"

                val result = aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", systemPrompt),
                            AiMessage("user", "\u4EE5\u4E0B\u662F\u6211\u7684\u65E5\u8BB0\u90E8\u5206\u5185\u5BB9\uFF1A\n\n$sampleTexts")
                        ),
                        temperature = 0.7f,
                        maxTokens = 256
                    )
                )

                val aiNote = result.getOrNull()?.content?.trim()
                if (aiNote != null) {
                    val enhanced = currentAnalysis.copy(
                        persona = aiNote,
                        aiEnabled = true
                    )
                    _analysis.value = enhanced
                    cacheAnalysis(_selectedTimeRange.value, enhanced)
                }
            } catch (e: Exception) {
                _aiError.value = e.message
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private fun runComparativeAnalysis() {
        val tagId = _selectedTagId.value ?: return
        viewModelScope.launch {
            val allEntries = dao.getAllEntriesOnce()
            val taggedIds = dao.getTagsForDiary(-1)
                .let { dao.getAllDiaryTagPairsOnce() }
                .filter { it.tagId == tagId }
                .map { it.diaryId }
                .toSet()

            val tagEntries = allEntries.filter { it.id in taggedIds }
            val otherEntries = allEntries.filter { it.id !in taggedIds }

            if (tagEntries.isEmpty() || otherEntries.isEmpty()) {
                _analysis.value = _analysis.value?.copy(comparativeInsight = null)
                return@launch
            }

            val tagDims = computeDimensions(tagEntries)
            val otherDims = computeDimensions(otherEntries)

            val insightParts = mutableListOf<String>()
            for (key in dimensionKeys) {
                val tagVal = tagDims[key] ?: 0f
                val otherVal = otherDims[key] ?: 0f
                val diff = tagVal - otherVal
                if (kotlin.math.abs(diff) > 0.05f) {
                    val dir = if (diff > 0) "\u9AD8" else "\u4F4E"
                    insightParts.add("$key$dir ${"%.0f".format(kotlin.math.abs(diff) * 100)}%")
                }
            }

            val tagName = _availableTags.value.find { it.id == tagId }?.name ?: "\u8BE5\u6807\u7B7E"
            val comparativeInsight = if (insightParts.isNotEmpty()) {
                "\u5199\u300C$tagName\u300D\u65F6\uFF0C${insightParts.joinToString("\uFF0C")}\u3002"
            } else {
                "\u5199\u300C$tagName\u300D\u65F6\u98CE\u683C\u4E0E\u5176\u4ED6\u65E5\u8BB0\u5DEE\u5F02\u4E0D\u5927\u3002"
            }

            val current = _analysis.value
            if (current != null) {
                _analysis.value = current.copy(
                    comparativeInsight = comparativeInsight,
                    previousDimensions = otherDims,
                    dimensions = tagDims
                )
            }
        }
    }

    private fun extractChineseWords(text: String): List<String> {
        val words = mutableListOf<String>()
        val currentWord = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                currentWord.append(ch)
            } else {
                if (currentWord.isNotEmpty()) {
                    val word = currentWord.toString()
                    if (word.length >= 2 || word.all { it.isLowerCase() || it.isUpperCase() }) {
                        words.add(word)
                    }
                    currentWord.clear()
                }
            }
        }
        if (currentWord.isNotEmpty()) {
            val word = currentWord.toString()
            if (word.length >= 2 || word.all { it.isLowerCase() || it.isUpperCase() }) {
                words.add(word)
            }
        }
        return words
    }
}
