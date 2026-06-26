package com.diary.app.ai

import android.content.Context

class ModelScopeProvider(
    context: Context,
    configStore: AiConfigStore,
    rateLimiter: RateLimiter
) : BaseHttpProvider(context, configStore, rateLimiter) {

    override val id = "modelscope"
    override val displayName = "ModelScope"
    override val defaultModel = "Qwen/Qwen2.5-7B-Instruct"
    override val availableModels = listOf(
        "Qwen/Qwen2.5-7B-Instruct",
        "Qwen/Qwen2.5-14B-Instruct",
        "Qwen/Qwen2.5-32B-Instruct",
        "Qwen/Qwen3.5-35B-A3B"
    )

    override val connectTimeoutSeconds = 15L
    override val readTimeoutSeconds = 30L
}
