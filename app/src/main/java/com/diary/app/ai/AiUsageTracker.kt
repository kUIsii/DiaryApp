package com.diary.app.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiUsageTracker {
    private const val PREFS_NAME = "ai_usage"
    private const val KEY_PREFIX_REQUESTS = "req_"
    private const val KEY_PREFIX_TOKENS = "tok_"
    private const val KEY_PREFIX_MODEL_TOKENS = "mtok_"
    private const val KEY_PREFIX_MODEL_REQUESTS = "mreq_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val gson = Gson()

    @Synchronized
    fun record(context: Context, tokens: Int, model: String? = null) {
        val key = todayKey()
        val p = prefs(context)
        val editor = p.edit()
        editor.putInt(KEY_PREFIX_REQUESTS + key, p.getInt(KEY_PREFIX_REQUESTS + key, 0) + 1)
        editor.putInt(KEY_PREFIX_TOKENS + key, p.getInt(KEY_PREFIX_TOKENS + key, 0) + tokens)

        if (model != null) {
            // Per-model request count
            val modelReqKey = KEY_PREFIX_MODEL_REQUESTS + key
            val modelReqMap = parseMap(p.getString(modelReqKey, "{}") ?: "{}")
            modelReqMap[model] = (modelReqMap[model] ?: 0) + 1
            editor.putString(modelReqKey, gson.toJson(modelReqMap))

            // Per-model token count
            val modelTokKey = KEY_PREFIX_MODEL_TOKENS + key
            val modelTokMap = parseMap(p.getString(modelTokKey, "{}") ?: "{}")
            modelTokMap[model] = (modelTokMap[model] ?: 0) + tokens
            editor.putString(modelTokKey, gson.toJson(modelTokMap))
        }

        editor.apply()
    }

    fun getTodayStats(context: Context): UsageStats {
        val key = todayKey()
        val p = prefs(context)
        return UsageStats(
            requests = p.getInt(KEY_PREFIX_REQUESTS + key, 0),
            tokens = p.getInt(KEY_PREFIX_TOKENS + key, 0),
            modelRequests = parseMap(p.getString(KEY_PREFIX_MODEL_REQUESTS + key, "{}") ?: "{}"),
            modelTokens = parseMap(p.getString(KEY_PREFIX_MODEL_TOKENS + key, "{}") ?: "{}")
        )
    }

    private fun parseMap(json: String): MutableMap<String, Int> {
        return try {
            val type = TypeToken.getParameterized(
                Map::class.java, String::class.java, Integer::class.java
            ).type
            val raw: Map<String, Int> = gson.fromJson(json, type)
            raw.mapValues { it.value.toInt() }.toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    data class UsageStats(
        val requests: Int,
        val tokens: Int,
        val modelRequests: Map<String, Int> = emptyMap(),
        val modelTokens: Map<String, Int> = emptyMap()
    )
}
