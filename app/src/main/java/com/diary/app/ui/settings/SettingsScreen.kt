package com.diary.app.ui.settings

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateCheckResult
import com.diary.app.update.UpdateDialog
import com.diary.app.update.toUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launchprivate object S {
    const val settings = "设置"
    const val writing = "写作"
    const val defaultMood = "默认心情"
    const val moodNone = "不设置"
    const val defaultWeatherLabel = "默认天气"
    const val weatherNone = "不设置"
    const val autoSave = "自动保存"
    const val autoSaveInterval = "%d 秒"
    const val defaultSort = "默认排序"
    const val sortCreatedDesc = "最新创建"
    const val sortCreatedAsc = "最早创建"
    const val sortUpdatedDesc = "最近修改"
    const val sortMoodDesc = "心情从高到低"
    const val calendarMode = "日历模式"
    const val calendarWeek = "周视图"
    const val calendarMonth = "月视图"
    const val notification = "通知"
    const val writingReminder = "写作提醒"
    const val writingReminderDesc = "每日定时提醒你写日记"
    const val reminderTime = "提醒时间"
    const val streakReminder = "连续记录提醒"
    const val streakReminderDesc = "当天未写日记时提醒你"
    const val weatherNotify = "天气提醒"
    const val weatherNotifyDesc = "天气变化时提醒记录"
    const val dailyReview = "每日回顾"
    const val dailyReviewDesc = "推送昨日日记回顾"
    const val dnd = "免打扰时段"
    const val dndTime = "%02d:00 - %02d:00"
    const val dataManagement = "数据管理"
    const val trashRetention = "回收站保留"
    const val trashDays = "%d 天"
    const val autoClean = "自动清理孤立媒体"
    const val autoCleanDesc = "自动删除无关联的图片和文件"
    const val clearCache = "清除缓存"
    const val clearCacheDesc = "清除 AI 缓存和临时文件"
    const val editor = "编辑器"
    const val fontSize = "字体大小"
    const val fontSizeValue = "%.0f sp"
    const val compactToolbar = "精简工具栏"
    const val compactToolbarDesc = "收起编辑器工具栏为单行"
    const val autoTag = "智能标签推荐"
    const val autoTagDesc = "保存时根据内容自动推荐标签"
    const val appearance = "外观"
    const val theme = "主题"
    const val themeDesc = "当前主题：%s"
    const val privacy = "隐私"
    const val appLock = "应用锁"
    const val appLockDesc = "使用指纹或 PIN 锁保护日记"
    const val locationRecord = "记录位置"
    const val locationRecordDesc = "在日记中自动记录当前位置"
    const val aiConsent = "AI 数据使用授权"
    const val aiConsentDesc = "允许 AI 读取日记内容生成洞察"
    const val screenshotProtect = "截屏保护"
    const val screenshotProtectDesc = "阻止系统截屏捕获应用内容"
    const val backupTitle = "备份与恢复"
    const val backupData = "备份数据"
    const val backupDataDesc = "导出日记和设置数据"
    const val tagManage = "标签管理"
    const val tagManageDesc = "管理日记标签和分类"
    const val about = "关于"
    const val checkUpdate = "检查更新"
    const val checking = "检查中..."
    const val changelog = "更新日志"
    const val changelogDesc = "查看各版本的更新内容"
    const val madeWith = "用 "
    const val madeBy = " 制作"
    val weatherOptions = listOf("晴天", "多云", "阴天", "小雨", "大雨", "雪天", "雾天", "风天")
    val moodLabels = listOf("不设置", "\uD83D\uDE22 很差", "\uD83D\uDE14 不好", "\uD83D\uDE10 一般", "\uD83E\uDD72 还好", "\uD83D\uDE0A 不错", "\uD83D\uDE04 很棒")
    val sortOptions = listOf("created_desc" to sortCreatedDesc, "created_asc" to sortCreatedAsc, "updated_desc" to sortUpdatedDesc, "mood_desc" to sortMoodDesc)
    val autoSaveOptions = listOf(0 to "关闭", 30 to "30 秒", 60 to "1 分钟", 120 to "2 分钟", 300 to "5 分钟")
    val trashOptions = listOf(7 to "7 天", 14 to "14 天", 30 to "30 天", 60 to "60 天", 90 to "90 天")
}

