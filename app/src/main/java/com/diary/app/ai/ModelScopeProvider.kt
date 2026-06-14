package com.diary.app.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ModelScopeProvider(
    private val context: Context,
    private val configStore: AiConfigStore,
    private val rateLimiter: RateLimiter
) : AiServiceProvider {

    override val id = "modelscope"
    override val displayName = "ModelScope"
    override val defaultModel = "Qwen/Qwen2.5-7B-Instruct"
    override val availableModels = listOf(
        "Qwen/Qwen2.5-7B-Instruct",
        "Qwen/Qwen2.5-14B-Instruct",
        "Qwen/Qwen2.5-32B-Instruct",
        "Qwen/Qwen3.5-35B-A3B"
    )

    private val gson = Gson()

    override suspend fun chat(request: AiRequest): AiResponse {
        if (!configStore.isConfigured(context)) throw AiError.NotConfigured

        val model = request.model ?: configStore.getModel(context)
        if (!rateLimiter.canMakeRequest(model)) throw AiError.RateLimited

        val apiKey = configStore.getApiKey(context)
        val endpoint = configStore.getEndpoint(context)

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

    private fun makeRequest(
        endpoint: String,
        apiKey: String,
        request: AiRequest,
        model: String
    ): AiResponse {
        val url = URL("${endpoint}chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
        }

        // 构建请求体
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

            val content = json
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            return AiResponse(
                content = content,
                model = json.get("model")?.asString ?: model,
                providerId = id
            )
        } finally {
            conn.disconnect()
        }
    }
}
