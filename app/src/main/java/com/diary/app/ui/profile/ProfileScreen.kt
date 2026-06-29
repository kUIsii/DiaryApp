package com.diary.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.biometric.BiometricHelper
import com.diary.app.reminder.ReminderManager
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.IconCircle
import com.diary.app.ui.components.SettingDivider
import com.diary.app.ui.editor.APP_FONT_SIZE_PREF_KEY
import com.diary.app.ui.editor.EDITOR_FONT_SIZE_PREF_KEY
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.isDarkStatic
import com.diary.app.data.BackupManager
import com.diary.app.reminder.NotificationPreferencesManager
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateCheckResult
import com.diary.app.update.UpdateDialog
import com.diary.app.update.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.diary.app.R

// Section icon colors with a slightly different tint balance per group
@Composable
private fun sectionIconBg(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    return when (index) {
        0 -> p.copy(alpha = 0.12f)
        1 -> p.copy(alpha = 0.08f)
        2 -> s.copy(alpha = 0.13f)
        3 -> t.copy(alpha = 0.12f)
        4 -> p.copy(alpha = 0.10f)
        else -> p.copy(alpha = 0.10f)
    }
}
@Composable
private fun sectionIconTint(index: Int): Color {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val t = MaterialTheme.colorScheme.tertiary
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    return when (index) {
        0 -> p
        1 -> Color(
            (p.red * 0.6f + gray.red * 0.4f),
            (p.green * 0.6f + gray.green * 0.4f),
            (p.blue * 0.6f + gray.blue * 0.4f),
            1f
        )
        2 -> s
        3 -> t
        4 -> Color(
            (p.red * 0.4f + s.red * 0.6f),
            (p.green * 0.4f + s.green * 0.6f),
            (p.blue * 0.4f + s.blue * 0.6f),
            1f
        )
        else -> p
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication ?: return
    val currentThemeMode by app.themeMode.collectAsState()
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var updateNotes by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1f) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var isForceUpdate by remember { mutableStateOf(false) }
    val fontSizeOptions = listOf(
        FontSizeOption("tiny", "极小", 10),
        FontSizeOption("smaller", "较小", 12),
        FontSizeOption("small", "小", 14),
        FontSizeOption("medium_small", "中小", 15),
        FontSizeOption("medium", "中", 16),
        FontSizeOption("large", "大", 18),
        FontSizeOption("extra_large", "特大", 20),
    )
    var currentFontSizeKey by remember {
        mutableStateOf(
            context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
                .getString(APP_FONT_SIZE_PREF_KEY, null)
                ?: context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
                    .getString(EDITOR_FONT_SIZE_PREF_KEY, "small")
                ?: "small"
        )
    }
    var reminderEnabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).first) }
    var reminderMinute by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).second) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Notification settings
    var weatherAlertsEnabled by remember { mutableStateOf(NotificationPreferencesManager.isWeatherAlertsEnabled(context)) }
    var achievementsNotifEnabled by remember { mutableStateOf(NotificationPreferencesManager.isAchievementsEnabled(context)) }
    var biometricLockEnabled by remember { mutableStateOf(BiometricHelper.isBiometricLockEnabled(context)) }
    var screenshotProtectionEnabled by remember { mutableStateOf(ScreenshotProtectionHelper.isEnabled(context)) }
    val canUseBiometric = BiometricHelper.canAuthenticate(context)
    var pinLockEnabled by remember { mutableStateOf(BiometricHelper.isPinLockEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    // Expanded state for each section
    var expandedSection by remember { mutableStateOf<String?>(null) }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
            reminderEnabled = true
        } else {
            Toast.makeText(context, context.getString(R.string.profile_notification_permission), Toast.LENGTH_SHORT).show()
            reminderEnabled = false
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.profile_select_reminder_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = timePickerState.hour
                    reminderMinute = timePickerState.minute
                    ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    val accentColor = MaterialTheme.colorScheme.primary

    if (showUpdateDialog) {
        UpdateDialog(
            versionName = updateVersion,
            releaseNotes = updateNotes,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            isForceUpdate = isForceUpdate,
            onConfirm = {
                isDownloading = true
                downloadProgress = -1f
                val fileName = "DiaryApp-v$updateVersion.apk"
                downloadJob = scope.launch {
                    try {
                        ApkInstaller.downloadAndInstall(context, updateUrl, fileName)
                            .collect { state ->
                                when (state) {
                                    is DownloadState.Progress -> {
                                        downloadProgress = if (state.totalBytes > 0) {
                                            state.bytesDownloaded.toFloat() / state.totalBytes
                                        } else -1f
                                    }
                                    is DownloadState.Completed -> { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false }
                                    is DownloadState.Failed -> { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false; Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show() }
                                }
                            }
                    } catch (e: Exception) { isDownloading = false; downloadProgress = -1f; showUpdateDialog = false; Toast.makeText(context, context.getString(R.string.profile_update_failed_msg, e.message ?: ""), Toast.LENGTH_SHORT).show() }
                }
            },
            onCancelDownload = {
                downloadJob?.cancel()
                downloadJob = null
                isDownloading = false
                downloadProgress = -1f
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { pin, hint -> BiometricHelper.setPin(context, pin, hint); pinLockEnabled = true; showPinDialog = false; Toast.makeText(context, context.getString(R.string.profile_pin_set_success), Toast.LENGTH_SHORT).show() }
        )
    }

    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            title = { Text(stringResource(R.string.profile_remove_pin_title)) },
            text = { Text(stringResource(R.string.profile_remove_pin_message)) },
            confirmButton = {
                TextButton(onClick = { BiometricHelper.removePin(context); pinLockEnabled = false; showRemovePinDialog = false; Toast.makeText(context, context.getString(R.string.profile_pin_removed), Toast.LENGTH_SHORT).show() }) {
                    Text(stringResource(R.string.profile_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showRemovePinDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    val scrollState = rememberScrollState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .pointerInput(onMainScreenSwipe) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            onMainScreenSwipe?.invoke(totalDrag)
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            HeaderSection(textColor = textColor, textTertiary = textTertiary)

            Spacer(modifier = Modifier.height(28.dp))

            // Collapsible sections
            var alpha by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(20f) }
            LaunchedEffect(showContent) {
                if (showContent) { delay(60L); alpha = 1f; offsetY = 0f }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.alpha = alpha; translationY = offsetY },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Appearance section
                CollapsibleSection(
                    icon = Icons.Default.Palette,
                    iconBg = sectionIconBg(0),
                    iconTint = sectionIconTint(0),
                    title = stringResource(R.string.profile_appearance),
                    subtitle = "主题模式和字体大小",
                    isExpanded = expandedSection == "appearance",
                    onToggle = { expandedSection = if (expandedSection == "appearance") null else "appearance" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ThemeCardSelector(
                        currentMode = currentThemeMode,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        onSelectMode = { app.setThemeMode(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FontSizeSliderItem(
                        currentKey = currentFontSizeKey,
                        options = fontSizeOptions,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        onValueChange = { key ->
                            currentFontSizeKey = key
                            context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putString(APP_FONT_SIZE_PREF_KEY, key).apply()
                        }
                    )
                }

                // Data management section
                CollapsibleSection(
                    icon = Icons.Default.Backup,
                    iconBg = sectionIconBg(1),
                    iconTint = sectionIconTint(1),
                    title = stringResource(R.string.profile_data_management),
                    subtitle = "分类管理、备份和回收站",
                    isExpanded = expandedSection == "data",
                    onToggle = { expandedSection = if (expandedSection == "data") null else "data" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableSettingRow(
                        icon = Icons.Default.Label,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = stringResource(R.string.profile_tag_management),
                        subtitle = stringResource(R.string.profile_tag_management_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTagManagement
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Favorite,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "收藏夹",
                        subtitle = "查看收藏的日记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToFavorites
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Delete,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = "回收站",
                        subtitle = "恢复已删除的日记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTrash
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Backup,
                        iconBg = sectionIconBg(1),
                        iconTint = sectionIconTint(1),
                        title = stringResource(R.string.backup_title),
                        subtitle = stringResource(R.string.settings_backup_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToBackup
                    )
                }

                // Notification settings section
                CollapsibleSection(
                    icon = Icons.Default.Notifications,
                    iconBg = sectionIconBg(2),
                    iconTint = sectionIconTint(2),
                    title = "通知设置",
                    subtitle = "管理各类通知提醒",
                    isExpanded = expandedSection == "reminder",
                    onToggle = { expandedSection = if (expandedSection == "reminder") null else "reminder" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    SwitchSettingRow(
                        icon = Icons.Default.Notifications,
                        iconBg = sectionIconBg(2),
                        iconTint = sectionIconTint(2),
                        title = stringResource(R.string.profile_daily_reminder),
                        subtitle = if (reminderEnabled) stringResource(R.string.profile_reminder_time, reminderHour, reminderMinute) else stringResource(R.string.profile_reminder_off),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        checked = reminderEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                        ReminderManager.scheduleReminder(context, reminderHour, reminderMinute); reminderEnabled = true
                                    } else { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                } else { ReminderManager.scheduleReminder(context, reminderHour, reminderMinute); reminderEnabled = true }
                            } else { ReminderManager.cancelReminder(context); reminderEnabled = false }
                        },
                        subtitleClick = if (reminderEnabled) {{ showTimePicker = true }} else null
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        icon = Icons.Default.Notifications,
                        iconBg = sectionIconBg(2),
                        iconTint = sectionIconTint(2),
                        title = "天气预警",
                        subtitle = if (weatherAlertsEnabled) "恶劣天气时发送通知" else "已关闭",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        checked = weatherAlertsEnabled,
                        onCheckedChange = { newValue ->
                            NotificationPreferencesManager.setWeatherAlertsEnabled(context, newValue)
                            weatherAlertsEnabled = newValue
                        }
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        icon = Icons.Default.Notifications,
                        iconBg = sectionIconBg(2),
                        iconTint = sectionIconTint(2),
                        title = "成就解锁",
                        subtitle = if (achievementsNotifEnabled) "解锁成就时通知" else "已关闭",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        checked = achievementsNotifEnabled,
                        onCheckedChange = { newValue ->
                            NotificationPreferencesManager.setAchievementsEnabled(context, newValue)
                            achievementsNotifEnabled = newValue
                        }
                    )
                    SettingDivider()
                }

                // Privacy & Security section
                CollapsibleSection(
                    icon = Icons.Default.Security,
                    iconBg = sectionIconBg(3),
                    iconTint = sectionIconTint(3),
                    title = stringResource(R.string.profile_privacy_security),
                    subtitle = "指纹、密码和应用锁",
                    isExpanded = expandedSection == "privacy",
                    onToggle = { expandedSection = if (expandedSection == "privacy") null else "privacy" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    SwitchSettingRow(
                        icon = Icons.Default.Lock,
                        iconBg = sectionIconBg(3),
                        iconTint = sectionIconTint(3),
                        title = stringResource(R.string.profile_app_lock),
                        subtitle = if (canUseBiometric) stringResource(R.string.profile_biometric_desc) else stringResource(R.string.profile_no_biometric),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        checked = biometricLockEnabled,
                        onCheckedChange = { biometricLockEnabled = it; BiometricHelper.setLockEnabled(context, it) },
                        switchEnabled = canUseBiometric
                    )
                    SettingDivider()
                    SwitchSettingRow(
                        icon = Icons.Default.Security,
                        iconBg = sectionIconBg(3),
                        iconTint = sectionIconTint(3),
                        title = stringResource(R.string.profile_screenshot_protection),
                        subtitle = stringResource(R.string.profile_screenshot_protection_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        accentColor = accentColor,
                        checked = screenshotProtectionEnabled,
                        onCheckedChange = { screenshotProtectionEnabled = it; ScreenshotProtectionHelper.setEnabled(context, it) }
                    )
                    SettingDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconCircle(icon = Icons.Default.Lock, bg = sectionIconBg(3), tint = sectionIconTint(3))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.profile_pin_lock), fontSize = 15.sp, color = textColor)
                                Text(
                                    if (pinLockEnabled) stringResource(R.string.profile_pin_set) else stringResource(R.string.profile_pin_set_desc),
                                    fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        TextButton(onClick = { if (pinLockEnabled) showRemovePinDialog = true else showPinDialog = true }) {
                            Text(
                                text = if (pinLockEnabled) stringResource(R.string.profile_pin_remove) else stringResource(R.string.profile_pin_setup),
                                fontSize = 13.sp,
                                color = if (pinLockEnabled) MaterialTheme.colorScheme.error else accentColor
                            )
                        }
                    }
                }

                // About section
                CollapsibleSection(
                    icon = Icons.Default.Info,
                    iconBg = sectionIconBg(4),
                    iconTint = sectionIconTint(4),
                    title = stringResource(R.string.profile_about),
                    subtitle = "版本和更新信息",
                    isExpanded = expandedSection == "about",
                    onToggle = { expandedSection = if (expandedSection == "about") null else "about" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableSettingRow(
                        icon = Icons.Default.SystemUpdate,
                        iconBg = sectionIconBg(4),
                        iconTint = sectionIconTint(4),
                        title = stringResource(R.string.profile_check_update),
                        subtitle = if (isChecking) stringResource(R.string.profile_checking) else stringResource(R.string.profile_check_update_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        trailing = {
                            if (isChecking) CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            if (!isChecking) {
                                isChecking = true
                                scope.launch {
                                    try {
                                        val result = UpdateChecker.checkForUpdateDetailed(context, BuildConfig.VERSION_NAME)
                                        isChecking = false
                                        when (result) {
                                            is UpdateCheckResult.UpdateAvailable -> {
                                                updateVersion = result.info.versionName
                                                updateNotes = result.info.releaseNotes
                                                updateUrl = result.info.downloadUrl
                                                isForceUpdate = result.info.isForceUpdate
                                                showUpdateDialog = true
                                            }
                                            else -> Toast.makeText(context, result.toUserMessage(context), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) { isChecking = false; Toast.makeText(context, context.getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.History,
                        iconBg = sectionIconBg(4),
                        iconTint = sectionIconTint(4),
                        title = stringResource(R.string.profile_changelog),
                        subtitle = stringResource(R.string.profile_changelog_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToChangelog
                    )
                }
            }

            // Version badge + footer
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.made_with_love), fontSize = 11.sp, color = textTertiary)
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                Text(stringResource(R.string.made_by), fontSize = 11.sp, color = textTertiary)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- Collapsible Section ---

@Composable
private fun CollapsibleSection(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column {
            // Header row
            val headerInteraction = remember { MutableInteractionSource() }
            val headerPressed by headerInteraction.collectIsPressedAsState()
            val headerBg by animateColorAsState(
                targetValue = if (headerPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "headerBg"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(headerBg)
                    .clickable(interactionSource = headerInteraction, indication = null) { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(icon = icon, bg = iconBg, tint = iconTint)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(250, delayMillis = 50)),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// --- Clickable Setting Row ---

@Composable
private fun ClickableSettingRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    textTertiary: Color,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconCircle(icon = icon, bg = iconBg, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, color = textColor)
                Text(subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
            }
        }
        trailing?.invoke()
    }
}

// --- Switch Setting Row ---

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    textTertiary: Color,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchEnabled: Boolean = true,
    subtitleClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconCircle(icon = icon, bg = iconBg, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, color = textColor)
                Text(
                    subtitle, fontSize = 11.sp, color = textTertiary,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .then(if (subtitleClick != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { subtitleClick() } else Modifier)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = switchEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

// --- Header ---

@Composable
private fun HeaderSection(textColor: Color, textTertiary: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarRing")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "ringRotation"
    )
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "ringPulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val ringColor1 = MaterialTheme.colorScheme.primary
        val ringColor2 = MaterialTheme.colorScheme.secondary
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer { rotationZ = ringRotation; scaleX = ringPulse; scaleY = ringPulse }
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(colors = listOf(ringColor1, ringColor2, ringColor1.copy(alpha = 0.3f), ringColor1)))
            )
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(ringColor1, ringColor2))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = textColor, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.app_subtitle), fontSize = 14.sp, color = textTertiary, letterSpacing = 0.5.sp)
    }
}

// --- Theme Card Selector ---

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ThemeCardSelector(
    currentMode: ThemeMode,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    onSelectMode: (ThemeMode) -> Unit
) {
    val isDark = currentMode.isDarkStatic()
    val currentFamily = currentMode.category.key

    val families = listOf(
        ThemeFamilyUi(
            key = "blue",
            label = "雾蓝",
            lightMode = ThemeMode.PURE_LIGHT,
            darkMode = ThemeMode.PURE_DARK,
            lightStart = com.diary.app.ui.theme.FogBlueLightBg1,
            lightEnd = com.diary.app.ui.theme.FogBlueLightAccent,
            darkStart = com.diary.app.ui.theme.FogBlueDarkBg1,
            darkEnd = com.diary.app.ui.theme.FogBlueDarkAccent
        ),
        ThemeFamilyUi(
            key = "green",
            label = "苔绿",
            lightMode = ThemeMode.MOSS_GREEN_LIGHT,
            darkMode = ThemeMode.MOSS_GREEN_DARK,
            lightStart = com.diary.app.ui.theme.MossGreenLightBg1,
            lightEnd = com.diary.app.ui.theme.MossGreenLightAccent,
            darkStart = com.diary.app.ui.theme.MossGreenDarkBg1,
            darkEnd = com.diary.app.ui.theme.MossGreenDarkAccent
        ),
        ThemeFamilyUi(
            key = "cyan",
            label = "海潮",
            lightMode = ThemeMode.OCEAN_LIGHT,
            darkMode = ThemeMode.OCEAN_DARK,
            lightStart = com.diary.app.ui.theme.OceanLightBg1,
            lightEnd = com.diary.app.ui.theme.OceanLightAccent,
            darkStart = com.diary.app.ui.theme.OceanDarkBg1,
            darkEnd = com.diary.app.ui.theme.OceanDarkAccent
        ),
        ThemeFamilyUi(
            key = "rose",
            label = "陶粉",
            lightMode = ThemeMode.PETAL_LIGHT,
            darkMode = ThemeMode.PETAL_DARK,
            lightStart = com.diary.app.ui.theme.PetalLightBg1,
            lightEnd = com.diary.app.ui.theme.PetalLightAccent,
            darkStart = com.diary.app.ui.theme.PetalDarkBg1,
            darkEnd = com.diary.app.ui.theme.PetalDarkAccent
        ),
        ThemeFamilyUi(
            key = "amber",
            label = "沙金",
            lightMode = ThemeMode.SAND_LIGHT,
            darkMode = ThemeMode.SAND_DARK,
            lightStart = com.diary.app.ui.theme.SandLightBg1,
            lightEnd = com.diary.app.ui.theme.SandLightAccent,
            darkStart = com.diary.app.ui.theme.SandDarkBg1,
            darkEnd = com.diary.app.ui.theme.SandDarkAccent
        ),
        ThemeFamilyUi(
            key = "clay",
            label = "陶土",
            lightMode = ThemeMode.CLAY_LIGHT,
            darkMode = ThemeMode.CLAY_DARK,
            lightStart = com.diary.app.ui.theme.ClayLightBg1,
            lightEnd = com.diary.app.ui.theme.ClayLightAccent,
            darkStart = com.diary.app.ui.theme.ClayDarkBg1,
            darkEnd = com.diary.app.ui.theme.ClayDarkAccent
        ),
        ThemeFamilyUi(
            key = "ink",
            label = "墨蓝",
            lightMode = ThemeMode.INK_LIGHT,
            darkMode = ThemeMode.INK_DARK,
            lightStart = com.diary.app.ui.theme.InkLightBg1,
            lightEnd = com.diary.app.ui.theme.InkLightAccent,
            darkStart = com.diary.app.ui.theme.InkDarkBg1,
            darkEnd = com.diary.app.ui.theme.InkDarkAccent
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("配色方案", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            families.forEach { family ->
                SchemeCard(
                    label = family.label,
                    previewStart = if (isDark) family.darkStart else family.lightStart,
                    previewEnd = if (isDark) family.darkEnd else family.lightEnd,
                    isSelected = currentFamily == family.key,
                    onClick = { onSelectMode(if (isDark) family.darkMode else family.lightMode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("深色模式", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                Text(if (isDark) "已开启" else "已关闭", fontSize = 12.sp, color = textTertiary)
            }
            Switch(
                checked = isDark,
                onCheckedChange = { dark ->
                    val family = families.firstOrNull { it.key == currentFamily } ?: families.first()
                    onSelectMode(if (dark) family.darkMode else family.lightMode)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
private fun SchemeCard(
    label: String,
    previewStart: Color,
    previewEnd: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when { isPressed -> 0.94f; isSelected -> 1.02f; else -> 1f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "schemeScale"
    )

    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(previewStart)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(previewEnd)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ThemeFamilyUi(
    val key: String,
    val label: String,
    val lightMode: ThemeMode,
    val darkMode: ThemeMode,
    val lightStart: Color,
    val lightEnd: Color,
    val darkStart: Color,
    val darkEnd: Color
)

// --- Font Size Slider ---

@Composable
private fun FontSizeSliderItem(
    currentKey: String, options: List<FontSizeOption>, textColor: Color, textSecondary: Color,
    textTertiary: Color, accentColor: Color, onValueChange: (String) -> Unit
) {
    val currentIndex = options.indexOfFirst { it.key == currentKey }.coerceIn(0, options.lastIndex)
    var sliderPosition by remember { mutableFloatStateOf(currentIndex.toFloat()) }
    val previewSize = options[sliderPosition.toInt().coerceIn(0, options.lastIndex)]

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(icon = Icons.Default.FormatSize, bg = sectionIconBg(0), tint = sectionIconTint(0))
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.profile_font_size), fontSize = 15.sp, color = textColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPosition,
                onValueChange = { newValue -> sliderPosition = newValue; val index = newValue.toInt().coerceIn(0, options.lastIndex); val newKey = options[index].key; if (newKey != currentKey) onValueChange(newKey) },
                valueRange = 0f..options.lastIndex.toFloat(),
                steps = if (options.size > 2) options.size - 2 else 0,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${previewSize.label} ${previewSize.sizePx}sp", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
            }
        }
    }
}

// --- Data class ---

private data class FontSizeOption(val key: String, val label: String, val sizePx: Int)

// --- PIN Setup Dialog ---

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onPinSet: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) { 0 -> stringResource(R.string.pin_setup_title); 1 -> stringResource(R.string.pin_confirm_title); else -> stringResource(R.string.pin_hint_title) },
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = when (step) { 0 -> stringResource(R.string.pin_enter_message); 1 -> stringResource(R.string.pin_confirm_message); else -> stringResource(R.string.pin_hint_message) },
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (step < 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        repeat(4) { index ->
                            val filled = index < currentInput.length
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ))
                        }
                    }
                    if (error) { Spacer(modifier = Modifier.height(8.dp)); Text(stringResource(R.string.pin_mismatch), fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    val padItems = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "DEL"))
                    padItems.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)) {
                            row.forEach { key ->
                                if (key.isEmpty()) { Spacer(modifier = Modifier.size(48.dp)) }
                                else {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                when (key) {
                                                    "DEL" -> { if (currentInput.isNotEmpty()) { currentInput = currentInput.dropLast(1); error = false } }
                                                    else -> {
                                                        if (currentInput.length < 4) {
                                                            currentInput += key; error = false
                                                            if (currentInput.length == 4) {
                                                                if (step == 0) { firstPin = currentInput; currentInput = ""; step = 1 }
                                                                else { if (currentInput == firstPin) { step = 2; currentInput = "" } else { error = true; currentInput = ""; step = 0; firstPin = "" } }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") Icon(Icons.Default.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        else Text(key, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = hint, onValueChange = { if (it.length <= 50) hint = it },
                        label = { Text(stringResource(R.string.pin_hint_label)) }, placeholder = { Text(stringResource(R.string.pin_hint_placeholder)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.pin_hint_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        },
        confirmButton = { if (step == 2) TextButton(onClick = { onPinSet(firstPin, hint) }) { Text(stringResource(R.string.pin_hint_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
