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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GetApp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.biometric.BiometricHelper
import com.diary.app.data.DiaryBackup
import com.diary.app.data.DiaryExporter
import com.diary.app.data.DiaryImporter
import com.diary.app.reminder.ReminderManager
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.SettingDivider
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.data.BackupManager
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.diary.app.R

// Semantic icon colors per section
private val AppearanceIconBg = Color(0x1A9C27B0)
private val AppearanceIconTint = Color(0xFF9C27B0)
private val DataIconBg = Color(0x1A2196F3)
private val DataIconTint = Color(0xFF2196F3)
private val ReminderIconBg = Color(0x1AFF9800)
private val ReminderIconTint = Color(0xFFFF9800)
private val PrivacyIconBg = Color(0x1AF44336)
private val PrivacyIconTint = Color(0xFFF44336)
private val AboutIconBg = Color(0x1A4CAF50)
private val AboutIconTint = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val currentThemeMode by app.themeMode.collectAsState()
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var updateNotes by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var isForceUpdate by remember { mutableStateOf(false) }
    val fontSizeOptions = listOf(
        FontSizeOption("tiny", "极小", 10),
        FontSizeOption("small", "小", 14),
        FontSizeOption("medium", "中", 16),
        FontSizeOption("large", "大", 18),
        FontSizeOption("extra_large", "特大", 20)
    )
    var currentFontSizeKey by remember {
        mutableStateOf(
            context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
                .getString("editor_font_size", "small") ?: "small"
        )
    }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<DiaryBackup?>(null) }

    var reminderEnabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).first) }
    var reminderMinute by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).second) }
    var showTimePicker by remember { mutableStateOf(false) }

    var biometricLockEnabled by remember { mutableStateOf(BiometricHelper.isBiometricLockEnabled(context)) }
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val backup = DiaryImporter.readAndValidate(context, uri)
                pendingBackup = backup
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.profile_read_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val accentColor = MaterialTheme.colorScheme.primary

    if (showUpdateDialog) {
        UpdateDialog(
            versionName = updateVersion,
            releaseNotes = updateNotes,
            isDownloading = isDownloading,
            isForceUpdate = isForceUpdate,
            onConfirm = {
                isDownloading = true
                val fileName = "DiaryApp-v$updateVersion.apk"
                scope.launch {
                    try {
                        ApkInstaller.downloadAndInstall(context, updateUrl, fileName)
                            .collect { state ->
                                when (state) {
                                    is DownloadState.Completed -> { isDownloading = false; showUpdateDialog = false }
                                    is DownloadState.Failed -> { isDownloading = false; showUpdateDialog = false; Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show() }
                                }
                            }
                    } catch (e: Exception) { isDownloading = false; showUpdateDialog = false; Toast.makeText(context, context.getString(R.string.profile_update_failed_msg, e.message ?: ""), Toast.LENGTH_SHORT).show() }
                }
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

    pendingBackup?.let { backup ->
        val entryCount = backup.entries?.size ?: 0
        val tagCount = backup.tags?.size ?: 0
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text(stringResource(R.string.profile_import_confirm)) },
            text = { Text(stringResource(R.string.profile_import_confirm_message, entryCount, tagCount)) },
            confirmButton = {
                TextButton(onClick = {
                    val b = backup; pendingBackup = null; isImporting = true
                    scope.launch {
                        try { val result = DiaryImporter.import(app.database, b); isImporting = false; Toast.makeText(context, context.getString(R.string.profile_import_success, result.entryCount, result.tagCount), Toast.LENGTH_SHORT).show() }
                        catch (e: Exception) { isImporting = false; Toast.makeText(context, context.getString(R.string.profile_import_failed, e.message ?: ""), Toast.LENGTH_SHORT).show() }
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingBackup = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
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
                    iconBg = AppearanceIconBg,
                    iconTint = AppearanceIconTint,
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
                                .edit().putString("editor_font_size", key).apply()
                        }
                    )
                }

                // Data management section
                CollapsibleSection(
                    icon = Icons.Default.Backup,
                    iconBg = DataIconBg,
                    iconTint = DataIconTint,
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
                        iconBg = DataIconBg,
                        iconTint = DataIconTint,
                        title = stringResource(R.string.profile_tag_management),
                        subtitle = stringResource(R.string.profile_tag_management_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTagManagement
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Favorite,
                        iconBg = DataIconBg,
                        iconTint = DataIconTint,
                        title = "收藏夹",
                        subtitle = "查看收藏的日记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToFavorites
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Delete,
                        iconBg = DataIconBg,
                        iconTint = DataIconTint,
                        title = "回收站",
                        subtitle = "恢复已删除的日记",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTrash
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.Backup,
                        iconBg = DataIconBg,
                        iconTint = DataIconTint,
                        title = stringResource(R.string.profile_export_backup),
                        subtitle = if (isExporting) stringResource(R.string.profile_exporting) else stringResource(R.string.profile_export_backup_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        enabled = !isExporting,
                        trailing = {
                            if (isExporting) CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                scope.launch {
                                    try { val dao = app.database.diaryDao(); val path = DiaryExporter.export(context, dao); isExporting = false; Toast.makeText(context, context.getString(R.string.profile_export_success, path), Toast.LENGTH_LONG).show() }
                                    catch (e: Exception) { isExporting = false; Toast.makeText(context, context.getString(R.string.profile_export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.GetApp,
                        iconBg = DataIconBg,
                        iconTint = DataIconTint,
                        title = stringResource(R.string.profile_import_backup),
                        subtitle = if (isImporting) stringResource(R.string.profile_importing) else stringResource(R.string.profile_import_backup_desc),
                        textColor = textColor,
                        textTertiary = textTertiary,
                        enabled = !isImporting,
                        trailing = {
                            if (isImporting) CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        },
                        onClick = { if (!isImporting) filePickerLauncher.launch(arrayOf("application/json")) }
                    )
                }

                // Reminders section
                CollapsibleSection(
                    icon = Icons.Default.Notifications,
                    iconBg = ReminderIconBg,
                    iconTint = ReminderIconTint,
                    title = stringResource(R.string.profile_reminder_settings),
                    subtitle = "每日写作提醒",
                    isExpanded = expandedSection == "reminder",
                    onToggle = { expandedSection = if (expandedSection == "reminder") null else "reminder" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    SwitchSettingRow(
                        icon = Icons.Default.Notifications,
                        iconBg = ReminderIconBg,
                        iconTint = ReminderIconTint,
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
                }

                // Privacy & Security section
                CollapsibleSection(
                    icon = Icons.Default.Security,
                    iconBg = PrivacyIconBg,
                    iconTint = PrivacyIconTint,
                    title = stringResource(R.string.profile_privacy_security),
                    subtitle = "应用锁和隐私保护",
                    isExpanded = expandedSection == "privacy",
                    onToggle = { expandedSection = if (expandedSection == "privacy") null else "privacy" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    SwitchSettingRow(
                        icon = Icons.Default.Lock,
                        iconBg = PrivacyIconBg,
                        iconTint = PrivacyIconTint,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconCircle(icon = Icons.Default.Lock, bg = PrivacyIconBg, tint = PrivacyIconTint)
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
                    iconBg = AboutIconBg,
                    iconTint = AboutIconTint,
                    title = stringResource(R.string.profile_about),
                    subtitle = "版本信息和更新",
                    isExpanded = expandedSection == "about",
                    onToggle = { expandedSection = if (expandedSection == "about") null else "about" },
                    textColor = textColor,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary
                ) {
                    ClickableSettingRow(
                        icon = Icons.Default.SystemUpdate,
                        iconBg = AboutIconBg,
                        iconTint = AboutIconTint,
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
                                        val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                                        isChecking = false
                                        if (result != null) { updateVersion = result.versionName; updateNotes = result.releaseNotes; updateUrl = result.downloadUrl; isForceUpdate = result.isForceUpdate; showUpdateDialog = true }
                                        else { Toast.makeText(context, context.getString(R.string.profile_latest_version), Toast.LENGTH_SHORT).show() }
                                    } catch (e: Exception) { isChecking = false; Toast.makeText(context, context.getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    )
                    SettingDivider()
                    ClickableSettingRow(
                        icon = Icons.Default.History,
                        iconBg = AboutIconBg,
                        iconTint = AboutIconTint,
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
                        .clip(RoundedCornerShape(10.dp))
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
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(12.dp))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
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
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// --- Section Group Header ---

@Composable
private fun SectionGroupHeader(
    icon: ImageVector,
    bg: Color,
    tint: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    textTertiary: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon = icon, bg = bg, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(subtitle, fontSize = 11.sp, color = textTertiary, modifier = Modifier.padding(top = 1.dp))
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) { onClick() }
            .padding(vertical = 10.dp),
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

// --- Icon Circle ---

@Composable
private fun IconCircle(icon: ImageVector, bg: Color, tint: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer { rotationZ = ringRotation; scaleX = ringPulse; scaleY = ringPulse }
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(colors = listOf(DarkAccentStart, DarkAccentEnd, DarkAccentStart.copy(alpha = 0.3f), DarkAccentStart)))
            )
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(DarkAccentStart, DarkAccentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(stringResource(R.string.app_name), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(stringResource(R.string.app_subtitle), fontSize = 14.sp, color = textTertiary, letterSpacing = 0.5.sp)
    }
}

// --- Theme Card Selector ---

@Composable
private fun ThemeCardSelector(
    currentMode: ThemeMode,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    onSelectMode: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemeCard(
                mode = mode, isSelected = currentMode == mode,
                textColor = textColor, textSecondary = textSecondary,
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode, isSelected: Boolean, textColor: Color, textSecondary: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when { isPressed -> 0.93f; isSelected -> 1.02f; else -> 1f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(targetValue = if (isSelected) DarkAccentStart else Color.Transparent, animationSpec = tween(300), label = "borderColor")
    val glowAlpha by animateFloatAsState(targetValue = if (isSelected) 0.25f else 0f, animationSpec = tween(400), label = "glowAlpha")
    val icon = when (mode) { ThemeMode.PURE_LIGHT -> Icons.Default.LightMode; ThemeMode.PURE_DARK -> Icons.Default.DarkMode }
    val (previewStart, previewEnd) = when (mode) { ThemeMode.PURE_LIGHT -> Color(0xFFF8FBFF) to Color(0xFF4A90D9); ThemeMode.PURE_DARK -> Color(0xFF0A1520) to Color(0xFF70B8D8) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRect(brush = Brush.radialGradient(
                        colors = listOf(DarkAccentStart.copy(alpha = glowAlpha), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.8f
                    ), size = size)
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
            if (isSelected) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(
                    Brush.sweepGradient(listOf(previewStart.copy(alpha = 0.4f), previewEnd.copy(alpha = 0.4f), previewStart.copy(alpha = 0.2f), previewStart.copy(alpha = 0.4f)))
                ))
            }
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Brush.linearGradient(listOf(previewStart, previewEnd))))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Icon(icon, contentDescription = mode.label, tint = if (isSelected) DarkAccentStart else textSecondary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(mode.label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) textColor else textSecondary, textAlign = TextAlign.Center, maxLines = 1)
    }
}

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
            IconCircle(icon = Icons.Default.FormatSize, bg = AppearanceIconBg, tint = AppearanceIconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.profile_font_size), fontSize = 15.sp, color = textColor)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPosition,
                onValueChange = { newValue -> sliderPosition = newValue; val index = newValue.toInt().coerceIn(0, options.lastIndex); val newKey = options[index].key; if (newKey != currentKey) onValueChange(newKey) },
                valueRange = 0f..options.lastIndex.toFloat(),
                steps = if (options.size > 2) options.size - 2 else 0,
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${previewSize.label} ${previewSize.sizePx}sp", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            options.forEach { option -> Text(option.label, fontSize = 9.sp, color = textTertiary) }
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
                    Text(stringResource(R.string.pin_hint_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = { if (step == 2) TextButton(onClick = { onPinSet(firstPin, hint) }) { Text(stringResource(R.string.pin_hint_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
