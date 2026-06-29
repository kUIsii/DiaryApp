package com.diary.app.ui.personalyearbook

import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryDao
import com.diary.app.data.DiaryEntry
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class YearbookAiAnalyzer(private val aiService: AiServiceManager) {

    private val gson = Gson()
    private val zone = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun extractNarrativeArcs(entries: List<DiaryEntry>): List<NarrativeArc> {
        if (!aiService.isAiEnabled() || entries.isEmpty()) return emptyList()

        val prompt = buildString {
            appendLine("你是一个日记叙事分析专家。分析以下日记，识别出3-7个叙事弧（narrative arcs）。")
            appendLine("每个叙事弧代表一段时间内围绕某个主题的写作脉络：工作、人际关系、兴趣爱好、个人成长等。")
            appendLine("返回JSON：{\"arcs\":[{\"title\":\"标题\",\"entryIds\":[id,...],\"turningPointId\":id,\"emotionTrajectory\":[{\"entryId\":id,\"value\":mood}],\"summary\":\"总结\"}]}")
            appendLine("日记列表：")
            entries.forEach { e ->
                val d = Instant.ofEpochMilli(e.createdAt).atZone(zone).toLocalDate().format(dateFmt)
                val text = e.plainText.take(120).replace("\n", " ")
                appendLine("ID:${e.id} 日期:$d 标题:${e.title} 心情:${e.moodLevel ?: 3} 内容:$text")
            }
        }

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你只返回JSON，不要额外文字。",
            temperature = 0.6f,
            maxTokens = 2048
        )).getOrNull()?.content ?: return emptyList()

        return try {
            val cleaned = result.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val resp = gson.fromJson(cleaned, AiArcsResponse::class.java)
            resp.arcs.map { arc ->
                NarrativeArc(
                    title = arc.title,
                    entries = arc.entryIds,
                    turningPoint = arc.turningPointId,
                    emotionTrajectory = arc.emotionTrajectory.map { EmotionPoint(it.entryId, it.value) },
                    summary = arc.summary
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun selectMonthHighlights(entries: List<DiaryEntry>, year: Int): List<MonthHighlight> {
        if (!aiService.isAiEnabled() || entries.isEmpty()) return emptyList()

        val prompt = buildString {
            appendLine("你是一个日记精选专家。为${year}年每个有日记的月份选出一篇最具代表性的日记。")
            appendLine("选择标准：内容密度高、情感意义重要、对后续有影响、多样性。")
            appendLine("返回JSON：{\"highlights\":[{\"month\":1,\"entryId\":id,\"reason\":\"选择原因\"}]}")
            appendLine("日记列表：")
            entries.forEach { e ->
                val d = Instant.ofEpochMilli(e.createdAt).atZone(zone).toLocalDate()
                val text = e.plainText.take(100).replace("\n", " ")
                appendLine("ID:${e.id} 月:${d.monthValue} 日:${d.dayOfMonth} 标题:${e.title} 心情:${e.moodLevel ?: 3} 字数:${e.plainText.length} 内容:$text")
            }
        }

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你只返回JSON，不要额外文字。",
            temperature = 0.5f,
            maxTokens = 1536
        )).getOrNull()?.content ?: return emptyList()

        return try {
            val cleaned = result.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val resp = gson.fromJson(cleaned, AiHighlightsResponse::class.java)
            resp.highlights.map { h ->
                val entry = entries.find { it.id == h.entryId }
                MonthHighlight(
                    month = h.month,
                    entryId = h.entryId,
                    entryTitle = entry?.title ?: "",
                    reason = h.reason
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun generateMetaphor(entries: List<DiaryEntry>, arcs: List<NarrativeArc>): Pair<String, List<MetaphorPhase>> {
        if (!aiService.isAiEnabled() || entries.isEmpty()) {
            return "" to emptyList()
        }

        val arcSummary = if (arcs.isNotEmpty()) {
            arcs.joinToString(";") { "${it.title}:${it.summary}" }
        } else {
            "暂无明确叙事弧"
        }

        val prompt = buildString {
            appendLine("你是一个富有诗意的文字分析师。根据用户的日记叙事弧和整体情感变化，生成年度隐喻。")
            appendLine("格式：你的这一年像（一句中文比喻，10-20字）")
            appendLine("同时生成三个阶段的隐喻发展。")
            appendLine("叙事弧：$arcSummary")
            appendLine("总日记数：${entries.size}")
            val moods = entries.mapNotNull { it.moodLevel }
            if (moods.isNotEmpty()) {
                appendLine("平均心情：${"%.1f".format(moods.average())}")
            }
            appendLine("返回JSON：{\"metaphor\":\"你的这一年像...\",\"evolution\":[{\"period\":\"年初\",\"description\":\"...\"},{\"period\":\"年中\",\"description\":\"...\"},{\"period\":\"年末\",\"description\":\"...\"}]}")
        }

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你只返回JSON，不要额外文字。",
            temperature = 0.8f,
            maxTokens = 1024
        )).getOrNull()?.content ?: return "" to emptyList()

        return try {
            val cleaned = result.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val resp = gson.fromJson(cleaned, AiMetaphorResponse::class.java)
            resp.metaphor to resp.evolution.map { MetaphorPhase(it.period, it.description) }
        } catch (_: Exception) { "" to emptyList() }
    }

    suspend fun curatePhotos(entries: List<DiaryEntry>, dao: DiaryDao): List<String> {
        val entryIds = entries.map { it.id }
        val allImages = dao.getImagesForEntries(entryIds)
        if (allImages.isEmpty()) return emptyList()

        val entryMap = entries.associateBy { it.id }

        val scored = allImages.map { image ->
            val entry = entryMap[image.entryId]
            var score = 0f
            score += (image.fileSize.toFloat() / 512_000f).coerceIn(0f, 1f) * 0.3f
            if (entry != null) {
                val wordScore = (entry.plainText.length / 500f).coerceIn(0f, 1f) * 0.3f
                val moodScore = entry.moodLevel?.let { (kotlin.math.abs(it - 3.5f) / 2.5f).toFloat() } ?: 0.2f
                score += wordScore + moodScore * 0.4f
            }
            image to score
        }.sortedByDescending { it.second }

        val selected = mutableListOf<String>()
        for ((image, _) in scored) {
            if (selected.size >= 12) break
            if (image.localPath.isNotEmpty()) {
                selected.add(image.localPath)
            }
        }

        return selected
    }

    private data class AiArcDto(
        val title: String,
        val entryIds: List<Long>,
        val turningPointId: Long,
        val emotionTrajectory: List<AiEmotionDto>,
        val summary: String
    )

    private data class AiEmotionDto(val entryId: Long, val value: Float)

    private data class AiArcsResponse(val arcs: List<AiArcDto>)

    private data class AiHighlightDto(val month: Int, val entryId: Long, val reason: String)

    private data class AiHighlightsResponse(val highlights: List<AiHighlightDto>)

    private data class AiPhaseDto(val period: String, val description: String)

    private data class AiMetaphorResponse(val metaphor: String, val evolution: List<AiPhaseDto>)
}
