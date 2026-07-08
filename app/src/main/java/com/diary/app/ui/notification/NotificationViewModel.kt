package com.diary.app.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import com.diary.app.data.NotificationEntity
import com.diary.app.data.TimeCapsule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// region 分类枚举

enum class NotificationCategory(val label: String) {
    ALL("全部"),
    WEATHER_ALERT("天气预警"),
    MONTHLY_REPORT("月报"),
    ANNUAL_REPORT("年报"),
    TIME_CAPSULE("胶囊"),
    MILESTONE("里程碑"),
    ON_THIS_DAY("今日回顾"),
    INACTIVITY("写作提醒"),
    WEEKLY_SUMMARY("周报")
}

// endregion

// region NotificationItem sealed class

sealed class NotificationItem {
    abstract val id: String
    abstract val timestamp: Long
}

data class CapsuleUnlockNotification(
    val capsule: TimeCapsule,
    override val id: String = "capsule_${capsule.id}",
    override val timestamp: Long = capsule.unlockDate
) : NotificationItem()

data class OnThisDayNotification(
    val entry: DiaryPreview,
    val yearsAgo: Int,
    override val id: String = "onthisday_${entry.id}",
    override val timestamp: Long = entry.createdAt
) : NotificationItem()

data class MilestoneNotification(
    val title: String,
    val subtitle: String,
    override val id: String = "milestone_$title",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class StreakNotification(
    val days: Int,
    override val id: String = "streak_$days",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class AnnualReportNotification(
    val year: Int,
    override val id: String = "annual_$year",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class MonthlyReportNotification(
    val year: Int,
    val month: Int,
    val entryCount: Int,
    val wordCount: Int,
    override val id: String = "monthly_${year}_$month",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class WeatherAlertNotification(
    val weatherCity: String,
    val alertLevel: String,
    val alertType: String,
    val alertText: String,
    val alertId: String = "",
    override val id: String = "weather_alert_${System.currentTimeMillis()}",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class InactivityNotification(
    val daysSinceLastEntry: Int,
    override val id: String = "inactivity_${System.currentTimeMillis() / (24*60*60*1000)}",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

data class WeeklySummaryNotification(
    val entryCount: Int,
    val wordCount: Int,
    val topWeather: String?,
    override val id: String = "weekly_summary_${System.currentTimeMillis() / (24*60*60*1000)}",
    override val timestamp: Long = System.currentTimeMillis()
) : NotificationItem()

// endregion

// region 分类映射

val NotificationItem.category: NotificationCategory
    get() = when (this) {
        is CapsuleUnlockNotification -> NotificationCategory.TIME_CAPSULE
        is OnThisDayNotification -> NotificationCategory.ON_THIS_DAY
        is MilestoneNotification -> NotificationCategory.MILESTONE
        is StreakNotification -> NotificationCategory.MILESTONE
        is AnnualReportNotification -> NotificationCategory.ANNUAL_REPORT
        is MonthlyReportNotification -> NotificationCategory.MONTHLY_REPORT
        is WeatherAlertNotification -> NotificationCategory.WEATHER_ALERT
        is InactivityNotification -> NotificationCategory.INACTIVITY
        is WeeklySummaryNotification -> NotificationCategory.WEEKLY_SUMMARY
    }

// endregion

// region UI State

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val selectedCategory: NotificationCategory = NotificationCategory.ALL,
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val showTrash: Boolean = false,
    val trashedNotifications: List<NotificationItem> = emptyList()
)

// endregion

// region ViewModel

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("notification_cache", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // 保留旧接口兼容（NotificationScreen 目前使用 notifications）
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    init {
        // 同步 uiState 到 notifications 兼容旧接口
        viewModelScope.launch {
            _uiState.collect { state ->
                _notifications.value = if (state.showTrash) state.trashedNotifications
                else filterByCategory(state.notifications, state.selectedCategory)
            }
        }
        loadNotifications()
    }

    // region 公开方法

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val now = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val todayKey = now.toString()

            // 跳过同一天的重复生成，直接从 DB 读取
            val lastGenerated = prefs.getString("last_generated_date", "")
            val shouldGenerate = lastGenerated != todayKey

            if (shouldGenerate) {
                val existingEntities = dao.getAllNotifications().first()
                val existingIds = existingEntities.map { it.id }.toSet()
                val trashedEntities = dao.getTrashedNotifications().first()
                val trashedIds = trashedEntities.map { it.id }.toSet()

                val generated = generateNotifications(now, zone)
                val newEntities = generated
                    .filter { it.id !in existingIds && it.id !in trashedIds }
                    .map { it.toEntity() }

                if (newEntities.isNotEmpty()) {
                    dao.insertNotifications(newEntities)
                }
                prefs.edit().putString("last_generated_date", todayKey).apply()
            }

            val allEntities = dao.getAllNotifications().first()
            val trashedEntities = dao.getTrashedNotifications().first()
            val unreadCount = allEntities.count { !it.isRead }.coerceAtLeast(0)

            val allItems = allEntities.mapNotNull { it.toNotificationItem() }
            val trashedItems = trashedEntities.mapNotNull { it.toNotificationItem() }

            _uiState.value = _uiState.value.copy(
                notifications = allItems.sortedByDescending { it.timestamp },
                trashedNotifications = trashedItems.sortedByDescending { it.timestamp },
                unreadCount = unreadCount,
                isLoading = false
            )
        }
    }

    fun selectCategory(category: NotificationCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun trashNotification(id: String) {
        viewModelScope.launch {
            dao.trashNotification(id)
            // 从当前列表移除，加入回收站
            val current = _uiState.value
            val item = current.notifications.find { it.id == id }
            if (item != null) {
                val unreadDelta = if (dao.getNotificationById(id)?.isRead == false) 1 else 0
                _uiState.value = current.copy(
                    notifications = current.notifications.filter { it.id != id },
                    trashedNotifications = (listOf(item) + current.trashedNotifications)
                        .sortedByDescending { it.timestamp },
                    unreadCount = (current.unreadCount - unreadDelta).coerceAtLeast(0)
                )
            }
        }
    }

    fun restoreNotification(id: String) {
        viewModelScope.launch {
            dao.restoreNotification(id)
            val current = _uiState.value
            val item = current.trashedNotifications.find { it.id == id }
            if (item != null) {
                val unreadDelta = if (dao.getNotificationById(id)?.isRead == false) 1 else 0
                _uiState.value = current.copy(
                    notifications = (listOf(item) + current.notifications)
                        .sortedByDescending { it.timestamp },
                    trashedNotifications = current.trashedNotifications.filter { it.id != id },
                    unreadCount = current.unreadCount + unreadDelta
                )
            }
        }
    }

    fun permanentDeleteNotification(id: String) {
        viewModelScope.launch {
            dao.deleteNotification(id)
            val current = _uiState.value
            _uiState.value = current.copy(
                trashedNotifications = current.trashedNotifications.filter { it.id != id }
            )
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val ids = _uiState.value.trashedNotifications.map { it.id }
            ids.forEach { dao.deleteNotification(it) }
            _uiState.value = _uiState.value.copy(trashedNotifications = emptyList())
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            dao.markNotificationRead(id)
            val current = _uiState.value
            val unread = (current.unreadCount - 1).coerceAtLeast(0)
            _uiState.value = current.copy(unreadCount = unread)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val current = _uiState.value
            val unreadIds = current.notifications.filter { item ->
                val entity = dao.getNotificationById(item.id)
                entity != null && !entity.isRead
            }.map { it.id }
            unreadIds.forEach { id -> dao.markNotificationRead(id) }
            _uiState.value = current.copy(unreadCount = 0)
        }
    }

    fun toggleTrashView() {
        val current = _uiState.value
        _uiState.value = current.copy(showTrash = !current.showTrash)
    }

    // endregion

    // region 通知生成

    private suspend fun generateNotifications(
        now: LocalDate,
        zone: ZoneId
    ): List<NotificationItem> {
        val items = mutableListOf<NotificationItem>()
        val todayMillis = now.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowMillis = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // 1. 年度报告（12月25日起）
        if (now.monthValue == 12 && now.dayOfMonth >= 25) {
            val allPreviews = dao.getAllPreviewsOnce()
            val yearEntries = allPreviews.filter { entry ->
                val date = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
                date.year == now.year
            }
            if (yearEntries.isNotEmpty()) {
                items.add(
                    AnnualReportNotification(
                        year = now.year,
                        id = "annual_${now.year}",
                        timestamp = todayMillis
                    )
                )
            }
        }

        // 2. 胶囊到期通知
        val capsules = dao.getAllCapsulesOnce()
        capsules.filter { it.unlockDate < tomorrowMillis && !it.isOpened }.forEach { capsule ->
            items.add(CapsuleUnlockNotification(capsule))
        }

        // 3. 今日回顾（包含位置信息）
        val onThisDayEntries = dao.getPreviewsByMonthDay(now.monthValue, now.dayOfMonth)
        onThisDayEntries.forEach { entry ->
            val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            if (entryDate.year < now.year) {
                items.add(OnThisDayNotification(entry, now.year - entryDate.year))
            }
        }

        // 4. 里程碑
        val allPreviews = dao.getAllPreviewsOnce()
        val totalCount = allPreviews.size
        val milestones = listOf(10, 50, 100, 200, 300, 500, 1000)
        val highestMilestone = milestones.lastOrNull { totalCount >= it }
        if (highestMilestone != null) {
            val nextMilestone = milestones.firstOrNull { it > totalCount }
            if (nextMilestone == null || totalCount - highestMilestone < 10) {
                items.add(
                    MilestoneNotification(
                        title = "第 $highestMilestone 篇日记",
                        subtitle = "你已经写了 $totalCount 篇日记，继续记录吧"
                    )
                )
            }
        }

        // 5. 连续写作里程碑
        val entryDates = allPreviews.map { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
        }.toSet()
        val streak = computeStreak(entryDates)
        val streakMilestones = listOf(7, 14, 30, 50, 100, 365)
        val achievedStreak = streakMilestones.lastOrNull { streak >= it }
        if (achievedStreak != null) {
            items.add(StreakNotification(achievedStreak))
        }

        // 6. 月度报告（当月有日记数据时）
        val monthStart = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEntries = dao.getPreviewsByDateRange(monthStart, monthEnd)
        if (monthEntries.isNotEmpty()) {
            val wordCount = monthEntries.sumOf { it.plainText.length }
            items.add(
                MonthlyReportNotification(
                    year = now.year,
                    month = now.monthValue,
                    entryCount = monthEntries.size,
                    wordCount = wordCount,
                    id = "monthly_${now.year}_${now.monthValue}",
                    timestamp = todayMillis
                )
            )
        }

        // 7. 写作提醒（3天未写日记）
        val sortedDates = entryDates.sortedDescending()
        if (sortedDates.isNotEmpty()) {
            val lastEntryDate = sortedDates.first()
            val daysSinceLastEntry = java.time.temporal.ChronoUnit.DAYS.between(lastEntryDate, now).toInt()
            if (daysSinceLastEntry >= 3) {
                items.add(
                    InactivityNotification(
                        daysSinceLastEntry = daysSinceLastEntry,
                        id = "inactivity_${now}",
                        timestamp = todayMillis
                    )
                )
            }
        }

        // 8. 周报（每周一）
        if (now.dayOfWeek.value == 1) {
            val weekAgo = now.minusDays(7)
            val weekStart = weekAgo.atStartOfDay(zone).toInstant().toEpochMilli()
            val weekEntries = dao.getPreviewsByDateRange(weekStart, tomorrowMillis)
            if (weekEntries.isNotEmpty()) {
                val wordCount = weekEntries.sumOf { it.plainText.length }
                val weatherCounts = weekEntries.mapNotNull { it.weather?.takeIf { w -> w.isNotBlank() } }
                    .groupingBy { it }.eachCount()
                val topWeather = weatherCounts.maxByOrNull { it.value }?.key
                items.add(
                    WeeklySummaryNotification(
                        entryCount = weekEntries.size,
                        wordCount = wordCount,
                        topWeather = topWeather,
                        id = "weekly_summary_${now}",
                        timestamp = todayMillis
                    )
                )
            }
        }

        return items
    }

    private fun computeStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var streak = 0
        var current = dates.maxOrNull() ?: return 0
        val today = LocalDate.now()
        if (current.isAfter(today)) return 0
        while (current in dates) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    // endregion

    // region Entity <-> NotificationItem 转换

    private fun NotificationItem.toEntity(): NotificationEntity {
        val (type, title, subtitle, iconType, colorHex, relatedId) = when (this) {
            is CapsuleUnlockNotification -> NotificationMeta(
                type = "capsule",
                title = "时间胶囊已到期",
                subtitle = "你有一封信到了可以打开的日子",
                iconType = "lock_open",
                colorHex = 0xFF6750A4,
                relatedId = capsule.id
            )
            is OnThisDayNotification -> NotificationMeta(
                type = "on_this_day",
                title = "${yearsAgo} 年前的今天",
                subtitle = if (entry.title.isNotBlank()) entry.title else entry.plainText.take(40),
                iconType = "history",
                colorHex = 0xFF7B61FF,
                relatedId = entry.id
            )
            is MilestoneNotification -> NotificationMeta(
                type = "milestone",
                title = title,
                subtitle = subtitle,
                iconType = "emoji_events",
                colorHex = 0xFFD4A017,
                relatedId = null
            )
            is StreakNotification -> NotificationMeta(
                type = "streak",
                title = "连续写作 $days 天",
                subtitle = "坚持记录，保持习惯",
                iconType = "local_fire_department",
                colorHex = 0xFFE86833,
                relatedId = null
            )
            is AnnualReportNotification -> NotificationMeta(
                type = "annual_report",
                title = "${year} 年度报告已生成",
                subtitle = "回顾过去一年的点滴记录，点击查看年度总结",
                iconType = "history",
                colorHex = 0xFF4A90E2,
                relatedId = null
            )
            is MonthlyReportNotification -> NotificationMeta(
                type = "monthly_report",
                title = "${month}月写作报告",
                subtitle = "本月写了 ${entryCount} 篇日记，共 ${wordCount} 字",
                iconType = "assessment",
                colorHex = 0xFF4A90E2,
                relatedId = null
            )
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
            is InactivityNotification -> NotificationMeta(
                type = "inactivity",
                title = "你好久没写日记了",
                subtitle = "已经 ${daysSinceLastEntry} 天没记录了，写点什么吧",
                iconType = "edit_note",
                colorHex = 0xFF7B61FF,
                relatedId = null
            )
            is WeeklySummaryNotification -> NotificationMeta(
                type = "weekly_summary",
                title = "本周写作回顾",
                subtitle = "本周写了 ${entryCount} 篇日记，共 ${wordCount} 字" +
                    (topWeather?.let { "，最常在${it}时记录" } ?: ""),
                iconType = "date_range",
                colorHex = 0xFF4CAF50,
                relatedId = null
            )
        }
        return NotificationEntity(
            id = id,
            type = type,
            title = title,
            subtitle = subtitle,
            iconType = iconType,
            colorHex = colorHex,
            relatedId = relatedId,
            createdAt = timestamp
        )
    }

    private fun NotificationEntity.toNotificationItem(): NotificationItem? {
        return when (type) {
            "capsule" -> {
                val capsuleId = relatedId ?: return null
                CapsuleUnlockNotification(
                    capsule = TimeCapsule(
                        id = capsuleId,
                        title = title,
                        content = "",
                        createdAt = createdAt,
                        unlockDate = createdAt,
                        isRead = isRead
                    ),
                    id = id,
                    timestamp = createdAt
                )
            }
            "on_this_day" -> {
                val entryId = relatedId ?: return null
                OnThisDayNotification(
                    entry = DiaryPreview(
                        id = entryId,
                        title = title.removeSuffix(" 年前的今天").substringAfter(" ").ifBlank { "" },
                        plainText = subtitle,
                        moodLevel = null,
                        weather = null,
                        location = null,
                        latitude = null,
                        longitude = null,
                        isFavorite = false,
                        createdAt = createdAt,
                        updatedAt = createdAt
                    ),
                    yearsAgo = title.removeSuffix(" 年前的今天").toIntOrNull() ?: 1,
                    id = id,
                    timestamp = createdAt
                )
            }
            "milestone" -> MilestoneNotification(
                title = title,
                subtitle = subtitle,
                id = id,
                timestamp = createdAt
            )
            "streak" -> {
                val days = title.removePrefix("连续写作 ").removeSuffix(" 天").toIntOrNull() ?: 0
                StreakNotification(days = days, id = id, timestamp = createdAt)
            }
            "annual_report" -> {
                val year = title.removeSuffix(" 年度报告已生成").toIntOrNull() ?: return null
                AnnualReportNotification(year = year, id = id, timestamp = createdAt)
            }
            "monthly_report" -> {
                val regex = Regex("(\\d+)月写作报告")
                val match = regex.find(title) ?: return null
                val month = match.groupValues[1].toIntOrNull() ?: return null
                val countRegex = Regex("本月写了 (\\d+) 篇日记，共 (\\d+) 字")
                val countMatch = countRegex.find(subtitle)
                val entryCount = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val wordCount = countMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                // 从 id 推断 year: "monthly_2026_6"
                val parts = id.split("_")
                val year = if (parts.size >= 2) parts[1].toIntOrNull() ?: LocalDate.now().year
                else LocalDate.now().year
                MonthlyReportNotification(
                    year = year,
                    month = month,
                    entryCount = entryCount,
                    wordCount = wordCount,
                    id = id,
                    timestamp = createdAt
                )
            }
            "weather_alert" -> {
                val parts = title.split("预警 · ")
                val level = if (parts.size >= 2) parts[0] else ""
                val type = if (parts.size >= 2) parts[1] else title
                WeatherAlertNotification(
                    weatherCity = "",
                    alertLevel = level,
                    alertType = type,
                    alertText = subtitle,
                    alertId = id.removePrefix("weather_alert_"),
                    id = id,
                    timestamp = createdAt
                )
            }
            "inactivity" -> {
                val days = Regex("(\\d+) 天").find(subtitle)?.groupValues?.get(1)?.toIntOrNull() ?: 3
                InactivityNotification(
                    daysSinceLastEntry = days,
                    id = id,
                    timestamp = createdAt
                )
            }
            "weekly_summary" -> {
                val countRegex = Regex("本周写了 (\\d+) 篇日记，共 (\\d+) 字")
                val countMatch = countRegex.find(subtitle)
                val entryCount = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val wordCount = countMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                val weather = if (subtitle.contains("最常在")) {
                    subtitle.substringAfter("最常在").removeSuffix("时记录")
                } else null
                WeeklySummaryNotification(
                    entryCount = entryCount,
                    wordCount = wordCount,
                    topWeather = weather,
                    id = id,
                    timestamp = createdAt
                )
            }
            else -> null
        }
    }

    // endregion

    // region 辅助

    private fun filterByCategory(
        items: List<NotificationItem>,
        category: NotificationCategory
    ): List<NotificationItem> {
        if (category == NotificationCategory.ALL) return items
        return items.filter { it.category == category }
    }

    /** 内部元数据，用于 toEntity() 避免重复解构 */
    private data class NotificationMeta(
        val type: String,
        val title: String,
        val subtitle: String,
        val iconType: String,
        val colorHex: Long,
        val relatedId: Long?
    )

    // endregion
}

// endregion
