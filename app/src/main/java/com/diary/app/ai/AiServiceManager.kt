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
}
