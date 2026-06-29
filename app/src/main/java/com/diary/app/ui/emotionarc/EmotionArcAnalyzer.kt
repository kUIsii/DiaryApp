package com.diary.app.ui.emotionarc

import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class EmotionAnalysis(
    val id: String,
    val periodStart: Long,
    val periodEnd: Long,
    val patterns: List<EmotionPattern>,
    val triggers: List<EmotionTrigger>,
    val forecast: List<ForecastPoint>?,
    val narrativeSummary: String?,
    val comparisonId: String?
)

data class EmotionPattern(
    val type: String,
    val description: String,
    val confidence: Float,
    val relatedEntryIds: List<Long>
)

data class EmotionTrigger(
    val keyword: String,
    val impact: Float,
    val frequency: Int,
    val examples: List<Pair<Long, String>>
)

data class ForecastPoint(
    val dayOffset: Int,
    val emotion: Float,
    val confidence: Float
)

class EmotionArcAnalyzer(private val aiService: AiServiceManager) {

    private val gson = Gson()
    private val zone = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("MM-dd")

    suspend fun analyze(entries: List<DiaryEntry>, periodStart: Long, periodEnd: Long): EmotionAnalysis? {
        if (!aiService.isAiEnabled() || entries.isEmpty()) return null

        val prompt = buildPrompt(entries, periodStart, periodEnd)

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你只返回JSON，不要额外文字。",
            temperature = 0.6f,
            maxTokens = 2048
        )).getOrNull()?.content ?: return null

        return parseResponse(result, periodStart, periodEnd)
    }

    suspend fun comparePeriods(
        label1: String, entries1: List<DiaryEntry>,
        label2: String, entries2: List<DiaryEntry>
    ): String? {
        if (!aiService.isAiEnabled()) return null

        val prompt = buildString {
            appendLine("比较用户两个时期的日记情绪数据，用中文写出差异分析，包含具体数值和百分比。")
            appendLine("")
            appendLine("时期1：$label1")
            entries1.forEach { appendEntry(it) }
            appendLine("")
            appendLine("时期2：$label2")
            entries2.forEach { appendEntry(it) }
            appendLine("")
            appendLine("返回JSON：{\"comparison\":\"差异描述文本\"}")
        }

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你只返回JSON，不要额外文字。",
            temperature = 0.5f,
            maxTokens = 1024
        )).getOrNull()?.content ?: return null

        return try {
            val cleaned = result.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            gson.fromJson(cleaned, ComparisonDto::class.java).comparison
        } catch (_: Exception) { null }
    }

    fun analyzeLocal(entries: List<DiaryEntry>, periodStart: Long, periodEnd: Long): EmotionAnalysis {
        val patterns = mutableListOf<EmotionPattern>()
        val triggers = mutableListOf<EmotionTrigger>()

        val dayOfWeekEmotions = entries.groupBy { e ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = e.createdAt }
            cal.get(java.util.Calendar.DAY_OF_WEEK)
        }.mapValues { (_, es) ->
            es.mapNotNull { it.moodLevel?.let { (it - 1) / 5f } }.average().toFloat()
        }
        if (dayOfWeekEmotions.size >= 4) {
            val minDay = dayOfWeekEmotions.minByOrNull { it.value }
            val maxDay = dayOfWeekEmotions.maxByOrNull { it.value }
            if (minDay != null && maxDay != null) {
                val dayNames = mapOf(2 to "周一", 3 to "周二", 4 to "周三", 5 to "周四", 6 to "周五", 7 to "周六", 1 to "周日")
                patterns.add(EmotionPattern(
                    type = "weekly",
                    description = "${dayNames[maxDay.key] ?: "某天"}情绪最高${"%.1f".format(maxDay.value)}，${dayNames[minDay.key] ?: "某天"}最低${"%.1f".format(minDay.value)}",
                    confidence = 0.5f,
                    relatedEntryIds = emptyList()
                ))
            }
        }

        val weatherGroups = entries.filter { it.weather != null }.groupBy { it.weather }
        weatherGroups.forEach { (weather, group) ->
            val avg = group.mapNotNull { it.moodLevel?.let { (it - 1) / 5f } }.average().toFloat()
            if (group.size >= 2) {
                val diff = (avg - 0.5f)
                if (kotlin.math.abs(diff) > 0.1f) {
                    patterns.add(EmotionPattern(
                        type = "weather",
                        description = "${weather}天情绪${if (diff > 0) "偏高" else "偏低"}${"%.1f".format(kotlin.math.abs(diff))}",
                        confidence = 0.4f,
                        relatedEntryIds = group.map { it.id }
                    ))
                }
            }
        }

        val triggerKeywords = listOf("加班" to -0.3f, "朋友" to 0.3f, "家人" to 0.2f, "生病" to -0.4f, "旅行" to 0.4f)
        triggerKeywords.forEach { (keyword, baseImpact) ->
            val matching = entries.filter { it.plainText.contains(keyword) }
            if (matching.size >= 2) {
                val avgMood = matching.mapNotNull { it.moodLevel?.let { (it - 1) / 5f } }.average().toFloat()
                val impact = avgMood - 0.5f
                triggers.add(EmotionTrigger(
                    keyword = keyword,
                    impact = impact,
                    frequency = matching.size,
                    examples = matching.take(3).map { it.id to it.plainText.take(40) }
                ))
            }
        }

        val narrative = buildString {
            val moods = entries.mapNotNull { it.moodLevel?.let { (it - 1) / 5f } }
            if (moods.isNotEmpty()) {
                val avg = moods.average()
                val trend = when {
                    moods.size >= 3 && moods.last() > moods.first() + 0.15f -> "整体呈现上升趋势"
                    moods.size >= 3 && moods.last() < moods.first() - 0.15f -> "整体呈现下降趋势"
                    else -> "整体较为平稳"
                }
                appendLine("该时期共${entries.size}篇日记，平均情绪${"%.1f".format(avg)}，$trend。")
            }
        }.trim()

        return EmotionAnalysis(
            id = "$periodStart-$periodEnd",
            periodStart = periodStart,
            periodEnd = periodEnd,
            patterns = patterns,
            triggers = triggers,
            forecast = generateLocalForecast(entries),
            narrativeSummary = narrative.ifEmpty { null },
            comparisonId = null
        )
    }

    private fun generateLocalForecast(entries: List<DiaryEntry>): List<ForecastPoint> {
        val moods = entries.mapNotNull { it.moodLevel?.let { (it - 1) / 5f } }
        if (moods.isEmpty()) return emptyList()
        val avg = moods.average().toFloat()
        return (1..7).map { day ->
            val confidence = (0.8f - day * 0.08f).coerceAtLeast(0.3f)
            ForecastPoint(dayOffset = day, emotion = avg, confidence = confidence)
        }
    }

    private fun buildPrompt(entries: List<DiaryEntry>, start: Long, end: Long): String {
        return buildString {
            appendLine("分析以下日记数据，时间范围${formatDate(start)}到${formatDate(end)}，共${entries.size}篇。")
            appendLine("请做以下分析：")
            appendLine("1. 情绪模式：识别周期性模式（每周哪天情绪高/低、天气对情绪的影响、反复出现关键词的影响、季节性变化）")
            appendLine("2. 情绪触发词：找出反复出现且明显影响情绪的词语，给出影响幅度")
            appendLine("3. 未来7天预测：基于历史趋势预测未来7天情绪数值(0-1)")
            appendLine("4. 叙事总结：2-3句话概括该时期情绪变化的关键转折点")
            appendLine("")
            appendLine("返回JSON格式（严格）：")
            appendLine("{\"patterns\":[{\"type\":\"weekly\",\"description\":\"描述\",\"confidence\":0.8,\"relatedEntryIds\":[1,2]}],")
            appendLine("\"triggers\":[{\"keyword\":\"加班\",\"impact\":-0.3,\"frequency\":3,\"examples\":[[1,\"原文片段\"],[2,\"原文片段\"]]}],")
            appendLine("\"forecast\":[{\"dayOffset\":1,\"emotion\":0.55,\"confidence\":0.7}],")
            appendLine("\"narrativeSummary\":\"叙事总结文本\"}")
            appendLine("")
            appendLine("日记数据：")
            entries.forEach { appendEntry(it) }
        }
    }

    private fun StringBuilder.appendEntry(entry: DiaryEntry) {
        val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().format(dateFmt)
        val mood = entry.moodLevel?.let { (it - 1) / 5f } ?: 0.5f
        val text = entry.plainText.take(80).replace("\n", " ")
        appendLine("ID:${entry.id} 日期:$date 情绪:$mood 天气:${entry.weather ?: "无"} 内容:$text")
    }

    private fun formatDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    private fun parseResponse(json: String, periodStart: Long, periodEnd: Long): EmotionAnalysis? {
        return try {
            val cleaned = json.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val resp = gson.fromJson(cleaned, AiResponseDto::class.java)
            EmotionAnalysis(
                id = "$periodStart-$periodEnd",
                periodStart = periodStart,
                periodEnd = periodEnd,
                patterns = resp.patterns?.map { p ->
                    EmotionPattern(
                        type = p.type,
                        description = p.description,
                        confidence = p.confidence.coerceIn(0f, 1f),
                        relatedEntryIds = p.relatedEntryIds ?: emptyList()
                    )
                } ?: emptyList(),
                triggers = resp.triggers?.map { t ->
                    EmotionTrigger(
                        keyword = t.keyword,
                        impact = t.impact,
                        frequency = t.frequency,
                        examples = t.examples?.mapNotNull { ex ->
                            if (ex.size >= 2) {
                                val id = when (val v = ex[0]) {
                                    is Number -> v.toLong()
                                    is String -> v.toLongOrNull() ?: 0L
                                    else -> 0L
                                }
                                id to ex[1].toString()
                            } else null
                        } ?: emptyList()
                    )
                } ?: emptyList(),
                forecast = resp.forecast?.map { f ->
                    ForecastPoint(f.dayOffset, f.emotion.coerceIn(0f, 1f), f.confidence.coerceIn(0f, 1f))
                },
                narrativeSummary = resp.narrativeSummary?.takeIf { it.isNotBlank() },
                comparisonId = null
            )
        } catch (e: Exception) { null }
    }

    private data class AiResponseDto(
        val patterns: List<PatternDto>?,
        val triggers: List<TriggerDto>?,
        val forecast: List<ForecastDto>?,
        val narrativeSummary: String?
    )
    private data class PatternDto(val type: String, val description: String, val confidence: Float, val relatedEntryIds: List<Long>?)
    private data class TriggerDto(val keyword: String, val impact: Float, val frequency: Int, val examples: List<List<Any>>?)
    private data class ForecastDto(val dayOffset: Int, val emotion: Float, val confidence: Float)
    private data class ComparisonDto(val comparison: String)
}
