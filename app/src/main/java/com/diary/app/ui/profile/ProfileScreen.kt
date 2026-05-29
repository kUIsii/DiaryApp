package com.diary.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.BuildConfig
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryBackup
import com.diary.app.data.DiaryExporter
import com.diary.app.data.DiaryImporter
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.update.ApkInstaller
import com.diary.app.update.DownloadState
import com.diary.app.update.UpdateChecker
import com.diary.app.update.UpdateDialog
import kotlinx.coroutines.launch

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
    var showThemeMenu by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<DiaryBackup?>(null) }

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
                                val dao = app.database.diaryDao()
                                val result = DiaryImporter.import(dao, b)
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

            // Header: Avatar + App name + Version
            HeaderSection(
                textColor = textColor,
                textTertiary = textTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Group: 外观
            SectionHeader(title = "外观", color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ThemeSettingItem(
                        currentMode = currentThemeMode,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        showThemeMenu = showThemeMenu,
                        onToggleMenu = { showThemeMenu = !showThemeMenu }
                    )

                    AnimatedVisibility(
                        visible = showThemeMenu,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    ) {
                        Column {
                            ThemeMode.entries.forEach { mode ->
                                ThemeModeItem(
                                    mode = mode,
                                    isSelected = currentThemeMode == mode,
                                    textSecondary = textSecondary,
                                    onClick = {
                                        app.setThemeMode(mode)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group: 数据管理
            SectionHeader(title = "数据管理", color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingItem(
                        icon = Icons.Default.Label,
                        title = "分类管理",
                        subtitle = "管理日记分类标签",
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToTagManagement
                    )
                    SettingDivider()
                    SettingItem(
                        icon = Icons.Default.Backup,
                        title = "导出备份",
                        subtitle = if (isExporting) "正在导出..." else "导出全部日记为 JSON 文件",
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
                        icon = Icons.Default.GetApp,
                        title = "导入备份",
                        subtitle = if (isImporting) "正在导入..." else "从 JSON 文件导入日记",
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

            Spacer(modifier = Modifier.height(20.dp))

            // Group: 关于
            SectionHeader(title = "关于", color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "检查更新",
                        subtitle = if (isChecking) "正在检查..." else "当前版本 v${BuildConfig.VERSION_NAME}",
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
                        textColor = textColor,
                        textTertiary = textTertiary,
                        onClick = onNavigateToChangelog
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = textTertiary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun HeaderSection(
    textColor: androidx.compose.ui.graphics.Color,
    textTertiary: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient avatar circle
        Box(
            modifier = Modifier
                .size(80.dp)
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
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "日记本",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            fontSize = 13.sp,
            color = textTertiary
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun ThemeSettingItem(
    currentMode: ThemeMode,
    textColor: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    textTertiary: androidx.compose.ui.graphics.Color,
    showThemeMenu: Boolean,
    onToggleMenu: () -> Unit
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                onToggleMenu()
            }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "主题模式",
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = currentMode.label,
                    fontSize = 12.sp,
                    color = textTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = if (showThemeMenu) Icons.Default.PhoneAndroid else Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = textTertiary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    rotationZ = if (showThemeMenu) 180f else 0f
                }
        )
    }
}

@Composable
private fun ThemeModeItem(
    mode: ThemeMode,
    isSelected: Boolean,
    textSecondary: androidx.compose.ui.graphics.Color,
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

    val dotColor by animateColorAsState(
        targetValue = if (isSelected) DarkAccentStart else textSecondary.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "dotColor"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dotScale"
    )

    val icon = when (mode) {
        ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
        ThemeMode.PURE_LIGHT -> Icons.Default.LightMode
        ThemeMode.PURE_DARK -> Icons.Default.DarkMode
        ThemeMode.GRADIENT -> Icons.Default.Palette
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) DarkAccentStart else textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = mode.label,
            fontSize = 14.sp,
            color = textSecondary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(dotScale)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textColor: androidx.compose.ui.graphics.Color,
    textTertiary: androidx.compose.ui.graphics.Color,
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
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) textColor.copy(alpha = 0.7f) else textTertiary,
                modifier = Modifier.size(22.dp)
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

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 34.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    )
}
