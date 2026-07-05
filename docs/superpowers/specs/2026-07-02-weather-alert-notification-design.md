# 天气预警 & 通知系统改进设计

**Version:** v2.78.11-experimental (planned)
**Date:** 2026-07-02
**Status:** Draft

---

## Motivation

现有天气功能存在三个问题：

1. **预警不可见** — 高德 API 已返回 `warnings` 数据，但首页完全不展示任何预警信息，用户不知道自己所在区域有恶劣天气预警
2. **双路通知缺失** — App 内通知收件箱已有 `WeatherAlertNotification` 类型和 `WEATHER_ALERT` 分类，但实际没有任何代码将预警写入 Room 通知表，导致收件箱里永远看不到天气预警
3. **`WeatherAlertNotification` 存的是无关数据** — 当前模型存储的是 "北京 · 晴 25°C"（天气概况），而非预警相关的等级、类型、原文

---

## 改动范围

### R1: 数据模型升级

**文件：** `NotificationViewModel.kt`

```kotlin
// 改前
data class WeatherAlertNotification(
    val weatherCity: String,
    val weatherDesc: String,    // "晴"（与预警无关）
    val temperature: String,    // "25"（与预警无关）
)

// 改后
data class WeatherAlertNotification(
    val weatherCity: String,   // "北京"
    val alertLevel: String,    // "黄色"
    val alertType: String,     // "暴雨"
    val alertText: String,     // 预警原文全文
)
```

- `toEntity()` 对应更新：`title = "${alertLevel}预警 · ${alertType}"`，`subtitle = alertText`
- `toNotificationItem()` 反解析同步更新

### R2: WeatherWorker 双路推送

**文件：** `WeatherWorker.kt`

```
doWork():
  1. fetchWeather()（不变）
  2. if alerts.isNotEmpty:
     a. 遍历 alerts，构造 NotificationEntity，通过 DAO 写入 Room 表（无条件）
     b. 检查 NotificationPreferencesManager.isWeatherAlertsEnabled()
         → 开启：发系统通知（现有 sendAlerts 逻辑）
         → 关闭：跳过系统通知，已在收件箱留底
```

- 获取 DAO：`(applicationContext as DiaryApplication).database.diaryDao()`
- 写入使用 `dao.insertNotification(entity)`
- 注意 Worker 在后台线程运行，Room 写入是同步操作，无需额外协程
- 确保 `WeatherWorker.ensureChannel()` 已经是启动时调用，渠道已存在

### R3: 首页预警可见

**文件：** `HomeScreen.kt`

天气行区域扩展为 3 种状态：

| 条件 | 显示内容 |
|---|---|
| `currentWeather == null` 或未启用 | "查看天气"（不变） |
| `currentWeather != null`，`alerts` 为空 | 天气行（不变） |
| `currentWeather != null`，`alerts` 非空 | 天气行 + 预警标签 |

预警标签：
- 圆角矩形，浅红背景 + 红色文字
- 显示 `alert.level + "预警 · " + alert.type`（如 "黄色预警 · 暴雨"）
- 点击标签：展开/收起 `alert.text` 原文（inline 展开，使用 AnimatedVisibility）
- 天气行原有区域点击：仍跳转 `WeatherDetailScreen`

### R4: 通知设置

**文件：** `NotificationPreferencesManager.kt`

`KEY_WEATHER_ALERTS_ENABLED` 含义变更为：**控制是否发送系统推送通知**。`WeatherWorker` 中检查此开关。

App 内部通知收件箱不受此开关影响，始终记录。

ProfileScreen 上开关文案不变，仍为 "天气预警"。但开关功能从"是否开启天气预警"变更为"是否接收天气预警系统推送"——对应的说明文字改为："开启后，恶劣天气预警将通过系统通知推送。已收到的预警可在通知中心查看。"

### R5: 刷新频率

**文件：** `WeatherWorker.kt`

- `PeriodicWorkRequest` 间隔从 3 小时改为 1 小时
- 启动时 `DiaryApplication.onCreate()` 已有 `WeatherWorker.schedule(this)` 包含 OneTimeWorkRequest 立即执行

---

## 不做的范围

- 不改变首页天气的启用/定位权限逻辑
- 不调整其他通知类型（月报/年报/胶囊/今日回顾等）的行为
- 不改动 `WeatherManager` 的数据获取逻辑
- 不改动 `NotificationViewModel` 的其他生成逻辑

---

## API 调用评估

| 服务 | 调用频率 | 月调用量 | 个人免费额度 | 费用 |
|---|---|---|---|---|
| 天气查询（Web API） | 1次/小时 | ~720次 | 5,000次/月 | 免费 |
| 逆地理编码（获取 adcode） | 1次/小时 | ~720次 | 150,000次/月 | 免费 |

结论：个人使用场景下完全免费。

---

## 涉及文件清单

| 文件 | 改动类型 |
|---|---|
| `ui/notification/NotificationViewModel.kt` | 修改 WeatherAlertNotification、toEntity、toNotificationItem |
| `weather/WeatherWorker.kt` | 新增 DAO 写入、刷新频率改为 1h、检查通知开关 |
| `ui/home/HomeScreen.kt` | 天气行增加预警标签 + 展开收起 |
| `reminder/NotificationPreferencesManager.kt` | 开关语义调整（可选，基本够用） |
