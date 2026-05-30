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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
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
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.data.BackupManager
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Semantic icon colors per section
private val AppearanceIconBg = Color(0x1A9C27B0)   // purple tinted
private val AppearanceIconTint = Color(0xFF9C27B0)
private val DataIconBg = Color(0x1A2196F3)          // blue tinted
private val DataIconTint = Color(0xFF2196F3)
private val ReminderIconBg = Color(0x1AFF9800)      // orange tinted
private val ReminderIconTint = Color(0xFFFF9800)
private val PrivacyIconBg = Color(0x1AF44336)       // red tinted
private val PrivacyIconTint = Color(0xFFF44336)
private val AboutIconBg = Color(0x1A4CAF50)         // green tinted
private val AboutIconTint = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {}
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
        FontSizeOption("small", "小", 14),
        FontSizeOption("medium", "中", 16),
        FontSizeOption("large", "大", 18),
        FontSizeOption("extra_large", "特大", 20)
    )
    var currentFontSizeKey by remember {
        mutableStateOf(
            context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
                .getString("editor_font_size", "medium") ?: "medium"
        )
    }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<DiaryBackup?>(null) }

    // Reminder state
    var reminderEnabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).first) }
    var reminderMinute by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).second) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Biometric lock state
    var biometricLockEnabled by remember { mutableStateOf(BiometricHelper.isBiometricLockEnabled(context)) }
    val canUseBiometric = BiometricHelper.canAuthenticate(context)
    // PIN lock state
    var pinLockEnabled by remember { mutableStateOf(BiometricHelper.isPinLockEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    // Stagger animation visibility
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
    }

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
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderHour = timePickerState.hour
                        reminderMinute = timePickerState.minute
                        ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                                    is DownloadState.Completed -> {
                                        isDownloading = false
                                        showUpdateDialog = false
                                    }
                                    is DownloadState.Failed -> {
                                        isDownloading = false
                                        showUpdateDialog = false
                                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        isDownloading = false
                        showUpdateDialog = false
                        Toast.makeText(context, context.getString(R.string.profile_update_failed_msg, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    // PIN setup dialog
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { pin, hint ->
                BiometricHelper.setPin(context, pin, hint)
                pinLockEnabled = true
                showPinDialog = false
                Toast.makeText(context, context.getString(R.string.profile_pin_set_success), Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Remove PIN confirmation dialog
    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            title = { Text(stringResource(R.string.profile_remove_pin_title)) },
            text = { Text(stringResource(R.string.profile_remove_pin_message)) },
            confirmButton = {
                TextButton(onClick = {
                    BiometricHelper.removePin(context)
                    pinLockEnabled = false
                    showRemovePinDialog = false
                    Toast.makeText(context, context.getString(R.string.profile_pin_removed), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.profile_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingBackup?.let { backup ->
        val entryCount = backup.entries?.size ?: 0
        val tagCount = backup.tags?.size ?: 0
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text(stringResource(R.string.profile_import_confirm)) },
            text = {
                Text(stringResource(R.string.profile_import_confirm_message, entryCount, tagCount))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = backup
                        pendingBackup = null
                        isImporting = true
                        scope.launch {
                            try {
                                val result = DiaryImporter.import(app.database, b)
                                isImporting = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.profile_import_success, result.entryCount, result.tagCount),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                isImporting = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.profile_import_failed, e.message ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
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

            // Header: Avatar + App name + Signature + Version
            HeaderSection(
                textColor = textColor,
                textTertiary = textTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Group: Appearance
            StaggeredItem(index = 0, showContent = showContent) {
                SectionHeader(title = stringResource(R.string.profile_appearance), icon = Icons.Default.Palette, color = AppearanceIconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StaggeredItem(index = 1, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ThemeCardSelector(
                            currentMode = currentThemeMode,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            onSelectMode = { app.setThemeMode(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FontSizeSliderItem(
                            currentKey = currentFontSizeKey,
                            options = fontSizeOptions,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            accentColor = accentColor,
                            onValueChange = { key ->
                                currentFontSizeKey = key
                                context.getSharedPreferences(
                                    "diary_prefs",
                                    android.content.Context.MODE_PRIVATE
                                )
                                    .edit()
                                    .putString("editor_font_size", key)
                                    .apply()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Data management
            StaggeredItem(index = 2, showContent = showContent) {
                SectionHeader(title = stringResource(R.string.profile_data_management), icon = Icons.Default.Backup, color = DataIconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StaggeredItem(index = 3, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.Label,
                            title = stringResource(R.string.profile_tag_management),
                            subtitle = stringResource(R.string.profile_tag_management_desc),
                            iconBg = DataIconBg,
                            iconTint = DataIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            onClick = onNavigateToTagManagement
                        )
                        SettingDivider()
                        SettingItem(
                            icon = Icons.Default.Backup,
                            title = stringResource(R.string.profile_export_backup),
                            subtitle = if (isExporting) stringResource(R.string.profile_exporting) else stringResource(R.string.profile_export_backup_desc),
                            iconBg = DataIconBg,
                            iconTint = DataIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            enabled = !isExporting,
                            trailing = {
                                if (isExporting) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                if (!isExporting) {
                                    isExporting = true
                                    scope.launch {
                                        try {
                                            val dao = app.database.diaryDao()
                                            val path = DiaryExporter.export(context, dao)
                                            isExporting = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_export_success, path),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            isExporting = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_export_failed, e.message ?: ""),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        SettingDivider()
                        SettingItem(
                            icon = Icons.Default.GetApp,
                            title = stringResource(R.string.profile_import_backup),
                            subtitle = if (isImporting) stringResource(R.string.profile_importing) else stringResource(R.string.profile_import_backup_desc),
                            iconBg = DataIconBg,
                            iconTint = DataIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            enabled = !isImporting,
                            trailing = {
                                if (isImporting) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                if (!isImporting) {
                                    filePickerLauncher.launch(arrayOf("application/json"))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Reminders
            StaggeredItem(index = 4, showContent = showContent) {
                SectionHeader(title = stringResource(R.string.profile_reminder_settings), icon = Icons.Default.Notifications, color = ReminderIconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StaggeredItem(index = 5, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        ReminderSettingItem(
                            enabled = reminderEnabled,
                            hour = reminderHour,
                            minute = reminderMinute,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            accentColor = accentColor,
                            onToggle = { newValue ->
                                if (newValue) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            ReminderManager.scheduleReminder(
                                                context,
                                                reminderHour,
                                                reminderMinute
                                            )
                                            reminderEnabled = true
                                        } else {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        ReminderManager.scheduleReminder(
                                            context,
                                            reminderHour,
                                            reminderMinute
                                        )
                                        reminderEnabled = true
                                    }
                                } else {
                                    ReminderManager.cancelReminder(context)
                                    reminderEnabled = false
                                }
                            },
                            onTimeClick = { showTimePicker = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Privacy
            StaggeredItem(index = 6, showContent = showContent) {
                SectionHeader(title = stringResource(R.string.profile_privacy_security), icon = Icons.Default.Security, color = PrivacyIconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StaggeredItem(index = 7, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        BiometricLockSettingItem(
                            enabled = biometricLockEnabled,
                            canUseBiometric = canUseBiometric,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            accentColor = accentColor,
                            onToggle = { newValue ->
                                biometricLockEnabled = newValue
                                BiometricHelper.setLockEnabled(context, newValue)
                            }
                        )
                        SettingDivider()
                        PinLockSettingItem(
                            enabled = pinLockEnabled,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            accentColor = accentColor,
                            onSetPin = { showPinDialog = true },
                            onRemovePin = { showRemovePinDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: About
            StaggeredItem(index = 8, showContent = showContent) {
                SectionHeader(title = stringResource(R.string.profile_about), icon = Icons.Default.Info, color = AboutIconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StaggeredItem(index = 9, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // App logo large icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(DarkAccentStart, DarkAccentEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "日记本",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Version card
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingItem(
                            icon = Icons.Default.SystemUpdate,
                            title = stringResource(R.string.profile_check_update),
                            subtitle = if (isChecking) stringResource(R.string.profile_checking) else stringResource(R.string.profile_check_update_desc),
                            iconBg = AboutIconBg,
                            iconTint = AboutIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            trailing = {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                if (!isChecking) {
                                    isChecking = true
                                    scope.launch {
                                        try {
                                            val result = UpdateChecker.checkForUpdate(
                                                BuildConfig.VERSION_NAME
                                            )
                                            isChecking = false
                                            if (result != null) {
                                                updateVersion = result.versionName
                                                updateNotes = result.releaseNotes
                                                updateUrl = result.downloadUrl
                                                isForceUpdate = result.isForceUpdate
                                                showUpdateDialog = true
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.profile_latest_version),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            isChecking = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_update_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        SettingDivider()
                        SettingItem(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.profile_changelog),
                            subtitle = stringResource(R.string.profile_changelog_desc),
                            iconBg = AboutIconBg,
                            iconTint = AboutIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            onClick = onNavigateToChangelog
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Made with love
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.made_with_love),
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "心形图标",
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.made_by),
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- Staggered entrance animation wrapper ---

@Composable
private fun StaggeredItem(
    index: Int,
    showContent: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(showContent) {
        if (showContent) {
            delay(index * 60L)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(durationMillis = 300),
        label = "staggerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        content()
    }
}

// --- Header ---

@Composable
private fun HeaderSection(
    textColor: Color,
    textTertiary: Color
) {
    // Rotating ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatarRing")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringPulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with animated ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(108.dp)
        ) {
            // Animated gradient ring
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        rotationZ = ringRotation
                        scaleX = ringPulse
                        scaleY = ringPulse
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                DarkAccentStart,
                                DarkAccentEnd,
                                DarkAccentStart.copy(alpha = 0.3f),
                                DarkAccentStart
                            )
                        )
                    )
            )
            // Inner avatar
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DarkAccentStart, DarkAccentEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "日记本",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle / signature
        Text(
            text = stringResource(R.string.app_subtitle),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = textTertiary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Version badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textTertiary
            )
        }
    }
}


// --- Theme Card Selector ---

@Composable
private fun ThemeCardSelector(
    currentMode: ThemeMode,
    textColor: Color,
    @Suppress("UNUSED_PARAMETER") textSecondary: Color,
    @Suppress("UNUSED_PARAMETER") textTertiary: Color,
    onSelectMode: (ThemeMode) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(
                icon = Icons.Default.Palette,
                bg = AppearanceIconBg,
                tint = AppearanceIconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.profile_theme_mode),
                fontSize = 15.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeCard(
                    mode = mode,
                    isSelected = currentMode == mode,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode,
    isSelected: Boolean,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isSelected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) DarkAccentStart else Color.Transparent,
        animationSpec = tween(300),
        label = "borderColor"
    )

    // Glow shadow for selected state
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.25f else 0f,
        animationSpec = tween(400),
        label = "glowAlpha"
    )

    val icon = when (mode) {
        ThemeMode.PURE_LIGHT -> Icons.Default.LightMode
        ThemeMode.PURE_DARK -> Icons.Default.DarkMode
    }

    // Color preview pairs
    val (previewStart, previewEnd) = when (mode) {
        ThemeMode.PURE_LIGHT -> Color(0xFFF8FBFF) to Color(0xFF4A90D9)
        ThemeMode.PURE_DARK -> Color(0xFF0A1520) to Color(0xFF70B8D8)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // Glow effect behind selected card
                if (glowAlpha > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DarkAccentStart.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(
                                size.width / 2f,
                                size.height / 2f
                            ),
                            radius = size.width * 0.8f
                        ),
                        size = size
                    )
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        // Color preview circle with ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    previewStart.copy(alpha = 0.4f),
                                    previewEnd.copy(alpha = 0.4f),
                                    previewStart.copy(alpha = 0.2f),
                                    previewStart.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(previewStart, previewEnd))
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = icon,
            contentDescription = mode.label,
            tint = if (isSelected) DarkAccentStart else textSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = mode.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) textColor else textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// --- Font Size Slider ---

@Composable
private fun FontSizeSliderItem(
    currentKey: String,
    options: List<FontSizeOption>,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    onValueChange: (String) -> Unit
) {
    val currentIndex = options.indexOfFirst { it.key == currentKey }.coerceIn(0, options.lastIndex)
    var sliderPosition by remember { mutableFloatStateOf(currentIndex.toFloat()) }

    val previewSize = options[sliderPosition.toInt().coerceIn(0, options.lastIndex)]

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(
                icon = Icons.Default.FormatSize,
                bg = AppearanceIconBg,
                tint = AppearanceIconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.profile_font_size),
                fontSize = 15.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                    val index = newValue.toInt().coerceIn(0, options.lastIndex)
                    val newKey = options[index].key
                    if (newKey != currentKey) {
                        onValueChange(newKey)
                    }
                },
                valueRange = 0f..options.lastIndex.toFloat(),
                steps = if (options.size > 2) options.size - 2 else 0,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Current size preview
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${previewSize.label} ${previewSize.sizePx}sp",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { option ->
                Text(
                    text = option.label,
                    fontSize = 10.sp,
                    color = textTertiary
                )
            }
        }
    }
}

// --- Setting Item ---

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color = Color.Transparent,
    iconTint: Color = Color.Unspecified,
    textColor: Color,
    textTertiary: Color,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressAlpha"
    )

    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha * pressAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconCircle(
                icon = icon,
                bg = iconBg,
                tint = if (iconTint != Color.Unspecified) iconTint else textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

// --- Icon Circle ---

@Composable
private fun IconCircle(
    icon: ImageVector,
    bg: Color,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative, adjacent label provides context
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}


// --- Biometric Lock Setting ---

@Composable
private fun BiometricLockSettingItem(
    enabled: Boolean,
    canUseBiometric: Boolean,
    textColor: Color,
    @Suppress("UNUSED_PARAMETER") textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconCircle(
                icon = Icons.Default.Lock,
                bg = PrivacyIconBg,
                tint = PrivacyIconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_app_lock),
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (canUseBiometric) stringResource(R.string.profile_biometric_desc) else stringResource(R.string.profile_no_biometric),
                    fontSize = 12.sp,
                    color = textTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            enabled = canUseBiometric,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

// --- Reminder Setting ---

@Composable
private fun ReminderSettingItem(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    textColor: Color,
    @Suppress("UNUSED_PARAMETER") textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    val timeText = stringResource(R.string.profile_reminder_time, hour, minute)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconCircle(
                icon = Icons.Default.Notifications,
                bg = ReminderIconBg,
                tint = ReminderIconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_daily_reminder),
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (enabled) timeText else stringResource(R.string.profile_reminder_off),
                    fontSize = 12.sp,
                    color = textTertiary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(
                            if (enabled) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTimeClick() } else Modifier
                        )
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

// --- Font size option data ---

private data class FontSizeOption(val key: String, val label: String, val sizePx: Int)

// --- PIN Lock Setting ---

@Composable
private fun PinLockSettingItem(
    enabled: Boolean,
    textColor: Color,
    textTertiary: Color,
    accentColor: Color,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconCircle(
                icon = Icons.Default.Lock,
                bg = PrivacyIconBg,
                tint = PrivacyIconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_pin_lock),
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (enabled) stringResource(R.string.profile_pin_set) else stringResource(R.string.profile_pin_set_desc),
                    fontSize = 12.sp,
                    color = textTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        TextButton(
            onClick = { if (enabled) onRemovePin() else onSetPin() }
        ) {
            Text(
                text = if (enabled) stringResource(R.string.profile_pin_remove) else stringResource(R.string.profile_pin_setup),
                fontSize = 13.sp,
                color = if (enabled) MaterialTheme.colorScheme.error else accentColor
            )
        }
    }
}

// --- PIN Setup Dialog ---

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String, String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0 = enter, 1 = confirm, 2 = hint
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    0 -> stringResource(R.string.pin_setup_title)
                    1 -> stringResource(R.string.pin_confirm_title)
                    else -> stringResource(R.string.pin_hint_title)
                },
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = when (step) {
                        0 -> stringResource(R.string.pin_enter_message)
                        1 -> stringResource(R.string.pin_confirm_message)
                        else -> stringResource(R.string.pin_hint_message)
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (step < 2) {
                    // PIN dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        repeat(4) { index ->
                            val filled = index < currentInput.length
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    if (error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pin_mismatch),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Number pad (compact)
                    val padItems = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL")
                    )
                    padItems.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        ) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(48.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                when (key) {
                                                    "DEL" -> {
                                                        if (currentInput.isNotEmpty()) {
                                                            currentInput = currentInput.dropLast(1)
                                                            error = false
                                                        }
                                                    }
                                                    else -> {
                                                        if (currentInput.length < 4) {
                                                            currentInput += key
                                                            error = false
                                                            if (currentInput.length == 4) {
                                                                if (step == 0) {
                                                                    firstPin = currentInput
                                                                    currentInput = ""
                                                                    step = 1
                                                                } else {
                                                                    if (currentInput == firstPin) {
                                                                        step = 2
                                                                        currentInput = ""
                                                                    } else {
                                                                        error = true
                                                                        currentInput = ""
                                                                        step = 0
                                                                        firstPin = ""
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Hint input step
                    OutlinedTextField(
                        value = hint,
                        onValueChange = { if (it.length <= 50) hint = it },
                        label = { Text(stringResource(R.string.pin_hint_label)) },
                        placeholder = { Text(stringResource(R.string.pin_hint_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pin_hint_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            if (step == 2) {
                TextButton(onClick = { onPinSet(firstPin, hint) }) {
                    Text(stringResource(R.string.pin_hint_done))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
