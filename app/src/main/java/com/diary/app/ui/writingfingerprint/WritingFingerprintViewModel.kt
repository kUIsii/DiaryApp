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

data class FingerprintAnalysis(
    val avgSentenceLength: Float,
    val vocabularyRichness: Float,
    val avgWordLength: Float,
    val punctuationRatio: Float,
    val paragraphCount: Int,
    val avgParagraphLength: Float,
    val totalEntries: Int,
    val styleLabel: String,
    val evolutionNote: String
)

class WritingFingerprintViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _analysis = MutableStateFlow<FingerprintAnalysis?>(null)
    val analysis: StateFlow<FingerprintAnalysis?> = _analysis.asStateFlow()

    private val _fingerprints = MutableStateFlow<List<WritingFingerprint>>(emptyList())
    val fingerprints: StateFlow<List<WritingFingerprint>> = _fingerprints.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            if (entries.isEmpty()) {
                _analysis.value = null
                return@launch
            }

            var totalSentences = 0
            var totalChars = 0
            val allWords = mutableListOf<String>()
            var totalPunctuation = 0
            var totalParagraphs = 0

            entries.forEach { entry ->
                val text = entry.plainText
                if (text.isBlank()) return@forEach
                totalChars += text.length
                val sentences = text.split(Regex("[.!?。！？\\n]+")).filter { it.isNotBlank() }
                totalSentences += sentences.size
                val words = extractChineseWords(text)
                allWords.addAll(words)
                totalPunctuation += text.count { it in "，,。.！？!?、；;" }
                val paragraphs = text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
                if (paragraphs.isEmpty()) {
                    val singleParagraphs = text.split("\n").filter { it.isNotBlank() }
                    totalParagraphs += singleParagraphs.size
                } else {
                    totalParagraphs += paragraphs.size
                }
            }

            val uniqueWords = allWords.toSet().size
            val avgSentenceLength = if (totalSentences > 0) totalChars.toFloat() / totalSentences else 0f
            val vocabRichness = if (allWords.isNotEmpty()) uniqueWords.toFloat() / allWords.size else 0f
            val avgWordLength = if (allWords.isNotEmpty()) allWords.sumOf { it.length }.toFloat() / allWords.size else 0f
            val punctRatio = if (totalChars > 0) totalPunctuation.toFloat() / totalChars else 0f
            val avgParaLength = if (totalParagraphs > 0) totalChars.toFloat() / totalParagraphs else 0f

            val styleLabel = when {
                avgSentenceLength > 30 && vocabRichness > 0.6f -> "细腻丰富"
                avgSentenceLength > 30 -> "沉稳详尽"
                avgSentenceLength < 15 && vocabRichness > 0.6f -> "简洁灵动"
                avgSentenceLength < 15 -> "简洁直接"
                vocabRichness > 0.7f -> "词汇丰富"
                punctRatio > 0.05f -> "情感充沛"
                else -> "稳定均衡"
            }

            val evolutionNote = "基于${entries.size}篇日记分析，你的写作风格以「$styleLabel」为主。" +
                    "平均句长${"%.1f".format(avgSentenceLength)}字，用词丰富度${"%.0f".format(vocabRichness * 100)}%。"

            _analysis.value = FingerprintAnalysis(
                avgSentenceLength = avgSentenceLength,
                vocabularyRichness = vocabRichness,
                avgWordLength = avgWordLength,
                punctuationRatio = punctRatio,
                paragraphCount = totalParagraphs,
                avgParagraphLength = avgParaLength,
                totalEntries = entries.size,
                styleLabel = styleLabel,
                evolutionNote = evolutionNote
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
