package com.diary.app.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

abstract class BaseHttpProvider(
    protected val context: Context,
    protected val configStore: AiConfigStore,
    protected val rateLimiter: RateLimiter
) : AiServiceProvider {

    protected val gson = Gson()

    protected open val connectTimeoutSeconds = 10L
    protected open val readTimeoutSeconds = 20L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    override suspend fun chat(request: AiRequest): AiResponse {
        if (!configStore.isConfigured(context)) throw AiError.NotConfigured

        val model = request.model ?: configStore.getModel(context, id).ifBlank { defaultModel }
        if (!rateLimiter.canMakeRequest(model)) throw AiError.RateLimited

        val apiKey = configStore.getApiKey(context, id)
        val endpoint = configStore.getEndpoint(context, id).ifBlank { "https://apihub.agnes-ai.com/v1/" }

        return try {
            val response = makeRequest(endpoint, apiKey, request, model)
            rateLimiter.recordRequest(model)
            response
        } catch (e: AiError) {
            throw e
        } catch (e: Exception) {
            throw AiError.Unknown(e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!configStore.isConfigured(context)) return false
        if (!rateLimiter.canMakeRequest(defaultModel)) return false
        return try {
            val testRequest = aiRequest("hi", maxTokens = 5)
            chat(testRequest)
            true
        } catch (e: Exception) {
            false
        }
    }

    protected open fun cleanEndpoint(endpoint: String): String {
        var url = endpoint.trim()
        if (url.endsWith("/")) url = url.dropLast(1)
        if (url.endsWith("/chat/completions")) url = url.removeSuffix("/chat/completions")
        if (url.endsWith("/v1")) url = url.removeSuffix("/v1")
        return "$url/"
    }

    private fun makeRequest(
        endpoint: String,
        apiKey: String,
        request: AiRequest,
        model: String
    ): AiResponse {
        val url = "${cleanEndpoint(endpoint)}chat/completions"

        val body = buildMap<String, Any> {
            put("model", model)
            put("messages", request.messages.map { mapOf("role" to it.role, "content" to it.content) })
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            put("stream", false)
        }

        val jsonBody = gson.toJson(body).toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val code = response.code
            val errorBody = response.body?.string() ?: ""
            when (code) {
                429 -> throw AiError.RateLimited
                401 -> throw AiError.ApiError(401, "API Key无效")
                402 -> throw AiError.ApiError(402, "配额已用完")
                else -> throw AiError.ApiError(code, "请求失败: $code $errorBody")
            }
        }

        val responseBody = response.body?.string() ?: throw AiError.ParseError("响应体为空")
        val json = JsonParser.parseString(responseBody).asJsonObject

        val choices = json.getAsJsonArray("choices")
            ?: throw AiError.ParseError("API返回空的choices数组")
        if (choices.size() == 0) throw AiError.ParseError("choices数组为空")
        val content = choices.get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString

        val totalTokens = json.getAsJsonObject("usage")
            ?.get("total_tokens")?.asInt ?: 0

        return AiResponse(
            content = content,
            model = json.get("model")?.asString ?: model,
            providerId = id,
            totalTokens = totalTokens
        )
    }
}
