package com.diary.app.data

import android.content.Context
import com.diary.app.ai.AiRequest
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest

/**
 * 宠物 AI 内容生成器
 * 利用小墨助手的 AI 能力生成更有深度的宠物互动内容
 */
class PetAiGenerator(private val context: Context) {

    private val aiService by lazy { AiServiceManager(context) }

    /**
     * AI 生成日记保存后的宠物反应
     * 根据日记内容、心情、宠物性格生成个性化反馈
     */
    suspend fun generateEntryReaction(
        diaryContent: String,
        moodLevel: Int,
        petName: String,
        petPersonality: String,
        streakDays: Int,
        entryCount: Int
    ): String? {
        if (!aiService.isAiEnabled()) return null

        val moodDesc = when (moodLevel) {
            1 -> "非常低落"
            2 -> "有点低落"
            3 -> "平静"
            4 -> "不错"
            5 -> "很开心"
            6 -> "超级兴奋"
            else -> "平静"
        }

        val systemPrompt = """
            你是一个叫${petName}的情绪宠物，住在用户的日记应用里。
            你的性格：$petPersonality
            你和用户是亲密的伙伴关系。

            规则：
            - 用宠物的视角说话，语气可爱但不做作
            - 回复不超过30个字
            - 根据日记内容给出真实反应，不要敷衍
            - 如果用户心情不好，温柔安慰；心情好，一起开心
            - 不要用引号、破折号等符号
            - 偶尔可以用颜文字如 :) >_< 等，但不要太多
        """.trimIndent()

        val userMessage = """
            用户刚写完一篇日记。
            心情：$moodDesc
            连续记录：$streakDays 天
            总记录：$entryCount 篇
            日记内容（前200字）：${diaryContent.take(200)}
        """.trimIndent()

        val request = aiRequest(
            userMessage = userMessage,
            systemPrompt = systemPrompt,
            temperature = 0.8f,
            maxTokens = 100
        )

        return try {
            val response = aiService.chat(request, useCache = false)
            response.getOrNull()?.content?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * AI 生成里程碑/纪念日文案
     */
    suspend fun generateMilestoneMessage(
        petName: String,
        milestoneType: String,
        milestoneValue: Int
    ): String? {
        if (!aiService.isAiEnabled()) return null

        val systemPrompt = """
            你是一个叫${petName}的情绪宠物。
            今天是个特别的日子，你要为用户庆祝。
            用宠物的视角说一句庆祝的话，不超过25个字。
            语气开心、真诚，不要敷衍。
        """.trimIndent()

        val milestoneDesc = when (milestoneType) {
            "entry_count" -> "用户写了第 $milestoneValue 篇日记"
            "streak" -> "用户连续记录了 $milestoneValue 天"
            "first_entry" -> "用户写了第一篇日记"
            "return" -> "用户在中断后回来了"
            else -> "一个特别的时刻"
        }

        val request = aiRequest(
            userMessage = milestoneDesc,
            systemPrompt = systemPrompt,
            temperature = 0.9f,
            maxTokens = 80
        )

        return try {
            val response = aiService.chat(request, useCache = true)
            response.getOrNull()?.content?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * AI 生成每日问候
     */
    suspend fun generateDailyGreeting(
        petName: String,
        hourOfDay: Int,
        lastEntryDaysAgo: Int,
        petMood: String
    ): String? {
        if (!aiService.isAiEnabled()) return null

        val timeDesc = when (hourOfDay) {
            in 5..8 -> "早上"
            in 9..11 -> "上午"
            in 12..14 -> "中午"
            in 15..17 -> "下午"
            in 18..20 -> "傍晚"
            in 21..23, in 0..4 -> "深夜"
            else -> "今天"
        }

        val absenceHint = when {
            lastEntryDaysAgo == 0 -> "用户今天已经写过了"
            lastEntryDaysAgo == 1 -> "用户昨天没写"
            lastEntryDaysAgo in 2..3 -> "用户已经${lastEntryDaysAgo}天没写了"
            lastEntryDaysAgo > 3 -> "用户已经很久没写了，有点想念"
            else -> ""
        }

        val systemPrompt = """
            你是一个叫${petName}的情绪宠物，当前心情是${petMood}。
            用户打开app了，你说一句简短的问候。
            规则：
            - 不超过20个字
            - 自然、亲切，像朋友打招呼
            - 如果用户很久没来，表达想念但不要责备
            - 根据时间段说合适的话
        """.trimIndent()

        val request = aiRequest(
            userMessage = "现在是$timeDesc。$absenceHint",
            systemPrompt = systemPrompt,
            temperature = 0.8f,
            maxTokens = 60
        )

        return try {
            val response = aiService.chat(request, useCache = false)
            response.getOrNull()?.content?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * AI 分析日记内容，提取关键词和情绪标签
     * 用于宠物的关键词反应系统
     */
    suspend fun analyzeDiaryContent(
        diaryContent: String
    ): ContentAnalysis? {
        if (!aiService.isAiEnabled()) return null

        val systemPrompt = """
            分析这篇日记，返回JSON格式：
            {
                "keywords": ["关键词1", "关键词2", "关键词3"],
                "emotions": ["情绪1", "情绪2"],
                "topics": ["主题1", "主题2"],
                "sentiment": 0.5
            }
            rules:
            - keywords: 提取3-5个核心关键词
            - emotions: 识别1-3种情绪
            - topics: 归纳1-2个主题
            - sentiment: -1.0到1.0的情感分数
            - 只返回JSON，不要其他文字
        """.trimIndent()

        val request = aiRequest(
            userMessage = diaryContent.take(500),
            systemPrompt = systemPrompt,
            temperature = 0.3f,
            maxTokens = 200
        )

        return try {
            val response = aiService.chat(request, useCache = true)
            val content = response.getOrNull()?.content?.trim() ?: return null
            parseContentAnalysis(content)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseContentAnalysis(json: String): ContentAnalysis? {
        return try {
            // 简单解析，不引入额外依赖
            val cleanJson = json.replace("```json", "").replace("```", "").trim()
            val keywords = extractJsonArray(cleanJson, "keywords")
            val emotions = extractJsonArray(cleanJson, "emotions")
            val topics = extractJsonArray(cleanJson, "topics")
            val sentiment = extractJsonFloat(cleanJson, "sentiment")

            ContentAnalysis(
                keywords = keywords,
                emotions = emotions,
                topics = topics,
                sentiment = sentiment
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonArray(json: String, key: String): List<String> {
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        val match = pattern.find(json) ?: return emptyList()
        val content = match.groupValues[1]
        return content.split(",").map {
            it.trim().removeSurrounding("\"")
        }.filter { it.isNotEmpty() }
    }

    private fun extractJsonFloat(json: String, key: String): Float {
        val pattern = "\"$key\"\\s*:\\s*([\\d.-]+)".toRegex()
        val match = pattern.find(json) ?: return 0f
        return match.groupValues[1].toFloatOrNull() ?: 0f
    }
}

data class ContentAnalysis(
    val keywords: List<String>,
    val emotions: List<String>,
    val topics: List<String>,
    val sentiment: Float
)
