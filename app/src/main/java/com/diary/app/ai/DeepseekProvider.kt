package com.diary.app.ai

import android.content.Context

class DeepseekProvider(
    context: Context,
    configStore: AiConfigStore,
    rateLimiter: RateLimiter
) : BaseHttpProvider(context, configStore, rateLimiter) {

    override val id = "deepseek"
    override val displayName = "Deepseek"
    override val defaultModel = "deepseek-v4-flash"
    override val availableModels = listOf("deepseek-v4-flash", "deepseek-v4-pro")

    override val defaultEndpoint = "https://api.deepseek.com/v1/"
    override val connectTimeout = 15000
    override val readTimeout = 60000
}
