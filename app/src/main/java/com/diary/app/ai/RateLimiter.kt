package com.diary.app.ai

import android.content.Context
import java.time.LocalDate

class RateLimiter(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "diary_prefs"
        private const val KEY_DAILY_DATE = "ai_rate_date"
        private const val KEY_DAILY_TOTAL = "ai_rate_total"
        private const val KEY_MODEL_USAGE = "ai_rate_models"  // JSON: {"model1": 50, "model2": 30}
        private const val DAILY_LIMIT = 2000
        private const val MODEL_LIMIT = 200
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class UsageStats(
        val dailyTotal: Int,
        val dailyLimit: Int = DAILY_LIMIT,
        val modelUsage: Map<String, Int>,
        val modelLimit: Int = MODEL_LIMIT
    )

    fun canMakeRequest(model: String): Boolean {
        resetIfNewDay()
        val total = prefs.getInt(KEY_DAILY_TOTAL, 0)
        if (total >= DAILY_LIMIT) return false
        val modelCount = getModelUsage(model)
        if (modelCount >= MODEL_LIMIT) return false
        return true
    }

    fun recordRequest(model: String) {
        resetIfNewDay()
        val total = prefs.getInt(KEY_DAILY_TOTAL, 0)
        prefs.edit().putInt(KEY_DAILY_TOTAL, total + 1).apply()

        val modelUsage = getModelUsageMap().toMutableMap()
        modelUsage[model] = (modelUsage[model] ?: 0) + 1
        val json = com.google.gson.Gson().toJson(modelUsage)
        prefs.edit().putString(KEY_MODEL_USAGE, json).apply()
    }

    fun getUsageStats(): UsageStats {
        resetIfNewDay()
        return UsageStats(
            dailyTotal = prefs.getInt(KEY_DAILY_TOTAL, 0),
            modelUsage = getModelUsageMap()
        )
    }

    private fun getModelUsage(model: String): Int {
        return getModelUsageMap()[model] ?: 0
    }

    private fun getModelUsageMap(): Map<String, Int> {
        val json = prefs.getString(KEY_MODEL_USAGE, "{}") ?: "{}"
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                Map::class.java, String::class.java, Integer::class.java
            ).type
            val raw: Map<String, Int> = com.google.gson.Gson().fromJson(json, type)
            raw.mapValues { it.value.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun resetIfNewDay() {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_DAILY_DATE, "") ?: ""
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DAILY_DATE, today)
                .putInt(KEY_DAILY_TOTAL, 0)
                .putString(KEY_MODEL_USAGE, "{}")
                .apply()
        }
    }
}
