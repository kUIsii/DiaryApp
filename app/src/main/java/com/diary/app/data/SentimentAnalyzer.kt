package com.diary.app.data

/**
 * 简单的情感分析器
 * 基于关键词匹配，判断文本的情感倾向
 */
object SentimentAnalyzer {

    // 积极词汇
    private val positiveWords = listOf(
        "开心", "快乐", "高兴", "喜欢", "爱", "幸福", "美好", "愉快",
        "满意", "满足", "感恩", "感谢", "兴奋", "期待", "希望", "成功",
        "进步", "突破", "收获", "成长", "温暖", "感动", "惊喜", "庆祝"
    )

    // 消极词汇
    private val negativeWords = listOf(
        "难过", "伤心", "失望", "沮丧", "焦虑", "紧张", "担心", "害怕",
        "愤怒", "生气", "烦躁", "无聊", "孤独", "寂寞", "压力", "疲惫",
        "累", "糟糕", "失败", "挫折", "痛苦", "悲伤", "忧郁", "郁闷"
    )

    // 中性词汇
    private val neutralWords = listOf(
        "普通", "一般", "平常", "正常", "平常", "日常", "平淡", "平静"
    )

    /**
     * 分析文本情感
     * 返回 -1.0（极度消极）到 1.0（极度积极）
     */
    fun analyze(plainText: String): Float {
        if (plainText.isBlank()) return 0f

        val text = plainText.lowercase()
        val positiveCount = positiveWords.count { text.contains(it) }
        val negativeCount = negativeWords.count { text.contains(it) }

        val total = positiveCount + negativeCount
        if (total == 0) return 0f

        return (positiveCount - negativeCount).toFloat() / total
    }

    /**
     * 获取情感标签
     */
    fun getSentimentLabel(plainText: String): SentimentLabel {
        val score = analyze(plainText)
        return when {
            score > 0.3f -> SentimentLabel.POSITIVE
            score < -0.3f -> SentimentLabel.NEGATIVE
            else -> SentimentLabel.NEUTRAL
        }
    }

    /**
     * 检测是否包含压力相关内容
     */
    fun hasStressContent(plainText: String): Boolean {
        val stressWords = listOf(
            "压力", "焦虑", "紧张", "担心", "害怕", "恐惧",
            "崩溃", "失控", "绝望", "无助", "烦躁", "愤怒"
        )
        val text = plainText.lowercase()
        return stressWords.any { text.contains(it) }
    }

    /**
     * 检测是否包含新话题
     */
    fun isNewTopic(plainText: String, recentTopics: List<String> = emptyList()): Boolean {
        val text = plainText.lowercase()
        // 简单检测：如果包含新出现的关键词
        val topicKeywords = listOf(
            "旅行", "美食", "运动", "读书", "电影", "音乐",
            "工作", "学习", "生活", "家庭", "朋友", "爱情"
        )
        return topicKeywords.any { keyword ->
            text.contains(keyword) && !recentTopics.any { it.contains(keyword) }
        }
    }

    /**
     * 检测是否是深夜内容
     */
    fun isLateNight(createdAt: Long): Boolean {
        val hour = java.time.Instant.ofEpochMilli(createdAt)
            .atZone(java.time.ZoneId.systemDefault()).hour
        return hour == 23 || hour in 0..2
    }
}

/**
 * 情感标签
 */
enum class SentimentLabel {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}
