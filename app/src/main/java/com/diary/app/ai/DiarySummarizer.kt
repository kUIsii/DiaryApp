package com.diary.app.ai

import android.content.Context
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiarySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DiarySummarizer {

    suspend fun generateSummary(
        context: Context,
        entry: DiaryEntry
    ): String? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? DiaryApplication ?: return@withContext null
        if (!app.aiService.isAiEnabled()) return@withContext null
        val content = entry.plainText.take(2000)
        val prompt = "请为以下日记内容生成一个简洁的摘要（1-2句话），概括主要内容和情感：\n\n日记内容：\n$content\n\n摘要："
        val request = aiRequest(userMessage = prompt, maxTokens = 100)
        app.aiService.chat(request, useCache = true).fold(
            onSuccess = { it.content },
            onFailure = { null }
        )
    }

    suspend fun generateSummariesForEntries(
        context: Context,
        entries: List<DiaryEntry>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<Long, String> = withContext(Dispatchers.IO) {
        val summaries = mutableMapOf<Long, String>()
        entries.forEachIndexed { index, entry ->
            val summary = generateSummary(context, entry)
            if (summary != null) {
                summaries[entry.id] = summary
            }
            onProgress(index + 1, entries.size)
        }
        summaries
    }
}
