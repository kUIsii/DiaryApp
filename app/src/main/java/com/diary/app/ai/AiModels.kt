package com.diary.app.ai

// 统一请求/响应模型
data class AiMessage(
    val role: String,       // "system", "user", "assistant"
    val content: String
)

data class AiRequest(
    val messages: List<AiMessage>,
    val model: String? = null,         // null则用provider默认模型
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512
)

data class AiResponse(
    val content: String,
    val model: String,
    val providerId: String,
    val totalTokens: Int = 0
)

data class AiStreamChunk(
    val delta: String,
    val isFinished: Boolean
)

sealed class AiError : Exception() {
    object NotConfigured : AiError() {
        override val message = "未配置API Key"
    }
    object RateLimited : AiError() {
        override val message = "今日调用次数已用完"
    }
    object NetworkError : AiError() {
        override val message = "网络不可用"
    }
    data class ApiError(val code: Int, override val message: String) : AiError()
    data class Unknown(override val cause: Throwable?) : AiError() {
        override val message = cause?.message ?: "未知错误"
    }
}

// 便捷构建函数
fun aiRequest(
    userMessage: String,
    systemPrompt: String? = null,
    model: String? = null,
    temperature: Float = 0.7f,
    maxTokens: Int = 512
): AiRequest {
    val msgs = buildList {
        systemPrompt?.let { add(AiMessage("system", it)) }
        add(AiMessage("user", userMessage))
    }
    return AiRequest(msgs, model, temperature, maxTokens)
}
