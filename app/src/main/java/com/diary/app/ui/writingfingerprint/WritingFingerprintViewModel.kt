package com.diary.app.ui.writingfingerprint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.WritingFingerprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeRange(val label: String, val days: Int) {
    WEEK_7("\u8FD17\u5929", 7),
    MONTH_30("\u8FD130\u5929", 30),
    QUARTER_90("\u8FD190\u5929", 90),
    ALL("\u5168\u90E8", 0)
}

data class FingerprintAnalysis(
    val avgSentenceLength: Float,
    val vocabularyRichness: Float,
    val avgWordLength: Float,
    val punctuationRatio: Float,
    val paragraphCount: Int,
    val avgParagraphLength: Float,
    val totalEntries: Int,
    val styleLabel: String,
    val evolutionNote: String,
    val dailyWordCounts: List<Pair<Long, Int>> = emptyList(),
    val timeDistribution: Map<String, Int> = emptyMap(),
    val creativityScore: Int = 0,
    val comparison: ComparisonData? = null,
    val timeRange: TimeRange = TimeRange.ALL
)

data class ComparisonData(
    val avgSentenceLengthChange: Float,
    val vocabularyRichnessChange: Float,
    val totalWordsChange: Int,
    val totalEntriesChange: Int
)

class WritingFingerprintViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _analysis = MutableStateFlow<FingerprintAnalysis?>(null)
    val analysis: StateFlow<FingerprintAnalysis?> = _analysis.asStateFlow()

    private val _fingerprints = MutableStateFlow<List<WritingFingerprint>>(emptyList())
    val fingerprints: StateFlow<List<WritingFingerprint>> = _fingerprints.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow(TimeRange.ALL)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    init {
        analyze()
    }

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        analyze()
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
            val prevCutoff = if (range.days > 0) cutoff - range.days * 86400000L else 0L
            val prevEntries = allEntries.filter { it.createdAt >= prevCutoff && it.createdAt < cutoff }

            if (entries.isEmpty()) {
                _analysis.value = null
                return@launch
            }

            var totalSentences = 0
            var totalChars = 0
            val allWords = mutableListOf<String>()
            var totalPunctuation = 0
            var totalParagraphs = 0

            val dailyWordMap = mutableMapOf<Long, Int>()
            val timeDistMap = mutableMapOf("\u65E9\u6668" to 0, "\u4E0B\u5348" to 0, "\u665A\u4E0A" to 0, "\u6DF1\u591C" to 0)

            entries.forEach { entry ->
                val text = entry.plainText
                if (text.isBlank()) return@forEach
                totalChars += text.length
                val sentences = text.split(Regex("[.!?\u3002\uFF01\uFF1F\\n]+")).filter { it.isNotBlank() }
                totalSentences += sentences.size
                val words = extractChineseWords(text)
                allWords.addAll(words)
                totalPunctuation += text.count { it in "\uFF0C,\u3002.\uFF01\uFF1F!?\u3001\uFF1B;" }
                val paragraphs = text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
                if (paragraphs.isEmpty()) {
                    val singleParagraphs = text.split("\n").filter { it.isNotBlank() }
                    totalParagraphs += singleParagraphs.size
                } else {
                    totalParagraphs += paragraphs.size
                }

                val dayKey = entry.createdAt / 86400000L
                dailyWordMap[dayKey] = (dailyWordMap[dayKey] ?: 0) + text.length

                val cal = Calendar.getInstance().apply { timeInMillis = entry.createdAt }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 6..11 -> timeDistMap["\u65E9\u6668"] = (timeDistMap["\u65E9\u6668"] ?: 0) + 1
                    in 12..17 -> timeDistMap["\u4E0B\u5348"] = (timeDistMap["\u4E0B\u5348"] ?: 0) + 1
                    in 18..23 -> timeDistMap["\u665A\u4E0A"] = (timeDistMap["\u665A\u4E0A"] ?: 0) + 1
                    else -> timeDistMap["\u6DF1\u591C"] = (timeDistMap["\u6DF1\u591C"] ?: 0) + 1
                }
            }

            val uniqueWords = allWords.toSet().size
            val avgSentenceLength = if (totalSentences > 0) totalChars.toFloat() / totalSentences else 0f
            val vocabRichness = if (allWords.isNotEmpty()) uniqueWords.toFloat() / allWords.size else 0f
            val avgWordLength = if (allWords.isNotEmpty()) allWords.sumOf { it.length }.toFloat() / allWords.size else 0f
            val punctRatio = if (totalChars > 0) totalPunctuation.toFloat() / totalChars else 0f
            val avgParaLength = if (totalParagraphs > 0) totalChars.toFloat() / totalParagraphs else 0f

            val scoreSentence = (avgSentenceLength / 40f).coerceIn(0f, 1f) * 25f
            val scoreVocabulary = vocabRichness.coerceIn(0f, 1f) * 25f
            val scoreWordLength = (avgWordLength / 3f).coerceIn(0f, 1f) * 15f
            val scorePunct = (punctRatio * 10f).coerceIn(0f, 1f) * 10f
            val scoreEntries = (entries.size.toFloat() / 50f).coerceIn(0f, 1f) * 25f
            val creativityScore = (scoreSentence + scoreVocabulary + scoreWordLength + scorePunct + scoreEntries).toInt().coerceIn(0, 100)

            val styleLabel = when {
                avgSentenceLength > 30 && vocabRichness > 0.6f -> "\u7EC6\u817B\u4E30\u5BCC"
                avgSentenceLength > 30 -> "\u6C89\u7A33\u8BE6\u5C3D"
                avgSentenceLength < 15 && vocabRichness > 0.6f -> "\u7B80\u6D01\u7075\u52A8"
                avgSentenceLength < 15 -> "\u7B80\u6D01\u76F4\u63A5"
                vocabRichness > 0.7f -> "\u8BCD\u6C47\u4E30\u5BCC"
                punctRatio > 0.05f -> "\u60C5\u611F\u5145\u6C9B"
                else -> "\u7A33\u5B9A\u5747\u8861"
            }

            val evolutionNote = "\u57FA\u4E8E${entries.size}\u7BC7\u65E5\u8BB0\u5206\u6790\uFF0C\u4F60\u7684\u5199\u4F5C\u98CE\u683C\u4EE5\u300C$styleLabel\u300D\u4E3A\u4E3B\u3002" +
                    "\u5E73\u5747\u53E5\u957F${"\uFEFF" }${"\uFEFF" }${"%.1f".format(avgSentenceLength)}\u5B57\uFF0C\u7528\u8BCD\u4E30\u5BCC\u5EA6${"\uFEFF" }${"\uFEFF" }${"%.0f".format(vocabRichness * 100)}%\u3002"

            val comparison = if (prevEntries.isNotEmpty()) {
                var pTotalSentences = 0
                var pTotalChars = 0
                val pAllWords = mutableListOf<String>()
                prevEntries.forEach { entry ->
                    val text = entry.plainText
                    if (text.isBlank()) return@forEach
                    pTotalChars += text.length
                    val sentences = text.split(Regex("[.!?\u3002\uFF01\uFF1F\\n]+")).filter { it.isNotBlank() }
                    pTotalSentences += sentences.size
                    val words = extractChineseWords(text)
                    pAllWords.addAll(words)
                }
                val pAvgSentence = if (pTotalSentences > 0) pTotalChars.toFloat() / pTotalSentences else 0f
                val pVocabRich = if (pAllWords.isNotEmpty()) pAllWords.toSet().size.toFloat() / pAllWords.size else 0f
                ComparisonData(
                    avgSentenceLengthChange = avgSentenceLength - pAvgSentence,
                    vocabularyRichnessChange = vocabRichness - pVocabRich,
                    totalWordsChange = totalChars - pTotalChars,
                    totalEntriesChange = entries.size - prevEntries.size
                )
            } else null

            val dailyWordCounts = dailyWordMap.entries
                .map { (it.key * 86400000L) to it.value }
                .sortedBy { it.first }
                .take(30)

            _analysis.value = FingerprintAnalysis(
                avgSentenceLength = avgSentenceLength,
                vocabularyRichness = vocabRichness,
                avgWordLength = avgWordLength,
                punctuationRatio = punctRatio,
                paragraphCount = totalParagraphs,
                avgParagraphLength = avgParaLength,
                totalEntries = entries.size,
                styleLabel = styleLabel,
                evolutionNote = evolutionNote,
                dailyWordCounts = dailyWordCounts,
                timeDistribution = timeDistMap,
                creativityScore = creativityScore,
                comparison = comparison,
                timeRange = range
            )
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
