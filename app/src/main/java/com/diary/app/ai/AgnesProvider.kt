package com.diary.app.ai

import android.content.Context

class AgnesProvider(
    context: Context,
    configStore: AiConfigStore,
    rateLimiter: RateLimiter
) : BaseHttpProvider(context, configStore, rateLimiter) {

    override val id = "agnes"
    override val displayName = "Agnes AI"
    override val defaultModel = "agnes-2.0-flash"
    override val availableModels = listOf("agnes-2.0-flash")
    override val defaultEndpoint = "https://apihub.agnes-ai.com/v1/"
}
