package com.diary.app.data

/**
 * 写作教练 - 分析写作习惯并给出建议
 */
object WritingCoach {
    
    data class WritingAnalysis(
        val totalEntries: Int = 0,
        val avgWordCount: Float = 0f,
        val avgSentenceLength: Float = 0f,
        val vocabularyRichness: Float = 0f,
        val topRepeatedWords: List<Pair<String, Int>> = emptyList(),
        val emotionDistribution: Map<String, Int> = emptyMap(),
        val writingTimePattern: String = "",
        val suggestions: List<String> = emptyList()
    )
    
    /**
     * 分析多篇日记的写作模式
     */
    fun analyzeWritingPatterns(entries: List<DiaryEntry>): WritingAnalysis {
        if (entries.isEmpty()) return WritingAnalysis()
        
        val allText = entries.joinToString(" ") { it.plainText }
        val wordCounts = entries.map { it.plainText.split(Regex("\\s+")).size }
        val avgWordCount = wordCounts.average().toFloat()
        
        // 分析句子长度
        val sentences = allText.split(Regex("[.!?。！？\\n]+")).filter { it.isNotBlank() }
        val avgSentenceLength = if (sentences.isNotEmpty()) {
            sentences.map { it.length }.average().toFloat()
        } else 0f
        
        // 词汇丰富度
        val words = tokenizeSimple(allText)
        val uniqueWords = words.toSet()
        val vocabularyRichness = if (words.isNotEmpty()) {
            uniqueWords.size.toFloat() / words.size
        } else 0f
        
        // 高频重复词（排除停用词）
        val stopWords = setOf("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "他", "她", "它", "们")
        val wordFreq = words.filter { it !in stopWords && it.length >= 2 }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }
        
        // 情绪分布
        val emotionDistribution = entries.mapNotNull { it.moodLevel }
            .groupingBy { it }.eachCount()
            .mapKeys { moodLevelToLabel(it.key) }
        
        // 写作时间模式
        val writingTimePattern = analyzeWritingTimePattern(entries)
        
        // 生成建议
        val suggestions = generateSuggestions(
            avgWordCount = avgWordCount,
            avgSentenceLength = avgSentenceLength,
            vocabularyRichness = vocabularyRichness,
            topRepeatedWords = wordFreq,
            writingTimePattern = writingTimePattern
        )
        
        return WritingAnalysis(
            totalEntries = entries.size,
            avgWordCount = avgWordCount,
            avgSentenceLength = avgSentenceLength,
            vocabularyRichness = vocabularyRichness,
            topRepeatedWords = wordFreq,
            emotionDistribution = emotionDistribution,
            writingTimePattern = writingTimePattern,
            suggestions = suggestions
        )
    }
    
    private fun tokenizeSimple(text: String): List<String> {
        val cleaned = text.replace(Regex("[\\p{Punct}\\p{IsPunctuation}]"), " ")
        return cleaned.split(Regex("\\s+"))
            .filter { it.length >= 2 }
    }
    
    private fun moodLevelToLabel(level: Int): String {
        return when (level) {
            1 -> "很低落"
            2 -> "有些低落"
            3 -> "平静"
            4 -> "不错"
            5 -> "很开心"
            6 -> "非常兴奋"
            else -> "未知"
        }
    }
    
    private fun analyzeWritingTimePattern(entries: List<DiaryEntry>): String {
        if (entries.isEmpty()) return ""
        
        val hours = entries.map { 
            java.time.Instant.ofEpochMilli(it.createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .hour 
        }
        
        val morning = hours.count { it in 5..11 }
        val afternoon = hours.count { it in 12..17 }
        val evening = hours.count { it in 18..22 }
        val night = hours.count { it in 23..4 || it in 0..4 }
        
        val total = hours.size
        return when {
            morning > total * 0.5 -> "晨间写作者"
            afternoon > total * 0.5 -> "午后写作者"
            evening > total * 0.5 -> "晚间写作者"
            night > total * 0.3 -> "深夜写作者"
            else -> "全时段写作者"
        }
    }
    
    private fun generateSuggestions(
        avgWordCount: Float,
        avgSentenceLength: Float,
        vocabularyRichness: Float,
        topRepeatedWords: List<Pair<String, Int>>,
        writingTimePattern: String
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 字数建议
        if (avgWordCount < 50) {
            suggestions.add("尝试写得更长一些，50-200字能更好地表达想法")
        } else if (avgWordCount > 500) {
            suggestions.add("你的日记很详细，偶尔尝试精简表达也是不错的练习")
        }
        
        // 句长建议
        if (avgSentenceLength > 30) {
            suggestions.add("句子偏长，尝试用短句增加节奏感")
        } else if (avgSentenceLength < 10) {
            suggestions.add("句子偏短，可以尝试用长句表达更复杂的想法")
        }
        
        // 词汇丰富度建议
        if (vocabularyRichness < 0.3) {
            suggestions.add("用词重复较多，尝试用不同的词表达相似的意思")
        }
        
        // 重复词建议
        if (topRepeatedWords.isNotEmpty()) {
            val (word, count) = topRepeatedWords.first()
            if (count > 10) {
                suggestions.add("「$word」出现了${count}次，可以用同义词替换增加变化")
            }
        }
        
        // 写作时间建议
        if (writingTimePattern == "深夜写作者") {
            suggestions.add("你常在深夜写作，注意保护眼睛和休息")
        }
        
        // 如果没有特别建议，给一个通用鼓励
        if (suggestions.isEmpty()) {
            suggestions.add("你的写作习惯很健康，继续保持！")
        }
        
        return suggestions.take(5) // 最多5条建议
    }
}