private fun weatherIcon(weather: String): ImageVector = when (weather) {
    "晴天" -> Icons.Default.WbSunny; "多云" -> Icons.Default.Cloud; "阴天" -> Icons.Default.CloudQueue
    "小雨" -> Icons.Default.Grain; "大雨" -> Icons.Default.Thunderstorm; "雪天" -> Icons.Default.AcUnit
    "雾天" -> Icons.Default.WaterDrop; "风天" -> Icons.Default.Air; else -> Icons.Default.Cloud
}

private fun moodIcon(level: Int): ImageVector = when (level) {
    1 -> Icons.Default.SentimentVeryDissatisfied; 2 -> Icons.Default.SentimentDissatisfied; 3 -> Icons.Default.SentimentNeutral
    4 -> Icons.Default.SentimentSatisfied; 5 -> Icons.Default.SentimentSatisfiedAlt; 6 -> Icons.Default.SentimentVerySatisfied
    else -> Icons.Default.Mood
}@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTheme: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication ?: return
    val currentThemeMode by app.themeMode.collectAsState()
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    var isChecking by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var updateNotes by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1f) }
    var isForceUpdate by remember { mutableStateOf(false) }
    var defaultMoodLevel by remember { mutableStateOf(AppPreferences.defaultMoodLevel) }
    var defaultWeather by remember { mutableStateOf(AppPreferences.defaultWeather) }
    var autoSaveInterval by remember { mutableStateOf(AppPreferences.autoSaveInterval) }
    var defaultSortBy by remember { mutableStateOf(AppPreferences.defaultSortBy) }
    var defaultCalendarMode by remember { mutableStateOf(AppPreferences.defaultCalendarMode) }
    var writingReminderEnabled by remember { mutableStateOf(AppPreferences.writingReminderEnabled) }
    var writingReminderHour by remember { mutableStateOf(AppPreferences.writingReminderHour) }
    var writingReminderMinute by remember { mutableStateOf(AppPreferences.writingReminderMinute) }
    var streakBreakReminder by remember { mutableStateOf(AppPreferences.streakBreakReminder) }
    var weatherReminder by remember { mutableStateOf(AppPreferences.weatherReminder) }
    var dailyReviewPush by remember { mutableStateOf(AppPreferences.dailyReviewPush) }
    var doNotDisturbStart by remember { mutableStateOf(AppPreferences.doNotDisturbStart) }
    var doNotDisturbEnd by remember { mutableStateOf(AppPreferences.doNotDisturbEnd) }
    var trashRetentionDays by remember { mutableStateOf(AppPreferences.trashRetentionDays) }
    var autoCleanOrphanMedia by remember { mutableStateOf(AppPreferences.autoCleanOrphanMedia) }
    var editorFontSize by remember { mutableStateOf(AppPreferences.editorFontSize) }
    var editorToolbarCompact by remember { mutableStateOf(AppPreferences.editorToolbarCompact) }
    var autoTagSuggestion by remember { mutableStateOf(AppPreferences.autoTagSuggestion) }
    var locationRecordingEnabled by remember { mutableStateOf(AppPreferences.locationRecordingEnabled) }
    var aiDataUsageConsent by remember { mutableStateOf(AppPreferences.aiDataUsageConsent) }
    var screenshotProtection by remember { mutableStateOf(AppPreferences.screenshotProtection) }
    var showWeatherPicker by remember { mutableStateOf(false) }
    var showMoodPicker by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }
    var showAutoSavePicker by remember { mutableStateOf(false) }
    var showTrashPicker by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }
    if (showUpdateDialog) {
        UpdateDialog(versionName = updateVersion, releaseNotes = updateNotes, isDownloading = isDownloading, downloadProgress = downloadProgress, isForceUpdate = isForceUpdate, onConfirm = {
            isDownloading = true; downloadProgress = -1f; val fileName = "DiaryApp-v.apk"
            scope.launch { try { ApkInstaller.downloadAndInstall(context, updateUrl, fileName).collect { state -> when (state) { is DownloadState.Progress -> downloadProgress = if (state.totalBytes > 0) state.bytesDownloaded.toFloat() / state.totalBytes else -1f; is DownloadState.Completed -> { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false }; is DownloadState.Failed -> { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false; Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show() } } } } catch (e: Exception) { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false; Toast.makeText(context, "下载失败: ", Toast.LENGTH_SHORT).show() } } }
        }, onDismiss = { showUpdateDialog = false })
    }
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = S.settings, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // ═══ 写作 ═══
                Staggered(0, showContent) { SectionHeader(title = S.writing, icon = Icons.Default.Edit, color = primary) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(1, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    DropdownSettingItem(Icons.Default.Mood, S.defaultMood, if (defaultMoodLevel in 1..6) S.moodLabels[defaultMoodLevel] else S.moodNone, primary.copy(0.1f), primary, textColor, textTertiary, showMoodPicker, { showMoodPicker = !showMoodPicker }, { showMoodPicker = false }) { DropdownMenuItem(text = { Text(S.moodNone) }, onClick = { defaultMoodLevel = -1; AppPreferences.defaultMoodLevel = -1; showMoodPicker = false }); for (l in 1..6) DropdownMenuItem(text = { Text(S.moodLabels[l]) }, onClick = { defaultMoodLevel = l; AppPreferences.defaultMoodLevel = l; showMoodPicker = false }) }
                    SettingDivider()
                    DropdownSettingItem(weatherIcon(defaultWeather), S.defaultWeatherLabel, if (defaultWeather.isNotBlank()) defaultWeather else S.weatherNone, secondary.copy(0.1f), secondary, textColor, textTertiary, showWeatherPicker, { showWeatherPicker = !showWeatherPicker }, { showWeatherPicker = false }) { DropdownMenuItem(text = { Text(S.weatherNone) }, onClick = { defaultWeather = ""; AppPreferences.defaultWeather = ""; showWeatherPicker = false }); S.weatherOptions.forEach { w -> DropdownMenuItem(text = { Text(w) }, onClick = { defaultWeather = w; AppPreferences.defaultWeather = w; showWeatherPicker = false }) } }
                    SettingDivider()
                    DropdownSettingItem(Icons.Default.Timer, S.autoSave, S.autoSaveInterval.format(autoSaveInterval), primary.copy(0.1f), primary, textColor, textTertiary, showAutoSavePicker, { showAutoSavePicker = !showAutoSavePicker }, { showAutoSavePicker = false }) { S.autoSaveOptions.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { autoSaveInterval = v; AppPreferences.autoSaveInterval = v; showAutoSavePicker = false }) } }
                    SettingDivider()
                    DropdownSettingItem(Icons.Default.Sort, S.defaultSort, S.sortOptions.find { it.first == defaultSortBy }?.second ?: S.sortCreatedDesc, secondary.copy(0.1f), secondary, textColor, textTertiary, showSortPicker, { showSortPicker = !showSortPicker }, { showSortPicker = false }) { S.sortOptions.forEach { (k, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { defaultSortBy = k; AppPreferences.defaultSortBy = k; showSortPicker = false }) } }
                    SettingDivider()
                    DropdownSettingItem(Icons.Default.CalendarMonth, S.calendarMode, if (defaultCalendarMode == "week") S.calendarWeek else S.calendarMonth, primary.copy(0.1f), primary, textColor, textTertiary, showCalendarPicker, { showCalendarPicker = !showCalendarPicker }, { showCalendarPicker = false }) { DropdownMenuItem(text = { Text(S.calendarWeek) }, onClick = { defaultCalendarMode = "week"; AppPreferences.defaultCalendarMode = "week"; showCalendarPicker = false }); DropdownMenuItem(text = { Text(S.calendarMonth) }, onClick = { defaultCalendarMode = "month"; AppPreferences.defaultCalendarMode = "month"; showCalendarPicker = false }) }
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 通知 ═══
                Staggered(2, showContent) { SectionHeader(title = S.notification, icon = Icons.Default.Notifications, color = error) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(3, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    SwitchSettingItem(Icons.Default.Alarm, S.writingReminder, S.writingReminderDesc, error.copy(0.1f), error, textColor, textTertiary, writingReminderEnabled) { writingReminderEnabled = it; AppPreferences.writingReminderEnabled = it }
                    if (writingReminderEnabled) { SettingDivider(); TimeSettingItem(Icons.Default.Schedule, S.reminderTime, writingReminderHour, writingReminderMinute, error.copy(0.1f), error, textColor, textTertiary, context) { h, m -> writingReminderHour = h; writingReminderMinute = m; AppPreferences.writingReminderHour = h; AppPreferences.writingReminderMinute = m } }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.LocalFireDepartment, S.streakReminder, S.streakReminderDesc, Color(0xFFFF9800).copy(0.1f), Color(0xFFFF9800), textColor, textTertiary, streakBreakReminder) { streakBreakReminder = it; AppPreferences.streakBreakReminder = it }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.Cloud, S.weatherNotify, S.weatherNotifyDesc, secondary.copy(0.1f), secondary, textColor, textTertiary, weatherReminder) { weatherReminder = it; AppPreferences.weatherReminder = it }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.Replay, S.dailyReview, S.dailyReviewDesc, primary.copy(0.1f), primary, textColor, textTertiary, dailyReviewPush) { dailyReviewPush = it; AppPreferences.dailyReviewPush = it }
                    SettingDivider()
                    TimeRangeItem(Icons.Default.DoNotDisturb, S.dnd, S.dndTime.format(doNotDisturbStart, doNotDisturbEnd), doNotDisturbStart, doNotDisturbEnd, Color(0xFF9C27B0).copy(0.1f), Color(0xFF9C27B0), textColor, textTertiary, context, { doNotDisturbStart = it; AppPreferences.doNotDisturbStart = it }, { doNotDisturbEnd = it; AppPreferences.doNotDisturbEnd = it })
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 数据管理 ═══
                Staggered(4, showContent) { SectionHeader(title = S.dataManagement, icon = Icons.Default.Storage, color = secondary) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(5, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    DropdownSettingItem(Icons.Default.DeleteSweep, S.trashRetention, S.trashDays.format(trashRetentionDays), secondary.copy(0.1f), secondary, textColor, textTertiary, showTrashPicker, { showTrashPicker = !showTrashPicker }, { showTrashPicker = false }) { S.trashOptions.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { trashRetentionDays = v; AppPreferences.trashRetentionDays = v; showTrashPicker = false }) } }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.CleaningServices, S.autoClean, S.autoCleanDesc, secondary.copy(0.1f), secondary, textColor, textTertiary, autoCleanOrphanMedia) { autoCleanOrphanMedia = it; AppPreferences.autoCleanOrphanMedia = it }
                    SettingDivider()
                    SettingsNavigateItem(Icons.Default.Cache, S.clearCache, S.clearCacheDesc, secondary.copy(0.1f), secondary, textColor, textTertiary) { context.cacheDir?.deleteRecursively(); Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show() }
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 编辑器 ═══
                Staggered(6, showContent) { SectionHeader(title = S.editor, icon = Icons.Default.EditNote, color = primary) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(7, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    SliderSettingItem(Icons.Default.FormatSize, S.fontSize, S.fontSizeValue.format(editorFontSize), editorFontSize, 12f..24f, 11, primary.copy(0.1f), primary, textColor, textTertiary, { editorFontSize = it }, { AppPreferences.editorFontSize = editorFontSize })
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.ViewCompact, S.compactToolbar, S.compactToolbarDesc, primary.copy(0.1f), primary, textColor, textTertiary, editorToolbarCompact) { editorToolbarCompact = it; AppPreferences.editorToolbarCompact = it }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.AutoAwesome, S.autoTag, S.autoTagDesc, Color(0xFFFF9800).copy(0.1f), Color(0xFFFF9800), textColor, textTertiary, autoTagSuggestion) { autoTagSuggestion = it; AppPreferences.autoTagSuggestion = it }
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 外观 ═══
                Staggered(8, showContent) { SectionHeader(title = S.appearance, icon = Icons.Default.Palette, color = Color(0xFF4CAF50)) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(9, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { SettingsNavigateItem(Icons.Default.Palette, S.theme, S.themeDesc.format(currentThemeMode.label), Color(0xFF4CAF50).copy(0.1f), Color(0xFF4CAF50), textColor, textTertiary, onClick = onNavigateToTheme) } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 隐私 ═══
                Staggered(10, showContent) { SectionHeader(title = S.privacy, icon = Icons.Default.Security, color = error) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(11, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    SettingsNavigateItem(Icons.Default.Lock, S.appLock, S.appLockDesc, error.copy(0.1f), error, textColor, textTertiary, onClick = onNavigateToAppLock)
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.LocationOn, S.locationRecord, S.locationRecordDesc, Color(0xFF2196F3).copy(0.1f), Color(0xFF2196F3), textColor, textTertiary, locationRecordingEnabled) { locationRecordingEnabled = it; AppPreferences.locationRecordingEnabled = it }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.SmartToy, S.aiConsent, S.aiConsentDesc, Color(0xFF9C27B0).copy(0.1f), Color(0xFF9C27B0), textColor, textTertiary, aiDataUsageConsent) { aiDataUsageConsent = it; AppPreferences.aiDataUsageConsent = it }
                    SettingDivider()
                    SwitchSettingItem(Icons.Default.Screenshot, S.screenshotProtect, S.screenshotProtectDesc, error.copy(0.1f), error, textColor, textTertiary, screenshotProtection) { screenshotProtection = it; AppPreferences.screenshotProtection = it; val act = context as? android.app.Activity; if (it) act?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else act?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 备份 ═══
                Staggered(12, showContent) { SectionHeader(title = S.backupTitle, icon = Icons.Default.Backup, color = Color(0xFF00BCD4)) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(13, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column {
                    SettingsNavigateItem(Icons.Default.Backup, S.backupData, S.backupDataDesc, Color(0xFF00BCD4).copy(0.1f), Color(0xFF00BCD4), textColor, textTertiary, onClick = onNavigateToBackup)
                    SettingDivider()
                    SettingsNavigateItem(Icons.Default.Label, S.tagManage, S.tagManageDesc, Color(0xFF00BCD4).copy(0.1f), Color(0xFF00BCD4), textColor, textTertiary, onClick = onNavigateToTagManagement)
                } } }
                Spacer(modifier = Modifier.height(20.dp))
                // ═══ 关于 ═══
                Staggered(14, showContent) { SectionHeader(title = S.about, icon = Icons.Default.Info, color = primary) }
                Spacer(modifier = Modifier.height(8.dp))
                Staggered(15, showContent) { GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(primary, secondary))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 16.dp, vertical = 6.dp)) { Text(text = "v", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textSecondary) }
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsNavigateItem(Icons.Default.SystemUpdate, S.checkUpdate, if (isChecking) S.checking else "检查是否有新版本", primary.copy(0.1f), primary, textColor, textTertiary, trailing = if (isChecking) {{ CircularProgressIndicator(color = primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }} else null, onClick = { if (!isChecking) { isChecking = true; scope.launch { try { val result = UpdateChecker.checkForUpdateDetailed(context, BuildConfig.VERSION_NAME); isChecking = false; when (result) { is UpdateCheckResult.UpdateAvailable -> { updateVersion = result.info.versionName; updateNotes = result.info.releaseNotes; updateUrl = result.info.downloadUrl; isForceUpdate = result.info.isForceUpdate; showUpdateDialog = true }; else -> Toast.makeText(context, result.toUserMessage(context), Toast.LENGTH_SHORT).show() } } catch (e: Exception) { isChecking = false; Toast.makeText(context, "更新检查失败: ", Toast.LENGTH_SHORT).show() } } } })
                    SettingDivider()
                    SettingsNavigateItem(Icons.Default.History, S.changelog, S.changelogDesc, primary.copy(0.1f), primary, textColor, textTertiary, onClick = onNavigateToChangelog)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text(text = S.madeWith, fontSize = 12.sp, color = textTertiary); Icon(Icons.Default.Favorite, contentDescription = null, tint = error, modifier = Modifier.size(14.dp)); Text(text = S.madeBy, fontSize = 12.sp, color = textTertiary) }
                } } }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}// ═══════════════════════════════════════════════════════════
