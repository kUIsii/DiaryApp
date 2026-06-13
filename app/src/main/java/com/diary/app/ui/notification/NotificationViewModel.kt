package com.diary.app.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import com.diary.app.data.TimeCapsule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val items = mutableListOf<NotificationItem>()
            val now = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val todayMillis = now.atStartOfDay(zone).toInstant().toEpochMilli()
            val tomorrowMillis = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            // 1. Capsule unlock notifications
            val capsules = dao.getAllCapsulesOnce()
            val unreadUnlocked = capsules.filter { capsule ->
                capsule.unlockDate < tomorrowMillis && !capsule.isRead
            }
            unreadUnlocked.forEach { capsule ->
                items.add(CapsuleUnlockNotification(capsule))
            }

            // 2. On This Day
            val onThisDayEntries = dao.getPreviewsByMonthDay(now.monthValue, now.dayOfMonth)
            onThisDayEntries.forEach { entry ->
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(zone).toLocalDate()
                if (entryDate.year < now.year) {
                    items.add(OnThisDayNotification(entry, now.year - entryDate.year))
                }
            }

            // 3. Milestones
            val allPreviews = dao.getAllPreviewsOnce()
            val totalCount = allPreviews.size
            val milestones = listOf(10, 50, 100, 200, 300, 500, 1000)
            val highestMilestone = milestones.lastOrNull { totalCount >= it }
            if (highestMilestone != null) {
                // Check if this milestone was recently crossed
                // (simple: just show the highest achieved milestone if total is close)
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

            // 4. Streak milestones
            val entryDates = allPreviews.map { entry ->
                Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
            }.toSet()
            val streak = computeStreak(entryDates)
            val streakMilestones = listOf(7, 14, 30, 50, 100, 365)
            val achievedStreak = streakMilestones.lastOrNull { streak >= it }
            if (achievedStreak != null) {
                items.add(StreakNotification(achievedStreak))
            }

            _notifications.value = items.sortedByDescending { it.timestamp }
        }
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
}
