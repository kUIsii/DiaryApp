package com.diary.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.material3.ExperimentalMaterial3Api
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ThemeMode
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
    var isMarkdownExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<DiaryBackup?>(null) }

    // Reminder state
    var reminderEnabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).first) }
    var reminderMinute by remember { mutableIntStateOf(ReminderManager.getReminderTime(context).second) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Biometric lock state
    var biometricLockEnabled by remember { mutableStateOf(BiometricHelper.isLockEnabled(context)) }
    val canUseBiometric = BiometricHelper.canAuthenticate(context)

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
            Toast.makeText(context, "需要通知权限才能开启提醒", Toast.LENGTH_SHORT).show()
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
            title = { Text("选择提醒时间") },
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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
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
                Toast.makeText(context, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val accentColor = MaterialTheme.colorScheme.primary

    if (showUpdateDialog) {
        UpdateDialog(
            versionName = updateVersion,
            releaseNotes = updateNotes,
            isDownloading = isDownloading,
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
                        Toast.makeText(context, "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    pendingBackup?.let { backup ->
        val entryCount = backup.entries?.size ?: 0
        val tagCount = backup.tags?.size ?: 0
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text("确认导入") },
            text = {
                Text("将导入 $entryCount 篇日记和 $tagCount 个分类，现有数据不会被覆盖。确定继续？")
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
                                    "导入成功: ${result.entryCount} 篇日记, ${result.tagCount} 个新分类",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                isImporting = false
                                Toast.makeText(
                                    context,
                                    "导入失败: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) {
                    Text("取消")
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

            Spacer(modifier = Modifier.height(36.dp))

            // Group: Appearance
            StaggeredItem(index = 0, showContent = showContent) {
                SectionHeader(title = "外观设置", icon = Icons.Default.Palette, color = AppearanceIconTint)
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                SectionHeader(title = "数据管理", icon = Icons.Default.Backup, color = DataIconTint)
            }
            Spacer(modifier = Modifier.height(10.dp))
            StaggeredItem(index = 3, showContent = showContent) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column {
                        SettingItem(
                            icon = Icons.Default.Label,
                            title = "分类管理",
                            subtitle = "管理日记分类标签",
                            iconBg = DataIconBg,
                            iconTint = DataIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            onClick = onNavigateToTagManagement
                        )
                        SettingDivider()
                        SettingItem(
                            icon = Icons.Default.Backup,
                            title = "导出备份",
                            subtitle = if (isExporting) "正在导出..." else "导出全部日记为 JSON 文件",
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
                                                "导出成功: $path",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            isExporting = false
                                            Toast.makeText(
                                                context,
                                                "导出失败: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        SettingDivider()
                        SettingItem(
                            icon = Icons.Default.Description,
                            title = "导出为 Markdown",
                            subtitle = if (isMarkdownExporting) "正在导出..." else "导出全部日记为 Markdown 文件",
                            iconBg = DataIconBg,
                            iconTint = DataIconTint,
                            textColor = textColor,
                            textTertiary = textTertiary,
                            enabled = !isMarkdownExporting,
                            trailing = {
                                if (isMarkdownExporting) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            onClick = {
                                if (!isMarkdownExporting) {
                                    isMarkdownExporting = true
                                    scope.launch {
                                        try {
                                            val dao = app.database.diaryDao()
                                            val path = DiaryExporter.exportAsMarkdown(context, dao)
                                            isMarkdownExporting = false
                                            Toast.makeText(
                                                context,
                                                "导出成功: $path",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            isMarkdownExporting = false
                                            Toast.makeText(
                                                context,
                                                "导出失败: ${e.message}",
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
                            title = "导入备份",
                            subtitle = if (isImporting) "正在导入..." else "从 JSON 文件导入日记",
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
                SectionHeader(title = "提醒设置", icon = Icons.Default.Notifications, color = ReminderIconTint)
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                SectionHeader(title = "隐私与安全", icon = Icons.Default.Security, color = PrivacyIconTint)
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: About
            StaggeredItem(index = 8, showContent = showContent) {
                SectionHeader(title = "关于", icon = Icons.Default.Info, color = AboutIconTint)
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                                contentDescription = null,
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
                            title = "检查更新",
                            subtitle = if (isChecking) "正在检查..." else "检查是否有新版本",
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
                                                showUpdateDialog = true
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "已是最新版本",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            isChecking = false
                                            Toast.makeText(
                                                context,
                                                "检查更新失败",
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
                            title = "更新日志",
                            subtitle = "查看历史版本记录",
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
                                text = "Made with ",
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " by Diary Team",
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
        animationSpec = tween(durationMillis = 400),
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with outer ring glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Outer glow ring (gradient)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(breathScale)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                DarkAccentStart.copy(alpha = 0.4f),
                                DarkAccentEnd.copy(alpha = 0.4f),
                                DarkAccentStart.copy(alpha = 0.2f),
                                DarkAccentEnd.copy(alpha = 0.4f),
                                DarkAccentStart.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
            // Inner gradient avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(breathScale)
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
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "日记本",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle / signature
        Text(
            text = "记录生活的每一天",
            fontSize = 14.sp,
            color = textTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            fontSize = 12.sp,
            color = textTertiary
        )
    }
}

// --- Section Header with icon ---

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 0.8.sp
        )
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
                text = "主题模式",
                fontSize = 15.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

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
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) DarkAccentStart else Color.Transparent,
        animationSpec = tween(250),
        label = "borderColor"
    )

    val icon = when (mode) {
        ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
        ThemeMode.PURE_LIGHT -> Icons.Default.LightMode
        ThemeMode.PURE_DARK -> Icons.Default.DarkMode
        ThemeMode.GRADIENT -> Icons.Default.Palette
    }

    // Color preview pairs
    val (previewStart, previewEnd) = when (mode) {
        ThemeMode.SYSTEM -> Color(0xFF667EEA) to Color(0xFF764BA2)
        ThemeMode.PURE_LIGHT -> Color(0xFFF0F2FA) to Color(0xFFFFFFFF)
        ThemeMode.PURE_DARK -> Color(0xFF0D0D0D) to Color(0xFF1A1A3E)
        ThemeMode.GRADIENT -> Color(0xFF667EEA) to Color(0xFF764BA2)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
        // Color preview circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(previewStart, previewEnd))
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) DarkAccentStart else textSecondary.copy(alpha = 0.6f),
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
                text = "编辑器字体大小",
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
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
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
            .size(32.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// --- Setting Divider ---

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    )
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
                    text = "应用锁",
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (canUseBiometric) "使用指纹或面部识别解锁" else "设备不支持生物识别",
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
    val timeText = String.format("每天 %02d:%02d", hour, minute)

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
                    text = "每日提醒",
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (enabled) timeText else "已关闭",
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
