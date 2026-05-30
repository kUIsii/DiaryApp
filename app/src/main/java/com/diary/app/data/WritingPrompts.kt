package com.diary.app.data

import java.time.LocalDate
import java.time.Month

/**
 * Writing prompts to inspire diary entries.
 * Organized by category and seasonal relevance.
 */
object WritingPrompts {

    private val dailyPrompts = listOf(
        "今天最让你感恩的一件事是什么？",
        "如果今天可以重来，你会改变什么？",
        "今天学到了什么新东西？",
        "描述一个今天让你微笑的瞬间。",
        "今天有什么让你感到自豪的事？",
        "如果用三个词形容今天，你会选什么？",
        "今天最让你意外的事情是什么？",
        "给未来的自己写一句话。",
        "今天你如何照顾了自己？",
        "今天有没有什么小小的快乐？",
        "写下今天听到的一句有意思的话。",
        "今天你最期待的是什么？",
        "描述今天的天气，它让你有什么感觉？",
        "今天有没有帮助过谁？",
        "如果今天是一首歌，会是哪首？"
    )

    private val seasonalPrompts = mapOf(
        // Spring (March-May)
        Month.MARCH to listOf(
            "春天来了，你注意到哪些变化？",
            "新的季节，新的开始。写下你的春日愿望。"
        ),
        Month.APRIL to listOf(
            "四月的雨让你想起什么？",
            "描述一个春天的气味。"
        ),
        Month.MAY to listOf(
            "五月的阳光下，你在想什么？",
            "这个春天，你收获了什么？"
        ),
        // Summer (June-August)
        Month.JUNE to listOf(
            "夏天的第一个记忆是什么？",
            "描述今天的晚霞。"
        ),
        Month.JULY to listOf(
            "炎热的天气里，什么让你感到清凉？",
            "写下夏日里的一段对话。"
        ),
        Month.AUGUST to listOf(
            "八月的夜晚，你在想什么？",
            "这个夏天最难忘的瞬间。"
        ),
        // Autumn (September-November)
        Month.SEPTEMBER to listOf(
            "秋天的第一片落叶，你注意到了吗？",
            "新学期/新开始的感觉如何？"
        ),
        Month.OCTOBER to listOf(
            "十月的颜色是什么？",
            "描述一个温暖的秋日午后。"
        ),
        Month.NOVEMBER to listOf(
            "感恩节快到了，你想感谢谁？",
            "秋天的味道是什么？"
        ),
        // Winter (December-February)
        Month.DECEMBER to listOf(
            "回顾这一年，你最大的成长是什么？",
            "冬日的温暖来自哪里？"
        ),
        Month.JANUARY to listOf(
            "新的一年，你对自己有什么期待？",
            "一月的清晨，写下你的第一想法。"
        ),
        Month.FEBRUARY to listOf(
            "冬末春初，你感受到什么变化？",
            "寒冷的天气里，什么让你感到温暖？"
        )
    )

    private val moodBasedPrompts = mapOf(
        1 to listOf(  // Very sad
            "即使在低落的日子里，也值得被记录。写下你的感受。",
            "允许自己悲伤。今天发生了什么？",
            "困难的时刻也会过去。写下此刻的心情。"
        ),
        2 to listOf(  // Sad
            "不开心的日子也需要被看见。写下今天。",
            "给自己一个温柔的拥抱。你在想什么？",
            "低落的时候，写下感受本身就是一种疗愈。"
        ),
        3 to listOf(  // Neutral
            "平静的一天也有值得记录的细节。",
            "平凡的日子里，有什么细微的变化？",
            "今天有什么让你感到平静的事？"
        ),
        4 to listOf(  // Happy
            "开心的时刻值得被珍藏。写下今天的快乐。",
            "分享你的喜悦！今天发生了什么好事？",
            "快乐是会传染的，记录下这份美好。"
        ),
        5 to listOf(  // Very happy
            "这么美好的一天！详细记录下来吧。",
            "幸福的瞬间要用心铭记。今天为何如此开心？",
            "把这份快乐写下来，未来的你会感谢现在的自己。"
        ),
        6 to listOf(  // Excited
            "兴奋的时刻！快写下这份激动的心情！",
            "是什么让你如此兴奋？详细记录下来！",
            "这份热情和激动，值得被好好保存。"
        )
    )

    /**
     * Get a random writing prompt based on current date and optional mood.
     * Uses date as seed for consistency within the same day.
     */
    fun getPrompt(moodLevel: Int? = null): String {
        val today = LocalDate.now()
        val dayOfYear = today.dayOfYear

        // If mood is provided, 50% chance to show mood-based prompt
        if (moodLevel != null && dayOfYear % 2 == 0) {
            val moodPrompts = moodBasedPrompts[moodLevel] ?: dailyPrompts
            return moodPrompts[dayOfYear % moodPrompts.size]
        }

        // 30% chance to show seasonal prompt
        if (dayOfYear % 10 < 3) {
            val month = today.month
            val seasonal = seasonalPrompts[month] ?: dailyPrompts
            return seasonal[dayOfYear % seasonal.size]
        }

        // Otherwise show daily prompt
        return dailyPrompts[dayOfYear % dailyPrompts.size]
    }

    /**
     * Get a fresh random prompt (different each time).
     */
    fun getRandomPrompt(): String {
        return dailyPrompts.random()
    }
}
