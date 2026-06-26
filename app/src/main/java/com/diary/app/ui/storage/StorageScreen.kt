package com.diary.app.ui.storage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@Composable
fun StorageScreen(
    onNavigateBack: () -> Unit,
    viewModel: StorageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val primary = MaterialTheme.colorScheme.primary

    var showClearCacheConfirm by remember { mutableStateOf(false) }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("清理缓存") },
            text = { Text("确定要清理缓存数据吗？这不会影响你的日记内容。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheConfirm = false
                }) {
                    Text("清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                    text = "存储管理",
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

                // Total storage summary
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "总占用空间",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatFileSize(state.totalSize),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )

                            if (!state.isLoading && state.totalSize > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                // Stacked bar (exclude totalAppDataSize from bar segments)
                                StorageBar(
                                    categories = state.categories.filter { it.sizeBytes > 0 && it.icon != StorageIcon.STORAGE },
                                    totalSize = state.totalSize
                                )
                            }
                        }
                    }
                }

                // Category breakdown
                if (!state.isLoading) {
                    items(state.categories) { category ->
                        StorageCategoryCard(
                            category = category,
                            totalSize = state.totalSize,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary
                        )
                    }
                }

                // Clear cache button
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "清理缓存",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Text(
                                    text = "当前缓存 ${formatFileSize(state.cacheSize)}",
                                    fontSize = 12.sp,
                                    color = textTertiary
                                )
                            }
                            TextButton(onClick = { showClearCacheConfirm = true }) {
                                Text("清理", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Database maintenance section
                item {
                    Text(
                        text = "数据库维护",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // VACUUM button
                item {
                    var showVacuumConfirm by remember { mutableStateOf(false) }

                    if (showVacuumConfirm) {
                        AlertDialog(
                            onDismissRequest = { showVacuumConfirm = false },
                            title = { Text("压缩数据库") },
                            text = { Text("VACUUM 会重组数据库文件，释放未使用的空间。对于大型数据库可能需要几秒钟。") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.vacuumDatabase()
                                    showVacuumConfirm = false
                                }) {
                                    Text("执行")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showVacuumConfirm = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "压缩数据库",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Text(
                                    text = if (state.isVacuuming) "正在压缩..."
                                    else if (state.vacuumSavedBytes > 0) "已释放 ${formatFileSize(state.vacuumSavedBytes)}"
                                    else "当前 ${formatFileSize(state.databaseSize)}",
                                    fontSize = 12.sp,
                                    color = textTertiary
                                )
                            }
                            TextButton(
                                onClick = { showVacuumConfirm = true },
                                enabled = !state.isVacuuming
                            ) {
                                Text("VACUUM", color = Color(0xFF6366F1))
                            }
                        }
                    }
                }

                // Media cleanup section
                item {
                    Text(
                        text = "媒体清理",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Orphan files cleanup
                item {
                    var showOrphanConfirm by remember { mutableStateOf(false) }
                    if (showOrphanConfirm && state.orphanFiles.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { showOrphanConfirm = false },
                            title = { Text("清理孤立文件") },
                            text = { Text("将删除 ${state.orphanFiles.size} 个孤立文件，释放 ${formatFileSize(state.orphanSizeBytes)} 空间。这些文件在数据库中没有对应记录。") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.cleanOrphanFiles()
                                    showOrphanConfirm = false
                                }) { Text("清理") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showOrphanConfirm = false }) { Text("取消") }
                            }
                        )
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null,
                                    tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("孤立文件清理", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                Text(
                                    text = if (state.isScanningOrphans) "正在扫描..."
                                    else if (state.orphanFiles.isNotEmpty()) "发现 ${state.orphanFiles.size} 个孤立文件 (${formatFileSize(state.orphanSizeBytes)})"
                                    else "扫描数据库中无记录的媒体文件",
                                    fontSize = 12.sp, color = textTertiary
                                )
                            }
                            if (state.orphanFiles.isNotEmpty()) {
                                TextButton(onClick = { showOrphanConfirm = true }) {
                                    Text("清理", color = Color(0xFFEF4444))
                                }
                            } else {
                                TextButton(
                                    onClick = { viewModel.scanOrphanFiles() },
                                    enabled = !state.isScanningOrphans
                                ) { Text("扫描", color = Color(0xFF6366F1)) }
                            }
                        }
                    }
                }

                // Duplicate detection
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BurstMode, contentDescription = null,
                                    tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("重复文件检测", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                Text(
                                    text = if (state.isScanningDuplicates) "正在扫描..."
                                    else if (state.duplicateGroups.isNotEmpty()) "发现 ${state.duplicateGroups.size} 组重复 (${formatFileSize(state.duplicateSizeBytes)})"
                                    else "检测完全相同的媒体文件",
                                    fontSize = 12.sp, color = textTertiary
                                )
                            }
                            if (state.duplicateGroups.isNotEmpty()) {
                                TextButton(onClick = {
                                    viewModel.cleanDuplicates(duplicateFilesToRemove(state.duplicateGroups))
                                }) { Text("清理", color = Color(0xFFF59E0B)) }
                            } else {
                                TextButton(
                                    onClick = { viewModel.scanDuplicates() },
                                    enabled = !state.isScanningDuplicates
                                ) { Text("扫描", color = Color(0xFF6366F1)) }
                            }
                        }
                    }
                }

                // Integrity check button
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state.isIntegrityOk) {
                                            true -> Color(0xFF10B981).copy(alpha = 0.12f)
                                            false -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                            null -> Color(0xFF94A3B8).copy(alpha = 0.12f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = when (state.isIntegrityOk) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> Color(0xFF94A3B8)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "完整性检查",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Text(
                                    text = when (state.isIntegrityOk) {
                                        true -> "数据库完整"
                                        false -> "发现问题，建议备份"
                                        null -> "检查数据库结构完整性"
                                    },
                                    fontSize = 12.sp,
                                    color = when (state.isIntegrityOk) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> textTertiary
                                    }
                                )
                            }
                            TextButton(onClick = { viewModel.checkIntegrity() }) {
                                Text("检查", color = Color(0xFF6366F1))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun StorageBar(
    categories: List<StorageCategory>,
    totalSize: Long
) {
    val colors = listOf(
        Color(0xFF6366F1), // indigo - database
        Color(0xFF10B981), // emerald - images
        Color(0xFFF59E0B), // amber - thumbnails
        Color(0xFF3B82F6), // blue - backup
        Color(0xFF94A3B8), // slate - cache
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        categories.forEachIndexed { index, category ->
            val weight = if (totalSize > 0) category.sizeBytes.toFloat() / totalSize else 0f
            if (weight > 0f) {
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(8.dp)
                        .background(colors.getOrElse(index) { Color.Gray })
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Legend
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEachIndexed { index, category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.getOrElse(index) { Color.Gray })
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = category.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StorageCategoryCard(
    category: StorageCategory,
    totalSize: Long,
    textColor: Color,
    textSecondary: Color,
    textTertiary: Color
) {
    val icon = when (category.icon) {
        StorageIcon.DATABASE -> Icons.Default.Storage
        StorageIcon.IMAGE -> Icons.Default.Image
        StorageIcon.THUMBNAIL -> Icons.Default.BurstMode
        StorageIcon.BACKUP -> Icons.Default.Backup
        StorageIcon.CACHE -> Icons.Default.Delete
        StorageIcon.STORAGE -> Icons.Default.PhoneAndroid
    }
    val iconColors = listOf(
        Color(0xFF6366F1),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF3B82F6),
        Color(0xFF94A3B8),
        Color(0xFF8B5CF6),
    )
    val iconColor = when (category.icon) {
        StorageIcon.DATABASE -> iconColors[0]
        StorageIcon.IMAGE -> iconColors[1]
        StorageIcon.THUMBNAIL -> iconColors[2]
        StorageIcon.BACKUP -> iconColors[3]
        StorageIcon.CACHE -> iconColors[4]
        StorageIcon.STORAGE -> iconColors[5]
    }
    val percentage = if (totalSize > 0) (category.sizeBytes * 100 / totalSize) else 0

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Text(
                        text = category.description,
                        fontSize = 12.sp,
                        color = textTertiary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatFileSize(category.sizeBytes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "${percentage}%",
                        fontSize = 11.sp,
                        color = textTertiary
                    )
                }
            }

            if (category.sizeBytes > 0 && totalSize > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = (category.sizeBytes.toFloat() / totalSize).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = iconColor,
                    trackColor = iconColor.copy(alpha = 0.12f),
                )
            }
        }
    }
}
