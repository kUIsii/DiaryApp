package com.diary.app.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AiServiceManager(private val context: Context) {

    private val configStore = AiConfigStore
    private val rateLimiter = RateLimiter(context)
    private val providers = mutableMapOf<String, AiServiceProvider>()
    private val gson = Gson()

    // 响应缓存 (SharedPreferences, TTL 24h)
    private val cachePrefs = context.getSharedPreferences("ai_cache", Context.MODE_PRIVATE)

    init {
        providers["modelscope"] = ModelScopeProvider(context, configStore, rateLimiter)
        providers["agnes"] = AgnesProvider(context, configStore, rateLimiter)
        providers["deepseek"] = DeepseekProvider(context, configStore, rateLimiter)
    }

    fun getActiveProvider(): AiServiceProvider? {
        val providerId = configStore.getActiveProvider(context)
        return providers[providerId]
    }

    fun getProvider(id: String): AiServiceProvider? = providers[id]

    fun getAllProviders(): List<AiServiceProvider> = providers.values.toList()

    fun isAiEnabled(): Boolean {
        return configStore.isAiEnabled(context) && configStore.isConfigured(context)
    }

    fun getUsageStats(): RateLimiter.UsageStats = rateLimiter.getUsageStats()

    fun getDetailedUsageStats(): AiUsageTracker.UsageStats = AiUsageTracker.getTodayStats(context)

    suspend fun chat(request: AiRequest, useCache: Boolean = true): Result<AiResponse> {
        val provider = getActiveProvider() ?: return Result.failure(AiError.NotConfigured)

        // 检查缓存
        if (useCache) {
            val cached = getCachedResponse(request)
            if (cached != null) return Result.success(cached)
        }

        return try {
            val response = withContext(Dispatchers.IO) { provider.chat(request) }
            if (useCache) cacheResponse(request, response)
            if (response.totalTokens > 0) AiUsageTracker.record(context, response.totalTokens, response.model, response.providerId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("AiService", "Chat failed", e)
            Result.failure(e)
        }
    }

    private fun getCachedResponse(request: AiRequest): AiResponse? {
        val hash = hashRequest(request)
        val cached = cachePrefs.getString(hash, null) ?: return null

        return try {
            val entry = gson.fromJson(cached, CacheEntry::class.java)
            if (System.currentTimeMillis() > entry.expiresAt) {
                cachePrefs.edit().remove(hash).apply()
                null
            } else {
                AiResponse(entry.content, entry.model, entry.providerId)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheResponse(request: AiRequest, response: AiResponse) {
        val hash = hashRequest(request)
        val entry = CacheEntry(
            content = response.content,
            model = response.model,
            providerId = response.providerId,
            expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        )
        cachePrefs.edit().putString(hash, gson.toJson(entry)).apply()
    }

    private fun hashRequest(request: AiRequest): String {
        val raw = request.messages.joinToString("|") { "${it.role}:${it.content}" }
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class CacheEntry(
        val content: String,
        val model: String,
        val providerId: String,
        val expiresAt: Long
    )

    /**
     * Suggest tags for diary content. Returns a list of tag names.
     */
    suspend fun suggestTags(content: String, existingTagNames: List<String>): List<String> {
        if (!isAiEnabled() || content.length < 30) return emptyList()
        val prompt = "根据以下日记内容，从已有标签中选择最合适的2-4个标签，如果没有合适的就不推荐。" +
            "只返回标签名，用逗号分隔，不要解释。\n" +
            "已有标签：${existingTagNames.joinToString("、")}\n\n" +
            "日记内容：${content.take(500)}"
        return try {
            val result = chat(aiRequest(prompt, maxTokens = 60, temperature = 0.3f))
            result.getOrNull()?.content?.split(Regex("[,，、\\s]+"))
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() && it in existingTagNames }
                ?.distinct()
                ?.take(4)
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Parse natural language search query into structured keywords.
     * Returns a map with keys: keywords, mood (1-5), weather, favorite (true/false), dateStart, dateEnd.
     */
    suspend fun parseSearchQuery(query: String): Map<String, String> {
        if (!isAiEnabled()) return mapOf("keywords" to query)
        val prompt = "将以下自然语言搜索转换为JSON格式的搜索参数。" +
            "返回格式：{\"keywords\":\"关键词\",\"mood\":\"1-5的数字(1很低落2低落3平静4开心5非常开心)\",\"weather\":\"天气类型(晴/多云/阴/雨/雪/风)\",\"favorite\":\"true或false\",\"dateStart\":\"起始时间戳毫秒(可空)\",\"dateEnd\":\"结束时间戳毫秒(可空)\"}" +
            "只填有信息的字段，其他留空字符串。只返回JSON，不要解释。当前时间戳：${System.currentTimeMillis()}\n\n搜索：$query"
        return try {
            val result = chat(aiRequest(prompt, maxTokens = 120, temperature = 0.1f))
            val json = result.getOrNull()?.content?.trim() ?: ""
            val parsed = gson.fromJson(json, Map::class.java) as? Map<*, *>
            mapOf(
                "keywords" to (parsed?.get("keywords")?.toString() ?: query),
                "mood" to (parsed?.get("mood")?.toString() ?: ""),
                "weather" to (parsed?.get("weather")?.toString() ?: ""),
                "favorite" to (parsed?.get("favorite")?.toString() ?: ""),
                "dateStart" to (parsed?.get("dateStart")?.toString() ?: ""),
                "dateEnd" to (parsed?.get("dateEnd")?.toString() ?: "")
            )
        } catch (_: Exception) { mapOf("keywords" to query) }
    }

    /**
     * Analyze writing style from recent entries. Returns style analysis text.
     */
    suspend fun analyzeWritingStyle(entries: List<String>): String? {
        if (!isAiEnabled() || entries.isEmpty()) return null
        val combined = entries.take(10).joinToString("\n---\n") { it.take(300) }
        val prompt = "分析以下日记片段的写作风格，用2-3句话描述特点（简洁/详细、理性/感性、叙事/反思等），" +
            "并给一个温和的写作建议。不超过80字。\n\n$combined"
        return try {
            val result = chat(aiRequest(prompt, maxTokens = 120, temperature = 0.7f))
            result.getOrNull()?.content?.trim()
        } catch (_: Exception) { null }
    }
}
