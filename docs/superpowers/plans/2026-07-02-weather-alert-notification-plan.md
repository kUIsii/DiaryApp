# 天气预警 & 通知系统改进 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现天气预警在首页可见 + App 通知收件箱记录 + 系统推送双路通知

**Architecture:** 修改 4 个文件：升级 NotificationViewModel 中的 WeatherAlertNotification 数据模型以存储真实预警内容；在 WeatherWorker 中加入 DAO 写入实现双路推送并调整为 1h 刷新；在 HomeScreen 天气行增加预警标签+展开收起交互

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager

---

### Task 1: 升级 WeatherAlertNotification 数据模型

**文件:**
- Modify: `ui/notification/NotificationViewModel.kt:83-89`

- [ ] **Step 1: 替换 WeatherAlertNotification 定义**

```kotlin
// 替换第 83-89 行
data class WeatherAlertNotification(
    val weatherCity: String,
    val alertLevel: String,
    val alertType: String,
    val alertText: String,
    override val id: String = "weather_alert_${System.currentTimeMillis()}",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()
```

### Task 2: 更新 toEntity() —— 写入通知表

**文件:**
- Modify: `ui/notification/NotificationViewModel.kt:490-497`

- [ ] **Step 1: 替换 WeatherAlertNotification 的 toEntity 分支**

```kotlin
// 替换第 490-497 行
is WeatherAlertNotification -> NotificationMeta(
    type = "weather_alert",
    title = "${alertLevel}预警 · ${alertType}",
    subtitle = alertText,
    iconType = "thunderstorm",
    colorHex = when (alertLevel) {
        "红色" -> 0xFFDC2626
        "橙色" -> 0xFFEA580C
        "黄色" -> 0xFFF59E0B
        "蓝色" -> 0xFF3B82F6
        else -> 0xFFE53935
    },
    relatedId = null
)
```

### Task 3: 更新 toNotificationItem() —— 从通知表读出

**文件:**
- Modify: `ui/notification/NotificationViewModel.kt:601-611`

- [ ] **Step 1: 替换 weather_alert 反解析分支**

```kotlin
// 替换第 601-611 行
"weather_alert" -> {
    val parts = title.split("预警 · ")
    val level = if (parts.size >= 2) parts[0] else ""
    val type = if (parts.size >= 2) parts[1] else title
    WeatherAlertNotification(
        weatherCity = "",
        alertLevel = level,
        alertType = type,
        alertText = subtitle,
        id = id,
        timestamp = createdAt
    )
}
```

### Task 4: WeatherWorker 双路推送 + 1h 间隔

**文件:**
- Modify: `weather/WeatherWorker.kt:21-32` (doWork), `weather/WeatherWorker.kt:79` (schedule)

- [ ] **Step 1: 修改 WeatherWorker.doWork()**

在 `doWork()` 中，当检测到预警时，先写入 Room 通知表，再检查开关决定是否发系统推送：

```kotlin
// 替换第 21-32 行
override suspend fun doWork(): Result {
    return try {
        val weather = WeatherManager.fetchWeather(applicationContext)
        if (weather != null && weather.alerts.isNotEmpty()) {
            val app = applicationContext as com.diary.app.DiaryApplication
            val dao = app.database.diaryDao()
            for (alert in weather.alerts) {
                val entity = com.diary.app.data.NotificationEntity(
                    id = "weather_alert_${System.currentTimeMillis()}_${alert.hashCode()}",
                    type = "weather_alert",
                    title = "${alert.level}预警 · ${alert.type}",
                    subtitle = alert.text,
                    iconType = "thunderstorm",
                    colorHex = when (alert.level) {
                        "红色" -> 0xFFDC2626L
                        "橙色" -> 0xFFEA580CL
                        "黄色" -> 0xFFF59E0BL
                        "蓝色" -> 0xFF3B82F6L
                        else -> 0xFFE53935L
                    },
                    relatedId = null,
                    createdAt = System.currentTimeMillis()
                )
                dao.insertNotification(entity)
            }
            if (com.diary.app.reminder.NotificationPreferencesManager.isWeatherAlertsEnabled(applicationContext)) {
                sendAlerts(weather.alerts, weather.city)
            }
        }
        Result.success()
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Weather refresh failed", e)
        Result.retry()
    }
}
```

- [ ] **Step 2: 修改 schedule()，将间隔从 3 小时改为 1 小时**

```kotlin
// 替换第 79 行
val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS).build()
```

### Task 5: 首页天气行增加预警标签

**文件:**
- Modify: `ui/home/HomeScreen.kt:628-675`

这个改动较大。当前天气行逻辑在 `HomeHeroSection` composable 内（约第 562-675 行），需要修改为：当 `currentWeather.alerts` 非空时，在天气行右侧显示预警标签，点击展开/收起原文。

先在 HomeScreen.kt 顶部 import 区域确认有必要的 import，然后修改天气行区域。

- [ ] **Step 1: 添加 import**

```kotlin
// 在 HomeScreen.kt import 区域（约第 102-103 行附近）添加：
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
```

- [ ] **Step 2: 修改天气行区块**

将 `HomeHeroSection` 中约第 628-675 行的天气行代码替换为带预警标签的版本：

```kotlin
// 替换第 628-675 行
// Weather row (separate, stable layout)
if (currentWeather != null && currentWeather.weather.isNotBlank()) {
    var alertExpanded by remember { mutableStateOf(false) }
    val hasAlerts = currentWeather.alerts.isNotEmpty()
    val firstAlert = if (hasAlerts) currentWeather.alerts.first() else null

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .clickable { onWeatherClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = com.diary.app.ui.components.weatherIconForType(
                    com.diary.app.weather.WeatherManager.mapAmapWeatherToType(currentWeather.weather)
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${currentWeather.city} · ${currentWeather.weather} ${currentWeather.temperature}°C",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasAlerts && firstAlert != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .clickable { alertExpanded = !alertExpanded }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${firstAlert.level}预警 · ${firstAlert.type}",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = alertExpanded && firstAlert != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (firstAlert != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.06f))
                        .clickable { alertExpanded = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = firstAlert.text,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} else if (!isWeatherEnabled) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .clickable { onWeatherToggle(true) }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "查看天气",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
```

### Task 6: 构建验证

- [ ] **Step 1: 运行 gradlew assembleExperimentalDebug**

```bash
./gradlew assembleExperimentalDebug
```

Expected: BUILD SUCCESSFUL

如果编译失败，根据错误信息修正 import。
