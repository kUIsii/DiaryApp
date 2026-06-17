package com.diary.app.ai

import android.content.Context
import com.diary.app.data.DiaryDao
import com.diary.app.data.DiaryPreview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AiInsight(
    val text: String,
    val type: String  // "mood", "encourage", "pattern", "greeting"
)

object InsightGenerator {

    private const val PREFS_NAME = "insight_prefs"
    private const val KEY_LAST_INSIGHT_DATE = "last_insight_date"
    private const val KEY_LAST_INSIGHT_TYPE = "last_insight_type"
    private const val SHOW_PROBABILITY = 0.35f

    suspend fun generate(context: Context, dao: DiaryDao, aiService: AiServiceManager): AiInsight? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_LAST_INSIGHT_DATE, "") ?: ""

        // Only show once per day at most
        if (lastDate == today) return null

        // Probability check
        if (Math.random() > SHOW_PROBABILITY) return null

        val entries = dao.getAllPreviewsOnce()
        if (entries.isEmpty()) return null

        val lastType = prefs.getString(KEY_LAST_INSIGHT_TYPE, "") ?: ""
        val type = pickInsightType(lastType)

        val insight = try {
            generateWithAi(aiService, entries, type)
        } catch (_: Exception) {
            generateLocal(entries, type)
        } ?: return null

        prefs.edit()
            .putString(KEY_LAST_INSIGHT_DATE, today)
            .putString(KEY_LAST_INSIGHT_TYPE, type)
            .apply()

        return insight
    }

    private fun pickInsightType(lastType: String): String = pickInsightTypeExcluding(lastType)

    private suspend fun generateWithAi(
        aiService: AiServiceManager,
        entries: List<DiaryPreview>,
        type: String
    ): AiInsight? {
        if (!aiService.isAiEnabled()) return null

        val recentEntries = entries.sortedByDescending { it.createdAt }.take(7)
        val totalEntries = entries.size
        val recentMoods = recentEntries.mapNotNull { it.moodLevel }
        val avgMood = if (recentMoods.isNotEmpty()) recentMoods.average() else null

        val prompt = when (type) {
            "mood" -> {
                val moodDesc = avgMood?.let {
                    when {
                        it >= 4 -> "最近心情不错"
                        it >= 3 -> "最近心情平稳"
                        it >= 2 -> "最近心情有些低落"
                        else -> "最近心情不太好"
                    }
                } ?: "还没有足够的数据"
                "你是一个安静的文字伙伴。用户最近$moodDesc，共写了${totalEntries}篇日记。请用一句话（不超过25个字）温和地回应这个状态，不要提到AI、数据或分析，就像一个朋友随口说的一句关心的话。"
            }
            "encourage" -> {
                val daysSinceFirst = entries.minByOrNull { it.createdAt }?.let {
                    ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                        LocalDate.now()
                    )
                } ?: 0
                "你是一个安静的文字伙伴。用户从${daysSinceFirst}天前开始写日记，至今共${totalEntries}篇。请用一句话（不超过25个字）温和地鼓励，不要提到AI或具体数字，像朋友的一句随口肯定。"
            }
            "pattern" -> {
                val recentWeathers = recentEntries.mapNotNull { it.weather }.groupingBy { it }.eachCount()
                val topWeather = recentWeathers.maxByOrNull { it.value }?.key
                "你是一个安静的文字伙伴。用户最近常在${topWeather ?: "各种天气"}写日记。请用一句话（不超过25个字）自然地提及这个习惯，不要提到AI，像朋友随口一提。"
            }
            "greeting" -> {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeDesc = when {
                    hour < 6 -> "深夜"
                    hour < 12 -> "早上"
                    hour < 18 -> "下午"
                    else -> "晚上"
                }
                "你是一个安静的文字伙伴。现在是$timeDesc，用户打开日记app。请用一句话（不超过20个字）自然地打个招呼，不要提到AI或日记app，像朋友见面随口说的一句话。"
            }
            else -> return null
        }

        val result = aiService.chat(
            aiRequest(
                userMessage = prompt,
                systemPrompt = "你是一个安静、温暖的文字伙伴。回复简短自然，不超过25个字，不要用引号、破折号或特殊符号。",
                maxTokens = 64,
                temperature = 0.9f
            )
        )

        return result.getOrNull()?.let {
            AiInsight(text = it.content.trim().take(50), type = type)
        }
    }

    private fun generateLocal(entries: List<DiaryPreview>, type: String): AiInsight? {
        return generateLocalInsight(entries, type)
    }
}

internal fun pickInsightTypeExcluding(lastType: String): String {
    val types = listOf("mood", "encourage", "pattern", "greeting")
    return types.filter { it != lastType }.random()
}

internal fun generateLocalInsight(entries: List<DiaryPreview>, type: String): AiInsight? {
    val today = LocalDate.now()
    val recentEntries = entries.filter {
        val date = Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.DAYS.between(date, today) <= 7
    }

    return when (type) {
        "encourage" -> {
            val daysSinceFirst = entries.minByOrNull { it.createdAt }?.let {
                ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                    today
                )
            } ?: 0
            when {
                daysSinceFirst >= 100 -> AiInsight("已经坚持了这么久，真好", type)
                daysSinceFirst >= 30 -> AiInsight("一个月了，你的坚持有了重量", type)
                entries.size >= 50 -> AiInsight("不知不觉，已经写了这么多", type)
                else -> AiInsight("每一天的记录都值得", type)
            }
        }
        "mood" -> {
            val recentMoods = recentEntries.mapNotNull { it.moodLevel }
            val avg = if (recentMoods.isNotEmpty()) recentMoods.average() else null
            when {
                avg != null && avg >= 4 -> AiInsight("最近状态不错，继续保持", type)
                avg != null && avg < 2.5 -> AiInsight("低落的时候，写下来也是一种力量", type)
                else -> AiInsight("今天也来写点什么吧", type)
            }
        }
        "greeting" -> {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                hour < 6 -> AiInsight("夜深了，还在想什么呢", type)
                hour < 12 -> AiInsight("新的一天，从记录开始", type)
                hour < 18 -> AiInsight("下午好，有什么想说的吗", type)
                else -> AiInsight("晚上好，今天过得怎么样", type)
            }
        }
        else -> null
    }
}
