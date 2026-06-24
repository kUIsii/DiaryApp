package com.diary.app.data

/**
 * 心情到环境的映射器
 * 根据日记心情等级更新小岛环境维度
 */
object MoodEnvironmentMapper {

    /**
     * 根据心情等级获取环境变化
     * 返回 Pair(环境变化, 经验值)
     */
    fun mapMoodToEnvironment(
        moodLevel: Int?,
        currentEnvironment: IslandEnvironment,
        weather: String? = null,
        plainText: String = ""
    ): Pair<EnvironmentDelta, Int> {
        val mood = moodLevel ?: 3  // 默认平静

        // 基础环境变化
        val baseDelta = when (mood) {
            1 -> EnvironmentDelta(     // 沮丧
                lushness = -0.05f,
                brightness = -0.08f,
                tranquility = -0.03f,
                warmth = -0.06f
            )
            2 -> EnvironmentDelta(     // 低落
                lushness = -0.02f,
                brightness = -0.04f,
                tranquility = -0.01f,
                warmth = -0.03f
            )
            3 -> EnvironmentDelta(     // 平静
                lushness = 0.01f,
                brightness = 0.01f,
                tranquility = 0.03f,
                warmth = 0.01f
            )
            4 -> EnvironmentDelta(     // 开心
                lushness = 0.03f,
                brightness = 0.04f,
                tranquility = 0.02f,
                warmth = 0.03f
            )
            5 -> EnvironmentDelta(     // 愉快
                lushness = 0.05f,
                brightness = 0.06f,
                tranquility = 0.03f,
                warmth = 0.05f
            )
            6 -> EnvironmentDelta(     // 兴奋
                lushness = 0.08f,
                brightness = 0.10f,
                tranquility = 0.02f,
                warmth = 0.08f
            )
            else -> EnvironmentDelta(0f, 0f, 0f, 0f)
        }

        // 天气影响
        val weatherDelta = when (weather) {
            "晴天" -> EnvironmentDelta(0.02f, 0.03f, 0.01f, 0.02f)
            "多云" -> EnvironmentDelta(0.01f, 0.01f, 0.02f, 0.01f)
            "阴天" -> EnvironmentDelta(0f, -0.02f, 0.03f, -0.01f)
            "雨天" -> EnvironmentDelta(-0.01f, -0.03f, 0.04f, -0.02f)
            "雷暴" -> EnvironmentDelta(-0.03f, -0.05f, -0.02f, -0.03f)
            "大风" -> EnvironmentDelta(-0.02f, -0.01f, -0.04f, -0.01f)
            else -> EnvironmentDelta(0f, 0f, 0f, 0f)
        }

        // 文字内容影响
        val textDelta = analyzeTextForEnvironment(plainText)

        // 合并所有变化
        val totalDelta = EnvironmentDelta(
            lushness = (baseDelta.lushness + weatherDelta.lushness + textDelta.lushness).coerceIn(-0.15f, 0.15f),
            brightness = (baseDelta.brightness + weatherDelta.brightness + textDelta.brightness).coerceIn(-0.15f, 0.15f),
            tranquility = (baseDelta.tranquility + weatherDelta.tranquility + textDelta.tranquility).coerceIn(-0.15f, 0.15f),
            warmth = (baseDelta.warmth + weatherDelta.warmth + textDelta.warmth).coerceIn(-0.15f, 0.15f)
        )

        // 计算经验值
        val baseExp = 10
        val moodExp = when (mood) {
            1, 2 -> 5
            3 -> 3
            4, 5, 6 -> 5
            else -> 3
        }
        val weatherExp = if (weather != null) 3 else 0
        val textExp = calculateTextExperience(plainText)

        return Pair(totalDelta, baseExp + moodExp + weatherExp + textExp)
    }

    /**
     * 分析文字内容对环境的影响
     */
    private fun analyzeTextForEnvironment(plainText: String): EnvironmentDelta {
        val text = plainText.lowercase()

        // 自然相关词汇增加茂盛度
        val natureWords = listOf("花", "树", "草", "鸟", "森林", "海洋", "山", "河")
        val natureCount = natureWords.count { text.contains(it) }

        // 平静相关词汇增加宁静度
        val calmWords = listOf("平静", "宁静", "安静", "放松", "舒适", "平和")
        val calmCount = calmWords.count { text.contains(it) }

        // 温暖相关词汇增加温暖度
        val warmWords = listOf("温暖", "爱", "拥抱", "家人", "朋友", "感谢")
        val warmCount = warmWords.count { text.contains(it) }

        // 光明相关词汇增加明亮度
        val brightWords = listOf("阳光", "光明", "希望", "梦想", "未来", "晴朗")
        val brightCount = brightWords.count { text.contains(it) }

        return EnvironmentDelta(
            lushness = (natureCount * 0.01f).coerceIn(0f, 0.05f),
            brightness = (brightCount * 0.01f).coerceIn(0f, 0.05f),
            tranquility = (calmCount * 0.01f).coerceIn(0f, 0.05f),
            warmth = (warmCount * 0.01f).coerceIn(0f, 0.05f)
        )
    }

    /**
     * 根据文字内容计算额外经验值
     */
    private fun calculateTextExperience(plainText: String): Int {
        val length = plainText.length
        val baseExp = length / 100  // 每100字1经验
        return baseExp.coerceAtMost(20)  // 最多20经验
    }

    /**
     * 计算连续记录倍率
     */
    fun getStreakMultiplier(streakDays: Int): Float {
        return when {
            streakDays >= 30 -> 2.0f
            streakDays >= 14 -> 1.5f
            streakDays >= 7 -> 1.3f
            streakDays >= 3 -> 1.1f
            else -> 1.0f
        }
    }

    /**
     * 计算升级所需经验值
     */
    fun getExperienceForLevel(level: Int): Int {
        return 100 * level + 50 * level * (level - 1) / 2
    }
}

/**
 * 环境维度变化量
 */
data class EnvironmentDelta(
    val lushness: Float,
    val brightness: Float,
    val tranquility: Float,
    val warmth: Float
)
