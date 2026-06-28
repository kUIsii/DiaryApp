package com.diary.app.ai

import android.content.Context
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiarySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 日记摘要生成器 - 使用AI生成日记摘要
 */
object DiarySummarizer {
    
    private const val TAG = "DiarySummarizer"
    
    /**
     * 生成日记摘要
     */
    suspend fun generateSummary(
        context: Context,
        entry: DiaryEntry
    ): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = AiConfigStore.getApiKey(context)
            val endpoint = AiConfigStore.getEndpoint(context)
            
            if (apiKey.isBlank() || endpoint.isBlank()) {
                return@withContext null
            }
            
            val content = entry.plainText.take(2000) // 限制长度
            
            val prompt = """
                请为以下日记内容生成一个简洁的摘要（1-2句话），概括主要内容和情感：
                
                日记内容：
                $content
                
                摘要：
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", listOf(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                ))
                put("max_tokens", 100)
                put("temperature", 0.7)
            }
            
            val url = URL("${endpoint}chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return@withContext message.getString("content").trim()
                }
            }
            
            null
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to generate summary", e)
            null
        }
    }
    
    /**
     * 批量生成摘要
     */
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
