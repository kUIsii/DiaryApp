package com.diary.app.ui.textmicroscope

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TextAnalysis(
    val totalChars: Int = 0,
    val totalWords: Int = 0,
    val totalSentences: Int = 0,
    val totalParagraphs: Int = 0,
    val avgSentenceLength: Float = 0f,
    val avgWordLength: Float = 0f,
    val vocabularyRichness: Float = 0f,  // 不重复词/总词数
    val punctuationRatio: Float = 0f,     // 标点数/总字符数
    val topWords: List<Pair<String, Int>> = emptyList(),
    val sentenceLengths: List<Int> = emptyList()
)

class TextMicroscopeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    
    private val _analysis = MutableStateFlow<TextAnalysis?>(null)
    val analysis: StateFlow<TextAnalysis?> = _analysis
    
    private val _allEntriesAnalysis = MutableStateFlow<List<Pair<Long, TextAnalysis>>>(emptyList())
    val allEntriesAnalysis: StateFlow<List<Pair<Long, TextAnalysis>>> = _allEntriesAnalysis
    
    fun analyzeAllEntries() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            val allPlainText = entries.joinToString("\n") { it.plainText }
            _analysis.value = analyzeText(allPlainText)
            
            _allEntriesAnalysis.value = entries.take(50).map { entry ->
                entry.id to analyzeText(entry.plainText)
            }
        }
    }
    
    companion object {
        fun analyzeText(text: String): TextAnalysis {
            if (text.isBlank()) return TextAnalysis()
            
            val chars = text.length
            val sentences = text.split(Regex("[.!?。！？\\n]+")).filter { it.isNotBlank() }
            val paragraphs = text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
            
            // 中文分词：按字符和简单规则
            val words = tokenizeChinese(text)
            val uniqueWords = words.toSet()
            
            // 高频词
            val wordFreq = words.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(10)
                .map { it.key to it.value }
            
            // 标点统计
            val punctuationCount = text.count { it in "\uFF0C\u3002\uFF01\uFF1F\u3001\uFF1B\uFF1A\u201C\u201D\u2018\u2019\uFF08\uFF09\u3010\u3011\u300A\u300B,.!?;:" }
            
            // 每句长度
            val sentenceLengths = sentences.map { it.length }
            
            return TextAnalysis(
                totalChars = chars,
                totalWords = words.size,
                totalSentences = sentences.size,
                totalParagraphs = paragraphs.size,
                avgSentenceLength = if (sentences.isNotEmpty()) sentences.map { it.length }.average().toFloat() else 0f,
                avgWordLength = if (words.isNotEmpty()) words.map { it.length }.average().toFloat() else 0f,
                vocabularyRichness = if (words.isNotEmpty()) uniqueWords.size.toFloat() / words.size else 0f,
                punctuationRatio = if (chars > 0) punctuationCount.toFloat() / chars else 0f,
                topWords = wordFreq,
                sentenceLengths = sentenceLengths
            )
        }
        
        private fun tokenizeChinese(text: String): List<String> {
            // 简单的中文分词：按标点和空格分割，然后按2-3字窗口滑动
            // 实际项目中应使用结巴分词等
            val cleaned = text.replace(Regex("[\uFF0C\u3002\uFF01\uFF1F\u3001\uFF1B\uFF1A\u201C\u201D\u2018\u2019\uFF08\uFF09\u3010\u3011\u300A\u300B\\n\\r\\t]"), " ")
            val tokens = mutableListOf<String>()
            
            // 提取连续的中文词组（2-4字）
            val chineseSegments = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
            for (segment in chineseSegments) {
                if (segment.all { it in '\u4e00'..'\u9fff' }) {
                    // 纯中文，按2字窗口切分
                    for (i in 0 until segment.length - 1) {
                        tokens.add(segment.substring(i, i + 2))
                    }
                    if (segment.length >= 3) {
                        for (i in 0 until segment.length - 2) {
                            tokens.add(segment.substring(i, i + 3))
                        }
                    }
                } else {
                    tokens.add(segment.lowercase())
                }
            }
            
            // 过滤停用词
            val stopWords = setOf("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这")
            return tokens.filter { it !in stopWords && it.length >= 2 }
        }
    }
}
