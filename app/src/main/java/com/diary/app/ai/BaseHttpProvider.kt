package com.diary.app.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

abstract class BaseHttpProvider(
    protected val context: Context,
    protected val configStore: AiConfigStore,
    protected val rateLimiter: RateLimiter
) : AiServiceProvider {

    protected val gson = Gson()

    protected open val connectTimeout = 10000
    protected open val readTimeout = 20000

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
        val url = URL("${cleanEndpoint(endpoint)}chat/completions")
        val conn = (url.openConnection() as? HttpURLConnection ?: throw IllegalArgumentException("Not HTTP")).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = this@BaseHttpProvider.connectTimeout
            readTimeout = this@BaseHttpProvider.readTimeout
            doOutput = true
        }

        val body = buildMap<String, Any> {
            put("model", model)
            put("messages", request.messages.map { mapOf("role" to it.role, "content" to it.content) })
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            put("stream", false)
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(gson.toJson(body))
            }

            val responseCode = conn.responseCode
            if (responseCode == 429) throw AiError.RateLimited
            if (responseCode == 401) throw AiError.ApiError(401, "API Key无效")
            if (responseCode == 402) throw AiError.ApiError(402, "配额已用完")
            if (responseCode != 200) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                throw AiError.ApiError(responseCode, "请求失败: $responseCode $errorBody")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
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
        } finally {
            conn.disconnect()
        }
    }
}
