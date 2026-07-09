package com.diary.app.weather

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 当前生效中的天气预警持久化仓库。
 *
 * 与"收件箱 / 系统推送"完全解耦：无论用户如何设置通知开关，
 * 只要预警检测开启，[WeatherAlertWorker] 每次巡检都会把"当前匹配到的预警"
 * 覆盖写入此处，首页横幅据此展示，保证首页始终能呈现最新预警。
 */
object WeatherAlertStore {

    private const val PREFS_NAME = "weather_alert_active"
    private const val KEY_ACTIVE = "active_alerts"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveActiveAlerts(context: Context, alerts: List<WeatherAlert>) {
        val arr = JSONArray().apply {
            alerts.forEach { a ->
                put(JSONObject().apply {
                    put("alertId", a.alertId)
                    put("province", a.province)
                    put("city", a.city)
                    put("level", a.level)
                    put("type", a.type)
                    put("text", a.text)
                    put("publishTime", a.publishTime)
                    put("source", a.source)
                })
            }
        }
        getPrefs(context).edit().putString(KEY_ACTIVE, arr.toString()).apply()
    }

    fun getActiveAlerts(context: Context): List<WeatherAlert> {
        val raw = getPrefs(context).getString(KEY_ACTIVE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                WeatherAlert(
                    alertId = o.optString("alertId", ""),
                    province = o.optString("province", ""),
                    city = o.optString("city", ""),
                    level = o.optString("level", ""),
                    type = o.optString("type", ""),
                    text = o.optString("text", ""),
                    publishTime = o.optString("publishTime", ""),
                    source = o.optString("source", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getAlertById(context: Context, alertId: String): WeatherAlert? =
        getActiveAlerts(context).firstOrNull { it.alertId == alertId }

    // ── 上次巡检记录（供"是否生效"可见性展示）──────────────
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val KEY_LAST_CHECK_SUCCESS = "last_check_success"
    private const val KEY_LAST_CHECK_COUNT = "last_check_count"
    private const val KEY_LAST_CHECK_ERROR = "last_check_error"

    data class LastCheck(
        val timeMs: Long,
        val success: Boolean,
        val count: Int,
        val error: String?
    )

    fun recordCheck(context: Context, success: Boolean, count: Int, error: String? = null) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .putBoolean(KEY_LAST_CHECK_SUCCESS, success)
            .putInt(KEY_LAST_CHECK_COUNT, count)
            .putString(KEY_LAST_CHECK_ERROR, error)
            .apply()
    }

    fun getLastCheck(context: Context): LastCheck? {
        val prefs = getPrefs(context)
        val time = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        if (time == 0L) return null
        return LastCheck(
            timeMs = time,
            success = prefs.getBoolean(KEY_LAST_CHECK_SUCCESS, false),
            count = prefs.getInt(KEY_LAST_CHECK_COUNT, 0),
            error = prefs.getString(KEY_LAST_CHECK_ERROR, null)
        )
    }

    /** 人类可读的"上次检查"摘要，供设置页展示。 */
    fun getLastCheckSummary(context: Context): String {
        val lc = getLastCheck(context) ?: return "尚未检查"
        val minsAgo = ((System.currentTimeMillis() - lc.timeMs) / 60000).coerceAtLeast(0)
        val when_ = when {
            minsAgo < 1 -> "刚刚"
            minsAgo < 60 -> "${minsAgo}分钟前"
            else -> "${minsAgo / 60}小时前"
        }
        return if (lc.success) {
            "$when_ 检查 · 命中 ${lc.count} 条"
        } else {
            "$when_ 检查失败 · ${lc.error ?: "未知原因"}"
        }
    }
}
