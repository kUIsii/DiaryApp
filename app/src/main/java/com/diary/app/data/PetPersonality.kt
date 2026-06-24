package com.diary.app.data

/**
 * 宠物性格形成器
 * 基于日记内容分析，逐步形成宠物性格
 */
object PetPersonalityAnalyzer {

    // 关键词词典
    private val keywordMap = mapOf(
        // 外向性
        "extraversion" to listOf(
            "聚会", "朋友", "社交", "聊天", "一起", "大家",
            "派对", "活动", "认识", "交流", "分享", "热闹"
        ),

        // 开放性
        "openness" to listOf(
            "学习", "尝试", "新", "发现", "探索", "好奇",
            "创造", "想象", "思考", "理解", "领悟", "灵感"
        ),

        // 尽责性
        "conscientiousness" to listOf(
            "计划", "完成", "目标", "任务", "安排", "坚持",
            "规律", "习惯", "效率", "进步", "提升", "规划"
        ),

        // 宜人性
        "agreeableness" to listOf(
            "帮助", "感谢", "包容", "理解", "关心", "陪伴",
            "温柔", "善良", "体谅", "支持", "鼓励", "信任"
        ),

        // 情绪稳定性
        "emotional_stability" to listOf(
            "平静", "宁静", "放松", "安心", "舒适", "满足",
            "稳定", "从容", "淡定", "坦然", "释然", "平和"
        )
    )

    // 焦虑词汇（降低情绪稳定性）
    private val anxietyWords = listOf(
        "焦虑", "紧张", "担心", "害怕", "恐惧", "不安",
        "压力", "烦躁", "崩溃", "失控", "绝望", "无助"
    )

    // 每次匹配的微调幅度
    private val adjustmentRate = 0.02f

    /**
     * 根据日记内容更新性格
     * 返回更新后的性格
     */
    fun updatePersonality(
        current: PetPersonality,
        plainText: String
    ): PetPersonality {
        val text = plainText.lowercase()

        // 计算各维度的变化
        var extraversionDelta = 0f
        var opennessDelta = 0f
        var conscientiousnessDelta = 0f
        var agreeablenessDelta = 0f
        var stabilityDelta = 0f

        // 匹配关键词
        keywordMap["extraversion"]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            extraversionDelta += matches * adjustmentRate
        }

        keywordMap["openness"]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            opennessDelta += matches * adjustmentRate
        }

        keywordMap["conscientiousness"]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            conscientiousnessDelta += matches * adjustmentRate
        }

        keywordMap["agreeableness"]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            agreeablenessDelta += matches * adjustmentRate
        }

        keywordMap["emotional_stability"]?.let { keywords ->
            val matches = keywords.count { text.contains(it) }
            stabilityDelta += matches * adjustmentRate
        }

        // 焦虑词汇降低情绪稳定性
        val anxietyMatches = anxietyWords.count { text.contains(it) }
        stabilityDelta -= anxietyMatches * adjustmentRate * 1.5f

        // 限制单次变化幅度
        val maxDelta = 0.1f
        extraversionDelta = extraversionDelta.coerceIn(-maxDelta, maxDelta)
        opennessDelta = opennessDelta.coerceIn(-maxDelta, maxDelta)
        conscientiousnessDelta = conscientiousnessDelta.coerceIn(-maxDelta, maxDelta)
        agreeablenessDelta = agreeablenessDelta.coerceIn(-maxDelta, maxDelta)
        stabilityDelta = stabilityDelta.coerceIn(-maxDelta, maxDelta)

        // 应用变化，限制在0.0~1.0范围
        return current.copy(
            extraversion = (current.extraversion + extraversionDelta).coerceIn(0f, 1f),
            openness = (current.openness + opennessDelta).coerceIn(0f, 1f),
            conscientiousness = (current.conscientiousness + conscientiousnessDelta).coerceIn(0f, 1f),
            agreeableness = (current.agreeableness + agreeablenessDelta).coerceIn(0f, 1f),
            emotionalStability = (current.emotionalStability + stabilityDelta).coerceIn(0f, 1f),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 获取性格的主要特征
     */
    fun getDominantTrait(personality: PetPersonality): String {
        val traits = mapOf(
            "外向活泼" to personality.extraversion,
            "开放好奇" to personality.openness,
            "认真尽责" to personality.conscientiousness,
            "温柔善良" to personality.agreeableness,
            "平静稳定" to personality.emotionalStability
        )
        return traits.maxByOrNull { it.value }?.key ?: "平静稳定"
    }

    /**
     * 根据性格获取反馈风格
     */
    fun getFeedbackStyle(personality: PetPersonality): FeedbackStyle {
        val dominant = getDominantTrait(personality)
        return when (dominant) {
            "外向活泼" -> FeedbackStyle.LIVELY
            "开放好奇" -> FeedbackStyle.CURIOUS
            "认真尽责" -> FeedbackStyle.ENCOURAGING
            "温柔善良" -> FeedbackStyle.WARM
            else -> FeedbackStyle.CALM
        }
    }
}

/**
 * 反馈风格
 */
enum class FeedbackStyle {
    LIVELY,      // 活泼型
    CURIOUS,     // 好奇型
    ENCOURAGING, // 鼓励型
    WARM,        // 温暖型
    CALM         // 平静型
}
