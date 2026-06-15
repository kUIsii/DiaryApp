package com.diary.app.ai

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiUsageTracker {
    private const val PREFS_NAME = "ai_usage"
    private const val KEY_PREFIX_REQUESTS = "req_"
    private const val KEY_PREFIX_TOKENS = "tok_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun record(context: Context, tokens: Int) {
        val key = todayKey()
        val p = prefs(context)
        p.edit()
            .putInt(KEY_PREFIX_REQUESTS + key, p.getInt(KEY_PREFIX_REQUESTS + key, 0) + 1)
            .putInt(KEY_PREFIX_TOKENS + key, p.getInt(KEY_PREFIX_TOKENS + key, 0) + tokens)
            .apply()
    }

    fun getTodayStats(context: Context): UsageStats {
        val key = todayKey()
        val p = prefs(context)
        return UsageStats(
            requests = p.getInt(KEY_PREFIX_REQUESTS + key, 0),
            tokens = p.getInt(KEY_PREFIX_TOKENS + key, 0)
        )
    }

    data class UsageStats(val requests: Int, val tokens: Int)
}
