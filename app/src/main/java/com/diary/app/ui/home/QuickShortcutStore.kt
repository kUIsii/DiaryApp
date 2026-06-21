package com.diary.app.ui.home

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

data class QuickShortcutOption(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val allShortcutOptions = listOf(
    QuickShortcutOption("stats", "统计", Icons.Default.BarChart),
    QuickShortcutOption("countdown", "倒数日", Icons.Default.Timer),
    QuickShortcutOption("ai_assistant", "AI 助手", Icons.Default.AutoAwesome),
    QuickShortcutOption("favorites", "收藏", Icons.Default.Favorite),
    QuickShortcutOption("time_capsule", "时间胶囊", Icons.Default.Schedule),
    QuickShortcutOption("media_library", "媒体库", Icons.Default.Image),
    QuickShortcutOption("diary_map", "日记地图", Icons.Default.LocationOn),
    QuickShortcutOption("biography", "AI 传记", Icons.Default.AutoAwesome),
    QuickShortcutOption("achievements", "成就", Icons.Default.EmojiEvents),
    QuickShortcutOption("todo", "待办", Icons.Default.CheckBox),
    QuickShortcutOption("timeline", "时间轴", Icons.Default.CalendarMonth),
    QuickShortcutOption("notifications", "通知", Icons.Default.Notifications),
    QuickShortcutOption("backup", "备份", Icons.Default.Backup),
    QuickShortcutOption("tag_management", "标签管理", Icons.Default.Label),
    QuickShortcutOption("storage", "存储", Icons.Default.Memory),
)

object QuickShortcutStore {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_SHORTCUTS = "quick_shortcuts"
    const val MAX_SHORTCUTS = 4

    private val DEFAULT_SHORTCUTS = listOf("stats", "countdown", "ai_assistant", "favorites")

    fun getShortcuts(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHORTCUTS, null)
        return if (raw.isNullOrBlank()) {
            DEFAULT_SHORTCUTS
        } else {
            raw.split(",").filter { it.isNotBlank() }.take(MAX_SHORTCUTS)
        }
    }

    fun setShortcuts(context: Context, routes: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHORTCUTS, routes.take(MAX_SHORTCUTS).joinToString(","))
            .apply()
    }

    fun getOption(route: String): QuickShortcutOption? {
        return allShortcutOptions.find { it.route == route }
    }
}
