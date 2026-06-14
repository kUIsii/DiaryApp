package com.diary.app.ai

interface AiServiceProvider {
    val id: String
    val displayName: String
    val defaultModel: String
    val availableModels: List<String>

    suspend fun chat(request: AiRequest): AiResponse
    suspend fun isAvailable(): Boolean
}
