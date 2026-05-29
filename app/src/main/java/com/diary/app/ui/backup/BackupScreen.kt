package com.diary.app.ui.backup

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.DiaryApplication
import com.diary.app.data.BackupFrequency
import com.diary.app.data.BackupManager
import com.diary.app.data.BackupRecord
import com.diary.app.data.DiaryExporter
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val scope = rememberCoroutineScope()
    val dao = app.database.diaryDao()

    var autoBackupEnabled by remember { mutableStateOf(BackupManager.isAutoBackupEnabled(context)) }
    var frequency by remember { mutableStateOf(BackupManager.getFrequency(context)) }
    var backupHistory by remember { mutableStateOf(BackupManager.getBackupHistory(context)) }
    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var deleteTarget by remember { mutableStateOf<BackupRecord?>(null) }
    var showFrequencyDialog by remember { mutableStateOf(false) }

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                    .background(
                                        if (frequency == option) accentColor
                                        else textTertiary
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option.label,
                                fontSize = 15.sp,
                                color = textColor
                            )
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
                        contentDescription = "返回",
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
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                            Column {
                                BackupSettingRow(
                                    icon = Icons.Default.Schedule,
                                    title = stringResource(R.string.backup_auto),
                                    subtitle = if (autoBackupEnabled) stringResource(R.string.backup_auto_on, frequency.label) else stringResource(R.string.backup_auto_off),
                                    iconBg = Color(0x1A2196F3),
                                    iconTint = Color(0xFF2196F3),
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
                                            iconBg = Color(0x1AFF9800),
                                            iconTint = Color(0xFFFF9800),
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

                item { Spacer(modifier = Modifier.height(4.dp)) }

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
                                            trackColor = accentColor.copy(alpha = 0.15f),
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
                                    subtitle = stringResource(R.string.backup_now_desc),
                                    iconBg = Color(0x1A4CAF50),
                                    iconTint = Color(0xFF4CAF50),
                                    textColor = textColor,
                                    textTertiary = textTertiary,
                                    enabled = !isBackingUp,
                                    onClick = {
                                        isBackingUp = true
                                        backupProgress = 0f
                                        scope.launch {
                                            launch {
                                                while (backupProgress < 0.9f) {
                                                    delay(100)
                                                    backupProgress += 0.05f
                                                }
                                            }
                                            try {
                                                val path = DiaryExporter.export(context, dao)
                                                val entries = dao.getAllEntriesOnce()
                                                val record = BackupRecord(
                                                    fileName = File(path).name,
                                                    filePath = path,
                                                    timestamp = System.currentTimeMillis(),
                                                    entryCount = entries.size,
                                                    fileSize = 0L
                                                )
                                                BackupManager.addBackupRecord(context, record)
                                                backupHistory = BackupManager.getBackupHistory(context)
                                                backupProgress = 1f
                                                delay(300)
                                                Toast.makeText(context, context.getString(R.string.backup_success, path), Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, context.getString(R.string.backup_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                            }
                                            isBackingUp = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                if (backupHistory.isNotEmpty()) {
                    item {
                        StaggeredBackupItem(index = 2, showContent = showContent) {
                            SectionHeader(
                                title = stringResource(R.string.backup_history),
                                icon = Icons.Default.History,
                                color = Color(0xFF9C27B0)
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
                                onDelete = { deleteTarget = record }
                            )
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
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.timestamp))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1A4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
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
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = textTertiary
                    )
                    if (record.entryCount > 0) {
                        Text(
                            text = "  |  ${stringResource(R.string.backup_entry_count, record.entryCount)}",
                            fontSize = 11.sp,
                            color = textTertiary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
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
        label = "scale"
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
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
        label = "actionScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f
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
                .clip(RoundedCornerShape(10.dp))
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
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    )
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .clip(RoundedCornerShape(0.5.dp))
                .background(color.copy(alpha = 0.15f))
        )
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
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(300),
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