// 可复用设置项组件
// ═══════════════════════════════════════════════════════════

@Composable
private fun SettingsNavigateItem(icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "nav")
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null, enabled = onClick != null) { onClick?.invoke() }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
        if (trailing != null) trailing() else if (onClick != null) Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SwitchSettingItem(icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "sw")
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iconTint, uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun DropdownSettingItem(icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, showDropdown: Boolean, onToggleDropdown: () -> Unit, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "dd")
    Box {
        Row(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null) { onToggleDropdown() }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
            Icon(if (showDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = textTertiary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = showDropdown, onDismissRequest = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) { content() }
    }
}

@Composable
private fun SliderSettingItem(icon: ImageVector, title: String, subtitle: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, steps: Int, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
        }
        Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, valueRange = valueRange, steps = steps, modifier = Modifier.fillMaxWidth().padding(start = 46.dp, top = 4.dp), colors = SliderDefaults.colors(thumbColor = iconTint, activeTrackColor = iconTint))
    }
}

@Composable
private fun TimeSettingItem(icon: ImageVector, title: String, hour: Int, minute: Int, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, context: android.content.Context, onTimeChange: (Int, Int) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "ts")
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null) { TimePickerDialog(context, { _, h, m -> onTimeChange(h, m) }, hour, minute, true).show() }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = "%02d:%02d".format(hour, minute), fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
        Icon(Icons.Default.Edit, contentDescription = null, tint = textTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TimeRangeItem(icon: ImageVector, title: String, subtitle: String, startHour: Int, endHour: Int, iconBg: Color, iconTint: Color, textColor: Color, textTertiary: Color, context: android.content.Context, onStartChange: (Int) -> Unit, onEndChange: (Int) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh), label = "tr")
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null) { TimePickerDialog(context, { _, h, _ -> onStartChange(h) }, startHour, 0, true).show(); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ TimePickerDialog(context, { _, h, _ -> onEndChange(h) }, endHour, 0, true).show() }, 300) }.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 15.sp, color = textColor); Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)) }
        Icon(Icons.Default.Edit, contentDescription = null, tint = textTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Staggered(index: Int, show: Boolean, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(show) { if (show) { delay(index * 50L); visible = true } }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300), label = "a")
    val offsetY by animateFloatAsState(targetValue = if (visible) 0f else 20f, animationSpec = tween(300), label = "o")
    Box(modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha; translationY = offsetY }) { content() }
}