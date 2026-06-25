package com.diary.app.ai

import android.content.Context

object AiConfigStore {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_AI_ENABLED = "ai_enabled"
    private const val KEY_AI_PROVIDER = "ai_active_provider"

    // Legacy single-provider keys (migrated to per-provider)
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_ENDPOINT = "ai_endpoint"
    private const val KEY_AI_MODEL = "ai_model"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Global ──────────────────────────────────────────────

    fun isAiEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AI_ENABLED, false)

    fun setAiEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    fun getActiveProvider(context: Context): String =
        prefs(context).getString(KEY_AI_PROVIDER, "agnes") ?: "agnes"

    fun setActiveProvider(context: Context, provider: String) {
        prefs(context).edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    // ── Per-provider config ─────────────────────────────────

    fun getApiKey(context: Context, providerId: String): String {
        val perProvider = prefs(context).getString("ai_key_$providerId", null)
        if (perProvider != null) return perProvider
        // Migration: read legacy key for agnes
        if (providerId == "agnes") {
            val legacy = prefs(context).getString(KEY_AI_API_KEY, "") ?: ""
            if (legacy.isNotBlank()) {
                setApiKey(context, providerId, legacy)
                return legacy
            }
        }
        return ""
    }

    fun setApiKey(context: Context, providerId: String, key: String) {
        prefs(context).edit().putString("ai_key_$providerId", key).apply()
    }

    fun getEndpoint(context: Context, providerId: String): String {
        val perProvider = prefs(context).getString("ai_endpoint_$providerId", null)
        if (perProvider != null) return perProvider
        // Migration: read legacy endpoint for agnes
        if (providerId == "agnes") {
            val legacy = prefs(context).getString(KEY_AI_ENDPOINT, null)
            if (legacy != null) {
                setEndpoint(context, providerId, legacy)
                return legacy
            }
        }
        return ""
    }

    fun setEndpoint(context: Context, providerId: String, endpoint: String) {
        prefs(context).edit().putString("ai_endpoint_$providerId", endpoint).apply()
    }

    fun getModel(context: Context, providerId: String): String {
        val perProvider = prefs(context).getString("ai_model_$providerId", null)
        if (perProvider != null) return perProvider
        if (providerId == "agnes") {
            val legacy = prefs(context).getString(KEY_AI_MODEL, null)
            if (legacy != null) {
                setModel(context, providerId, legacy)
                return legacy
            }
        }
        return ""
    }

    fun setModel(context: Context, providerId: String, model: String) {
        prefs(context).edit().putString("ai_model_$providerId", model).apply()
    }

    // ── Legacy compat (used by BaseHttpProvider) ────────────

    @Deprecated("Use getApiKey(context, providerId)")
    fun getApiKey(context: Context): String {
        val active = getActiveProvider(context)
        return getApiKey(context, active)
    }

    @Deprecated("Use getEndpoint(context, providerId)")
    fun getEndpoint(context: Context): String {
        val active = getActiveProvider(context)
        val ep = getEndpoint(context, active)
        return ep.ifBlank { "https://apihub.agnes-ai.com/v1/" }
    }

    @Deprecated("Use getModel(context, providerId)")
    fun getModel(context: Context): String {
        val active = getActiveProvider(context)
        return getModel(context, active)
    }

    @Deprecated("Use setApiKey(context, providerId, key)")
    fun setApiKey(context: Context, key: String) {
        val active = getActiveProvider(context)
        setApiKey(context, active, key)
    }

    @Deprecated("Use setEndpoint(context, providerId, endpoint)")
    fun setEndpoint(context: Context, endpoint: String) {
        val active = getActiveProvider(context)
        setEndpoint(context, active, endpoint)
    }

    @Deprecated("Use setModel(context, providerId, model)")
    fun setModel(context: Context, model: String) {
        val active = getActiveProvider(context)
        setModel(context, active, model)
    }

    fun isConfigured(context: Context): Boolean {
        val active = getActiveProvider(context)
        return getApiKey(context, active).isNotBlank()
    }
}
