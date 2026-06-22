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
    private val gson = com.google.gson.Gson()

    data class UsageStats(
        val dailyTotal: Int,
        val dailyLimit: Int = DAILY_LIMIT,
        val modelUsage: Map<String, Int>,
        val modelLimit: Int = MODEL_LIMIT
    )

    @Synchronized
    fun canMakeRequest(model: String): Boolean {
        resetIfNewDay()
        val total = prefs.getInt(KEY_DAILY_TOTAL, 0)
        val modelCount = getModelUsage(model)
        return canMakeRequestInternal(total, modelCount)
    }

    @Synchronized
    fun recordRequest(model: String) {
        resetIfNewDay()
        val total = prefs.getInt(KEY_DAILY_TOTAL, 0)
        prefs.edit().putInt(KEY_DAILY_TOTAL, total + 1).apply()

        val modelUsage = getModelUsageMap().toMutableMap()
        modelUsage[model] = (modelUsage[model] ?: 0) + 1
        val json = gson.toJson(modelUsage)
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
            val raw: Map<String, Int> = gson.fromJson(json, type)
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

internal fun canMakeRequestInternal(
    dailyTotal: Int,
    modelCount: Int,
    dailyLimit: Int = 2000,
    modelLimit: Int = 200
): Boolean {
    if (dailyTotal >= dailyLimit) return false
    if (modelCount >= modelLimit) return false
    return true
}
