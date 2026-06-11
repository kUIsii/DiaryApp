package com.diary.app.ui.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.DiaryApplication
import com.diary.app.R
import com.diary.app.data.BackupFrequency
import com.diary.app.data.BackupManager
import com.diary.app.data.BackupRecord
import com.diary.app.data.DiaryBackup
import com.diary.app.data.DiaryImporter
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.SectionHeader
import com.diary.app.ui.components.SettingDivider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val dao = app.database.diaryDao()
    val scope = rememberCoroutineScope()

    var autoBackupEnabled by remember { mutableStateOf(BackupManager.isAutoBackupEnabled(context)) }
    var frequency by remember { mutableStateOf(BackupManager.getFrequency(context)) }
    var backupHistory by remember { mutableStateOf(BackupManager.getBackupHistory(context)) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var deleteTarget by remember { mutableStateOf<BackupRecord?>(null) }
    var renameTarget by remember { mutableStateOf<BackupRecord?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<DiaryBackup?>(null) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            pendingImport = DiaryImporter.readAndValidate(context, uri)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "读取备份失败: ${e.message ?: ""}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) { showContent = true }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val accentColor = MaterialTheme.colorScheme.primary

    deleteTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.backup_delete_title)) },
            text = { Text(stringResource(R.string.backup_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    BackupManager.deleteBackup(context, record)
                    backupHistory = BackupManager.getBackupHistory(context)
                    deleteTarget = null
                    Toast.makeText(context, context.getString(R.string.backup_deleted), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    renameTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名备份") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("备份名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        BackupManager.renameBackup(context, record, renameInput)
                        backupHistory = BackupManager.getBackupHistory(context)
                        renameTarget = null
                        Toast.makeText(context, "备份名称已更新", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "重命名失败: ${e.message ?: ""}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    pendingImport?.let { backup ->
        val entryCount = backup.entries?.size ?: 0
        val tagCount = backup.tags?.size ?: 0
        var overwriteMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("将导入 $entryCount 篇日记和 $tagCount 个分类。")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { overwriteMode = !overwriteMode }
                            .padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = overwriteMode,
                            onCheckedChange = { overwriteMode = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "覆盖现有数据",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "清空所有日记后导入，用于完全恢复",
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pending = backup
                    val overwrite = overwriteMode
                    pendingImport = null
                    isImporting = true
                    scope.launch {
                        try {
                            val result = if (overwrite) {
                                DiaryImporter.importOverwrite(app.database, pending)
                            } else {
                                DiaryImporter.import(app.database, pending)
                            }
                            Toast.makeText(
                                context,
                                if (overwrite) "覆盖导入成功: ${result.entryCount} 篇日记"
                                else "导入成功: ${result.entryCount} 篇日记, ${result.tagCount} 个新分类",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "导入失败: ${e.message ?: ""}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isImporting = false
                        }
                    }
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text(stringResource(R.string.backup_frequency)) },
            text = {
                Column {
                    BackupFrequency.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    frequency = option
                                    BackupManager.setFrequency(context, option)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (frequency == option) accentColor else textTertiary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option.label, fontSize = 15.sp, color = textColor)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.backup_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    StaggeredBackupItem(index = 0, showContent = showContent) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                            Column {
                                BackupSettingRow(
                                    icon = Icons.Default.Schedule,
                                    title = stringResource(R.string.backup_auto),
                                    subtitle = if (autoBackupEnabled) {
                                        stringResource(R.string.backup_auto_on, frequency.label)
                                    } else {
                                        stringResource(R.string.backup_auto_off)
                                    },
                                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    textColor = textColor,
                                    textTertiary = textTertiary,
                                    accentColor = accentColor,
                                    checked = autoBackupEnabled,
                                    onToggle = { enabled ->
                                        autoBackupEnabled = enabled
                                        BackupManager.setAutoBackupEnabled(context, enabled)
                                    }
                                )

                                AnimatedVisibility(
                                    visible = autoBackupEnabled,
                                    enter = expandVertically(tween(200)),
                                    exit = shrinkVertically(tween(200))
                                ) {
                                    Column {
                                        SettingDivider()
                                        BackupSettingRow(
                                            icon = Icons.Default.Schedule,
                                            title = stringResource(R.string.backup_frequency),
                                            subtitle = frequency.label,
                                            iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                            iconTint = MaterialTheme.colorScheme.tertiary,
                                            textColor = textColor,
                                            textTertiary = textTertiary,
                                            accentColor = accentColor,
                                            onClick = { showFrequencyDialog = true }
                                        )
                                    }
                                }

                                val lastBackup = BackupManager.getLastBackupTime(context)
                                if (lastBackup > 0) {
                                    SettingDivider()
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    BackupInfoRow(
                                        label = stringResource(R.string.backup_last),
                                        value = dateFormat.format(Date(lastBackup)),
                                        textSecondary = textSecondary,
                                        textTertiary = textTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    StaggeredBackupItem(index = 1, showContent = showContent) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            enableShadow = true
                        ) {
                            Column {
                                AnimatedVisibility(
                                    visible = isBackingUp,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                        LinearProgressIndicator(
                                            progress = backupProgress,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = accentColor,
                                            trackColor = accentColor.copy(alpha = 0.15f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.backup_in_progress),
                                            fontSize = 12.sp,
                                            color = textTertiary,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }

                                BackupActionButton(
                                    icon = Icons.Default.Backup,
                                    title = stringResource(R.string.backup_now),
                                    subtitle = "保存一份可管理的本地 JSON 备份",
                                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    textColor = textColor,
                                    textTertiary = textTertiary,
                                    enabled = !isBackingUp && !isImporting,
                                    onClick = {
                                        isBackingUp = true
                                        backupProgress = 0f
                                        scope.launch {
                                            try {
                                                launch {
                                                    while (backupProgress < 0.9f) {
                                                        delay(100)
                                                        backupProgress += 0.05f
                                                    }
                                                }
                                                val record = BackupManager.createBackup(context, dao)
                                                backupHistory = BackupManager.getBackupHistory(context)
                                                backupProgress = 1f
                                                delay(250)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.backup_success, record.fileName),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.backup_failed, e.message ?: ""),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                isBackingUp = false
                                            }
                                        }
                                    }
                                )

                                SettingDivider()

                                BackupActionButton(
                                    icon = Icons.Default.GetApp,
                                    title = "导入备份",
                                    subtitle = if (isImporting) {
                                        "正在导入..."
                                    } else {
                                        "从文件导入 JSON 备份"
                                    },
                                    iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    textColor = textColor,
                                    textTertiary = textTertiary,
                                    enabled = !isBackingUp && !isImporting,
                                    onClick = {
                                        if (!isBackingUp && !isImporting) {
                                            filePickerLauncher.launch(arrayOf("application/json", "text/plain"))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (backupHistory.isNotEmpty()) {
                    item {
                        StaggeredBackupItem(index = 2, showContent = showContent) {
                            SectionHeader(
                                title = stringResource(R.string.backup_history),
                                icon = Icons.Default.History,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    itemsIndexed(backupHistory) { index, record ->
                        StaggeredBackupItem(index = 3 + index, showContent = showContent) {
                            BackupHistoryItem(
                                record = record,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                                onRename = {
                                    renameTarget = record
                                    renameInput = record.fileName.removeSuffix(".json")
                                },
                                onDelete = { deleteTarget = record }
                            )
                        }
                    }
                } else {
                    item {
                        StaggeredBackupItem(index = 2, showContent = showContent) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "暂无本地备份",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "创建一个本地备份后，就能在这里重命名、删除或导入。",
                                        fontSize = 12.sp,
                                        color = textTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
private fun BackupHistoryItem(
    record: BackupRecord,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.timestamp))
    val sizeLabel = when {
        record.fileSize <= 0L -> ""
        record.fileSize < 1024L -> "${record.fileSize}B"
        record.fileSize < 1024L * 1024L -> "${record.fileSize / 1024L}KB"
        else -> String.format("%.1fMB", record.fileSize / (1024f * 1024f))
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(text = dateStr, fontSize = 11.sp, color = textTertiary)
                    if (record.entryCount > 0) {
                        Text(
                            text = "  |  ${stringResource(R.string.backup_entry_count, record.entryCount)}",
                            fontSize = 11.sp,
                            color = textTertiary
                        )
                    }
                    if (sizeLabel.isNotBlank()) {
                        Text(text = "  |  $sizeLabel", fontSize = 11.sp, color = textTertiary)
                    }
                }
            }

            IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BackupSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    textColor: Color,
    textTertiary: Color,
    accentColor: Color,
    checked: Boolean? = null,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "backupSettingScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 15.sp, color = textColor)
                Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (checked != null && onToggle != null) {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun BackupInfoRow(
    label: String,
    value: String,
    textSecondary: Color,
    textTertiary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = textTertiary)
        Text(text = value, fontSize = 13.sp, color = textSecondary)
    }
}

@Composable
private fun BackupActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    textColor: Color,
    textTertiary: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "backupActionScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 15.sp, color = textColor)
            Text(text = subtitle, fontSize = 12.sp, color = textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun StaggeredBackupItem(
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
        animationSpec = tween(300),
        label = "backupStaggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(300),
        label = "backupStaggerOffset"
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
