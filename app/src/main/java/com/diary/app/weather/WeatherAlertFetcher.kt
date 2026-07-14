package com.diary.app.weather

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 天气预警抓取器（独立于每小时天气刷新）。
 *
 * 数据源策略（可插拔）：
 *  - 默认：中央气象台 nmc.cn（完全免费、官方权威、无需任何 Key）。
 *  - 若下方 QWEATHER_API_KEY 填入了和风天气免费 Key，则自动改用和风结构化接口（更干净、字段更完整）。
 *
 * nmc 接口返回全国预警列表，每条 alertId 的前 6 位即发布地的 adcode，
 * 因此可按用户所在地的 adcode 前缀精确过滤，避免收到无关地区的预警。
 */
object WeatherAlertFetcher {

    private const val TAG = "WeatherAlertFetcher"

    // 中央气象台预警列表接口（https，避免明文被系统拦截）
    // 注意：不要拼接 province 参数——实测带 province 会让 nmc 返回 500，
    // 改为拉取全国所有预警后按 adcode 前缀在客户端过滤（见下方 cityPrefix/provPrefix）。
    // pageSize=500 + 遍历所有分页，确保不遗漏（全国常驻 2000+ 条活跃预警）。

    // ===== 可插拔：和风天气（留空则用 nmc）=====
    private const val QWEATHER_API_KEY = "" // 填入和风天气免费 Key 后自动启用
    private const val QWEATHER_API_HOST = "https://api.qweather.com"
    // ===========================================

    // adcode 前 2 位 -> 省份名（用于 nmc 的 province 过滤参数）
    private val PROVINCE_BY_CODE = mapOf(
        "11" to "北京", "12" to "天津", "13" to "河北", "14" to "山西", "15" to "内蒙古",
        "21" to "辽宁", "22" to "吉林", "23" to "黑龙江", "31" to "上海", "32" to "江苏",
        "33" to "浙江", "34" to "安徽", "35" to "福建", "36" to "江西", "37" to "山东",
        "41" to "河南", "42" to "湖北", "43" to "湖南", "44" to "广东", "45" to "广西",
        "46" to "海南", "50" to "重庆", "51" to "四川", "52" to "贵州", "53" to "云南",
        "54" to "西藏", "61" to "陕西", "62" to "甘肃", "63" to "青海", "64" to "宁夏",
        "65" to "新疆", "71" to "台湾", "81" to "香港", "82" to "澳门"
    )

    suspend fun fetchAlerts(context: Context): List<WeatherAlert> = withContext(Dispatchers.IO) {
        return@withContext if (QWEATHER_API_KEY.isNotBlank()) {
            try {
                fetchFromQWeather(context)
            } catch (e: Exception) {
                Log.w(TAG, "QWeather failed, fallback to nmc", e)
                fetchFromNmc(context)
            }
        } else {
            fetchFromNmc(context)
        }
    }

