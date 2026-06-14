package com.diary.app.ai

import android.content.Context

object AiConfigStore {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_AI_ENABLED = "ai_enabled"
    private const val KEY_AI_PROVIDER = "ai_active_provider"
    private const val KEY_AI_MODEL = "ai_model"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_ENDPOINT = "ai_endpoint"

    private const val DEFAULT_ENDPOINT = "https://api-inference.modelscope.cn/v1/"
    private const val DEFAULT_MODEL = "Qwen/Qwen2.5-7B-Instruct"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAiEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AI_ENABLED, false)

    fun setAiEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    fun getActiveProvider(context: Context): String =
        prefs(context).getString(KEY_AI_PROVIDER, "modelscope") ?: "modelscope"

    fun setActiveProvider(context: Context, provider: String) {
        prefs(context).edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_AI_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_AI_API_KEY, key).apply()
    }

    fun getModel(context: Context): String =
        prefs(context).getString(KEY_AI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setModel(context: Context, model: String) {
        prefs(context).edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun getEndpoint(context: Context): String =
        prefs(context).getString(KEY_AI_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT

    fun setEndpoint(context: Context, endpoint: String) {
        prefs(context).edit().putString(KEY_AI_ENDPOINT, endpoint).apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }
}
