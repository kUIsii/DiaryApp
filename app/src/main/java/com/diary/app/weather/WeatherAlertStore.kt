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
}
