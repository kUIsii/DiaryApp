@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.LocalExtendedColors
import com.diary.app.weather.WeatherAlert
import com.diary.app.weather.WeatherAlertFetcher
import com.diary.app.weather.WeatherAlertStore
import com.diary.app.weather.WeatherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class AlertListState(
    val isLoading: Boolean = false,
    val adcode: String = "",
    val cityName: String = "",
    val province: String = "",
    val totalNationwide: Int = 0,
    val pagesFetched: Int = 0,
    val matchedCount: Int = 0,
    val alerts: List<WeatherAlert> = emptyList(),
    val error: String? = null,
    val lastCheck: WeatherAlertStore.LastCheck? = null
)

@Composable
fun WeatherAlertListScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(AlertListState()) }

    LaunchedEffect(Unit) {
        state = state.copy(isLoading = true)
        withContext(Dispatchers.IO) {
            try {
                val (adcode, cityName) = WeatherManager.getAdcode(context)
                    ?: Pair("unknown", "未知")
                val province = mapOf(
                    "11" to "北京", "12" to "天津", "13" to "河北", "14" to "山西", "15" to "内蒙古",
                    "21" to "辽宁", "22" to "吉林", "23" to "黑龙江", "31" to "上海", "32" to "江苏",
                    "33" to "浙江", "34" to "安徽", "35" to "福建", "36" to "江西", "37" to "山东",
                    "41" to "河南", "42" to "湖北", "43" to "湖南", "44" to "广东", "45" to "广西",
                    "46" to "海南", "50" to "重庆", "51" to "四川", "52" to "贵州", "53" to "云南",
                    "54" to "西藏", "61" to "陕西", "62" to "甘肃", "63" to "青海", "64" to "宁夏",
                    "65" to "新疆", "71" to "台湾", "81" to "香港", "82" to "澳门"
                )[adcode.take(2)] ?: "未知"

                state = state.copy(
                    adcode = adcode,
                    cityName = cityName,
                    province = province
                )

                // Direct fetch from nmc, paginate through all pages
                val alerts = mutableListOf<WeatherAlert>()
                val cityPrefix = adcode.take(4)
                val provPrefix = adcode.take(2)
                var pageNo = 1
                var totalNationwide = 0
                var pagesFetched = 0

                while (true) {
                    val url = "https://www.nmc.cn/rest/findAlarm?pageNo=$pageNo&pageSize=500"
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("User-Agent", "DiaryApp/1.0")

                    val json: JSONObject
                    try {
                        if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
                        json = JSONObject(conn.inputStream.bufferedReader().readText())
                    } finally { conn.disconnect() }

                    val data = json.optJSONObject("data") ?: break
                    val pageInfo = data.optJSONObject("page")
                    val list = data.optJSONArray("list") ?: break
                    if (pageNo == 1) totalNationwide = pageInfo?.optInt("count", 0) ?: 0
                    pagesFetched++

                    for (i in 0 until list.length()) {
                        val item = list.getJSONObject(i)
                        val alertId = item.optString("alertid", "")
                        if (alertId.isBlank()) continue

                        val idPrefix = alertId.take(6)
                        val isProvinceLevel = idPrefix.length == 6 && idPrefix.drop(2).all { it == '0' }
                        val matches = idPrefix.startsWith(cityPrefix) ||
                                (isProvinceLevel && idPrefix.take(2) == provPrefix)
                        if (!matches) continue

                        val title = item.optString("title", "")
                        val level = Regex("(红|橙|黄|蓝|未知)色预警").find(title)
                            ?.groupValues?.get(1)?.let { it + "色" } ?: ""
                        val type = Regex("发布(.+?)(红|橙|黄|蓝|未知)?色预警").find(title)
                            ?.groupValues?.get(1)?.trim() ?: ""
                        if (type.isBlank()) continue

                        val locIdx = title.indexOf("气象台")
                        val location = if (locIdx > 0) title.substring(0, locIdx) else cityName

                        val ts = alertId.substringAfter('_', "").takeIf { it.length == 14 } ?: ""
                        val pubTime = if (ts.length == 14)
                            "${ts.substring(0,4)}-${ts.substring(4,6)}-${ts.substring(6,8)} ${ts.substring(8,10)}:${ts.substring(10,12)}:${ts.substring(12,14)}"
                        else ""

                        alerts.add(WeatherAlert(
                            alertId = alertId, province = province, city = location,
                            level = level, type = type, text = title,
                            publishTime = pubTime, source = "中央气象台"
                        ))
                    }

                    val next = pageInfo?.optInt("next", -1) ?: -1
                    if (next <= pageNo) break
                    pageNo = next
                }

                state = state.copy(
                    isLoading = false,
                    totalNationwide = totalNationwide,
                    pagesFetched = pagesFetched,
                    matchedCount = alerts.size,
                    alerts = alerts.sortedByDescending { alertSeverity(it.level) },
                    lastCheck = WeatherAlertStore.getLastCheck(context)
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "未知错误",
                    lastCheck = WeatherAlertStore.getLastCheck(context)
                )
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.primary

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PageHeader(title = "天气预警", onNavigateBack = onNavigateBack)

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Diagnostic card
                    item(key = "diag") {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("诊断信息", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = accent)
                                Spacer(Modifier.height(8.dp))
                                DiagnosticRow("定位城市", "${state.cityName}  ${state.province}", textColor, textTertiary)
                                DiagnosticRow("adcode", state.adcode, textColor, textTertiary)
                                DiagnosticRow("全国预警总数", "${state.totalNationwide} 条", textColor, textTertiary)
                                DiagnosticRow("已翻阅页数", "${state.pagesFetched} 页", textColor, textTertiary)
                                DiagnosticRow("本地命中", "${state.matchedCount} 条", if (state.matchedCount > 0) accent else MaterialTheme.colorScheme.error, textTertiary)
                                if (state.lastCheck != null) {
                                    val mins = ((System.currentTimeMillis() - state.lastCheck!!.timeMs) / 60000).coerceAtLeast(0)
                                    val whenStr = when { mins < 1 -> "刚刚"; mins < 60 -> "${mins}分钟前"; else -> "${mins/60}小时前" }
                                    DiagnosticRow("上次巡检", "$whenStr · ${if (state.lastCheck!!.success) "成功" else "失败"} · ${state.lastCheck!!.count}条", textColor, textTertiary)
                                }
                                if (state.error != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("错误: ${state.error}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Section header
                    if (state.alerts.isNotEmpty()) {
                        item(key = "header") {
                            Text(
                                "本地预警 · ${state.matchedCount} 条",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        item(key = "empty") {
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("当前地区无活跃预警", fontSize = 14.sp, color = textSecondary)
                                    if (state.adcode == "110101") {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "adcode 为北京市东城区(110101)，可能未获取到真实定位。请检查定位权限是否开启。",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Alert list
                    items(state.alerts, key = { it.alertId }) { alert ->
                        AlertCard(alert = alert, accent = accent, textColor = textColor, textSecondary = textSecondary)
                    }

                    // Refresh button
                    item(key = "refresh") {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    state = state.copy(isLoading = true, error = null)
                                    // Re-trigger the LaunchedEffect by changing a key
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("重新加载", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = labelColor.copy(alpha = 0.7f))
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AlertCard(
    alert: WeatherAlert,
    accent: Color,
    textColor: Color,
    textSecondary: Color
) {
    val levelColor = alertLevelColor(alert.level)
    val bgAlpha = when (alert.level) { "红色" -> 0.12f; "橙色" -> 0.09f; "黄色" -> 0.06f; else -> 0.04f }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(levelColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${alert.level}${alert.type}预警",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = levelColor
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                alert.text,
                fontSize = 13.sp,
                color = textColor,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        modifier = Modifier.size(12.dp), tint = textSecondary)
                    Spacer(Modifier.width(2.dp))
                    Text(alert.city, fontSize = 12.sp, color = textSecondary)
                }
                if (alert.publishTime.isNotBlank()) {
                    Text(alert.publishTime, fontSize = 12.sp, color = textSecondary)
                }
            }
            if (alert.type.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Text(
                        alertGuidance(alert.type),
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