    // ---------- 中央气象台（默认，零配置）----------
    private fun fetchFromNmc(context: Context): List<WeatherAlert> {
        val (adcode, cityName) = WeatherManager.getAdcode(context) ?: return emptyList()
        val province = PROVINCE_BY_CODE[adcode.take(2)] ?: return emptyList()
        Log.d(TAG, "nmc: 定位 adcode=$adcode, city=$cityName, province=$province")

        val result = mutableListOf<WeatherAlert>()
        val cityPrefix = adcode.take(4)   // 市级（含下属区县）
        val provPrefix = adcode.take(2)   // 省级
        var pageNo = 1
        var totalAlerts = 0

        try {
            // 遍历所有分页，确保不遗漏用户的预警（全国常驻 2000+ 条预警，单页不够）
            while (true) {
                val url = "https://www.nmc.cn/rest/findAlarm?pageNo=$pageNo&pageSize=500"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "DiaryApp/1.0")

                val json: JSONObject
                try {
                    if (conn.responseCode != 200) throw java.io.IOException("nmc response code ${conn.responseCode}")
                    json = JSONObject(conn.inputStream.bufferedReader().readText())
                } finally {
                    conn.disconnect()
                }

                val data = json.optJSONObject("data") ?: break
                val pageInfo = data.optJSONObject("page")
                val list = data.optJSONArray("list") ?: break
                val total = pageInfo?.optInt("count", 0) ?: 0
                if (pageNo == 1) totalAlerts = total

                for (i in 0 until list.length()) {
                    val item = list.getJSONObject(i)
                    val alertId = item.optString("alertid", "")
                    if (alertId.isBlank()) continue

                    // alertId 形如 53092341600000_20260708221121，前 6 位为发布地 adcode
                    val idPrefix = alertId.take(6)
                    val isProvinceLevel = idPrefix.length == 6 && idPrefix.drop(2).all { it == '0' }
                    val matches = idPrefix.startsWith(cityPrefix) ||
                            (isProvinceLevel && idPrefix.take(2) == provPrefix)
                    if (!matches) continue

                    val title = item.optString("title", "")
                    val (type, level) = parseTitle(title)
                    if (type.isBlank()) {
                        Log.w(TAG, "nmc: 标题解析失败（跳过）: $title")
                        continue
                    }

                    result.add(
                        WeatherAlert(
                            alertId = alertId,
                            province = province,
                            city = extractLocation(title) ?: cityName,
                            level = level,
                            type = type,
                            text = title,
                            publishTime = parsePublishTimeFromAlertId(alertId),
                            source = "中央气象台"
                        )
                    )
                }

                // 判断是否还有下一页
                val next = pageInfo?.optInt("next", -1) ?: -1
                if (next <= pageNo) break
                pageNo = next
            }
            Log.d(TAG, "nmc: 遍历 $pageNo 页(共 $totalAlerts 条预警)，命中 ${result.size} 条 ($province/$cityName, 前缀 $cityPrefix)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch nmc alerts (page=$pageNo, adcode=$adcode)", e)
            throw e
        }
        return result
    }

    private fun parseTitle(title: String): Pair<String, String> {
        // 例："江西省宜春市铜鼓县气象台发布雷电黄色预警信号"
        val level = Regex("(红|橙|黄|蓝|未知)色预警").find(title)
            ?.groupValues?.get(1)?.let { it + "色" } ?: ""
        val type = Regex("发布(.+?)(红|橙|黄|蓝|未知)?色预警").find(title)
            ?.groupValues?.get(1)?.trim() ?: ""
        return type to level
    }

    private fun extractLocation(title: String): String? {
        val idx = title.indexOf("气象台")
        if (idx <= 0) return null
        return title.substring(0, idx).takeIf { it.isNotBlank() }
    }

    /**
     * nmc 的 alertId 形如 53092341600000_20260708221121，
     * 下划线后的 14 位数字为发布时间 YYYYMMDDHHMMSS，直接解析为可读格式。
     */
    private fun parsePublishTimeFromAlertId(alertId: String): String {
        val ts = alertId.substringAfter('_', "").takeIf { it.length == 14 } ?: return ""
        return try {
            val y = ts.substring(0, 4)
            val m = ts.substring(4, 6)
            val d = ts.substring(6, 8)
            val hh = ts.substring(8, 10)
            val mm = ts.substring(10, 12)
            val ss = ts.substring(12, 14)
            "$y-$m-$d $hh:$mm:$ss"
        } catch (_: Exception) { "" }
    }

    // ---------- 和风天气（填入 Key 后启用）----------
    private fun fetchFromQWeather(context: Context): List<WeatherAlert> {
        val (lon, lat) = WeatherManager.getLocationCoordinates(context) ?: return emptyList()
        val url = "$QWEATHER_API_HOST/weatheralert/v1/current/$lat/$lon?localTime=true&lang=zh"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("Authorization", "Bearer $QWEATHER_API_KEY")
        conn.setRequestProperty("User-Agent", "DiaryApp/1.0")
        val result = mutableListOf<WeatherAlert>()
        try {
            if (conn.responseCode != 200) throw java.io.IOException("QWeather response code ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val alerts = json.optJSONArray("alerts") ?: return emptyList()
            for (i in 0 until alerts.length()) {
                val a = alerts.getJSONObject(i)
                val id = a.optString("id", "")
                val event = a.optJSONObject("eventType")?.optString("name", "") ?: ""
                val level = when (a.optString("severity", "")) {
                    "extreme" -> "红色"
                    "severe" -> "橙色"
                    "moderate" -> "黄色"
                    "minor" -> "蓝色"
                    else -> ""
                }
                val text = a.optString("description", "").ifBlank { a.optString("headline", "") }
                if (event.isBlank()) continue
                result.add(
                    WeatherAlert(
                        alertId = id,
                        province = "",
                        city = "",
                        level = level,
                        type = event,
                        text = text,
                        publishTime = a.optString("pubTime", ""),
                        source = "和风天气"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch QWeather alerts", e)
            throw e
        } finally {
            conn.disconnect()
        }
        return result
    }
}
