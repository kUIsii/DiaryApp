package com.diary.app.ai

import android.content.Context
import com.diary.app.data.DiaryDao
import com.diary.app.data.NotificationEntity
import com.diary.app.ui.components.formatWordCountWithUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object MilestoneChecker {

    private const val PREFS_NAME = "milestone_prefs"
    private const val KEY_LAST_STREAK_MILESTONE = "last_streak_milestone"
    private const val KEY_LAST_WORD_MILESTONE = "last_word_milestone"

    private val STREAK_MILESTONES = listOf(3, 7, 14, 30, 50, 100, 200, 365)
    private val WORD_MILESTONES = listOf(1000, 5000, 10000, 30000, 50000, 100000, 200000, 500000)

    suspend fun checkAndNotify(context: Context, dao: DiaryDao) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val timestamps = dao.getAllEntriesOnce().map { it.createdAt }
        if (timestamps.isEmpty()) return

        val dates = timestamps.map {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()

        val streak = computeStreak(dates)
        val lastStreakMilestone = prefs.getInt(KEY_LAST_STREAK_MILESTONE, 0)
        val newStreakMilestone = STREAK_MILESTONES.lastOrNull { it <= streak && it > lastStreakMilestone }

        if (newStreakMilestone != null) {
            dao.insertNotification(
                NotificationEntity(
                    id = "streak_$newStreakMilestone",
                    type = "streak",
                    title = "连续写作 $newStreakMilestone 天",
                    subtitle = getStreakSubtitle(newStreakMilestone),
                    iconType = "streak",
                    colorHex = 0xFFFF6B35,
                    relatedId = null
                )
            )
            prefs.edit().putInt(KEY_LAST_STREAK_MILESTONE, newStreakMilestone).apply()
        }

        // Check word count milestones
        val totalWords = dao.getAllEntriesOnce().sumOf { it.plainText.length }
        val lastWordMilestone = prefs.getInt(KEY_LAST_WORD_MILESTONE, 0)
        val newWordMilestone = WORD_MILESTONES.lastOrNull { it <= totalWords && it > lastWordMilestone }

        if (newWordMilestone != null) {
            dao.insertNotification(
                NotificationEntity(
                    id = "words_$newWordMilestone",
                    type = "milestone",
                    title = "累计写作 ${formatWordCountWithUnit(newWordMilestone)}",
                    subtitle = getWordSubtitle(newWordMilestone),
                    iconType = "milestone",
                    colorHex = 0xFF667EEA,
                    relatedId = null
                )
            )
            prefs.edit().putInt(KEY_LAST_WORD_MILESTONE, newWordMilestone).apply()
        }
    }

    private fun computeStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        var current = dates.maxOrNull() ?: return 0
        val today = LocalDate.now()
        if (current.isAfter(today)) return 0
        while (current in dates) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    private fun getStreakSubtitle(days: Int): String = when (days) {
        3 -> "三天打鱼的阶段已过，继续加油"
        7 -> "一周不间断，写作正在成为习惯"
        14 -> "两周坚持，已经很了不起了"
        30 -> "一个月的坚持，写作已融入日常"
        50 -> "五十天如一日，这份毅力值得敬佩"
        100 -> "百日之约，你做到了"
        200 -> "两百天的陪伴，文字见证成长"
        365 -> "一整年的记录，每一天都值得纪念"
        else -> "坚持写作，记录美好生活"
    }

    private fun getWordSubtitle(words: Int): String = when (words) {
        1000 -> "一千字的开始，故事从这里展开"
        5000 -> "五千字的积累，点滴汇成河流"
        10000 -> "万字成就，你的文字世界越来越丰富"
        30000 -> "三万字的沉淀，思想有了重量"
        50000 -> "五万字的旅程，比很多书都长了"
        100000 -> "十万字的里程碑，你已经是位作者了"
        200000 -> "二十万字的积累，文字是最好的朋友"
        500000 -> "五十万字的宇宙，每一段都是珍贵记忆"
        else -> "继续书写，记录美好生活"
    }
}
