package com.diary.app.ui.storage

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.ExtendedColors
import com.diary.app.ui.theme.LocalExtendedColors
import kotlin.math.max
import kotlin.math.min

// --- Theme-aware segment bar colors ---
private fun segmentColorFor(category: StorageCategory, cs: androidx.compose.material3.ColorScheme, ex: ExtendedColors): Color {
    return when (category.icon) {
        StorageIcon.DATABASE -> cs.primary
        StorageIcon.IMAGE -> cs.secondary
        StorageIcon.THUMBNAIL -> cs.tertiary
        StorageIcon.BACKUP -> ex.gradientEnd
        StorageIcon.CACHE -> cs.onSurfaceVariant
        StorageIcon.STORAGE -> cs.onSurfaceVariant
    }
}

private fun splitFileSize(text: String): Pair<String, String> {
    val m = Regex("([\\d.]+)([A-Za-z]+)").find(text)
    return if (m != null) (m.groupValues[1] to m.groupValues[2]) else (text to "")
}

// --- Main screen ---

@Composable
fun StorageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (StorageIcon) -> Unit = {},
    viewModel: StorageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val cs = MaterialTheme.colorScheme
    val ex = LocalExtendedColors.current
    val textColor = cs.onBackground
    val textSecondary = cs.onSurfaceVariant

    var dialogType by remember { mutableStateOf<ConfirmDialogType?>(null) }
    var autoMaintain by remember { mutableStateOf(viewModel.autoMaintainEnabled) }
    var toast by remember { mutableStateOf<String?>(null) }

    dialogType?.let { type ->
        ConfirmDialog(
            type = type,
            state = state,
            onConfirm = {
                when (type) {
                    ConfirmDialogType.CLEAR_CACHE -> viewModel.clearCache()
                    ConfirmDialogType.CLEAR_THUMBNAILS -> viewModel.clearThumbnails()
                    ConfirmDialogType.EMPTY_TRASH -> viewModel.emptyTrash()
                    ConfirmDialogType.CREATE_BACKUP -> viewModel.createBackup()
                }
                dialogType = null
            },
            onDismiss = { dialogType = null }
        )
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                    .background(cs.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = com.diary.app.R.drawable.ic_back),
                        contentDescription = "返回",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "存储管理",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    letterSpacing = 0.03.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // --- Hero: total used space ---
                item {
                    StorageHeroCard(state, textColor, textSecondary, ex, cs)
                }

                // --- Quick organize (actions + auto maintain) ---
                if (!state.isLoading) {
                    item {
                        SectionLabel(text = "快速整理 · 可释放 ${formatFileSize(state.cleanableSize)}", color = textSecondary)
                    }
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                            Column {
                                QuickActionRow(
                                    iconRes = com.diary.app.R.drawable.ic_broom,
                                    iconColor = ex.success,
                                    name = "清理缓存",
                                    size = formatFileSize(state.cacheSize),
                                    onClick = { dialogType = ConfirmDialogType.CLEAR_CACHE }
                                )
                                QuickActionRow(
                                    iconRes = com.diary.app.R.drawable.ic_images,
                                    iconColor = ex.warning,
                                    name = "清理缩略图",
                                    size = formatFileSize(state.imageThumbSize),
                                    onClick = { dialogType = ConfirmDialogType.CLEAR_THUMBNAILS }
                                )
                                QuickActionRow(
                                    iconRes = com.diary.app.R.drawable.ic_cloud,
                                    iconColor = cs.primary,
                                    name = "立即备份",
                                    showArrow = true,
                                    onClick = { dialogType = ConfirmDialogType.CREATE_BACKUP }
                                )
                                AutoMaintainRow(
                                    iconColor = cs.primary,
                                    checked = autoMaintain,
                                    onCheckedChange = {
                                        autoMaintain = it
                                        viewModel.setAutoMaintain(it)
                                        toast = if (it) "已开启自动维护" else "已关闭自动维护"
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Space composition (clickable) ---
                if (!state.isLoading) {
                    val detailCategories = state.categories.filter { it.icon != StorageIcon.STORAGE }
                    item {
                        SectionLabel(text = "空间构成 · 点击管理", color = textSecondary)
                    }
                    items(detailCategories) { category ->
                        StorageCategoryRow(
                            category = category,
                            totalSize = state.totalSize,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            cs = cs,
                            ex = ex,
                            onClick = { onNavigateToCategory(category.icon) }
                        )
                    }
                }

                // --- Storage trend ---
                if (!state.isLoading) {
                    item {
                        SectionLabel(text = "存储趋势", color = textSecondary)
                    }
                    item {
                        StorageTrendCard(state, textColor, textSecondary, cs, ex)
                    }
                }

                // --- Cloud sync ---
                if (!state.isLoading) {
                    item {
                        CloudSyncRow(
                            textColor = textColor,
                            textSecondary = textSecondary,
                            cs = cs,
                            ex = ex,
                            lastBackupTime = state.backupLastTime,
                            onSync = { dialogType = ConfirmDialogType.CREATE_BACKUP }
                        )
                    }
                }

                // --- Tip ---
                if (!state.isLoading && state.totalSize > 0) {
                    item {
                        val mediaPct = (state.mediaSize * 100 / state.totalSize).toInt()
                        TipRow(
                            text = "媒体文件占 $mediaPct%，建议开启自动云端备份",
                            textSecondary = textSecondary,
                            ex = ex
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
        toast?.let { msg ->
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.surface.copy(alpha = 0.96f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(msg, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

}

// --- Section label (xiaomo style: small diamond marker + uppercase-ish text) ---

@Composable
private fun SectionLabel(text: String, color: Color) {
    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, letterSpacing = 0.08.sp)
    }
}

// --- Hero card ---

@Composable
private fun StorageHeroCard(
    state: StorageState,
    textColor: Color,
    textSecondary: Color,
    extendedColors: ExtendedColors,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    val heroGradient = listOf(
        extendedColors.gradientStart.copy(alpha = 0.16f),
        extendedColors.gradientEnd.copy(alpha = 0.12f),
        colorScheme.surface.copy(alpha = 0.06f)
    )
    val (number, unit) = splitFileSize(formatFileSize(state.totalSize))
    val usedPct = if (state.totalAppDataSize > 0) (state.totalSize * 100 / state.totalAppDataSize).toInt() else 0

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, gradientColors = heroGradient) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("已用空间", fontSize = 13.sp, color = textSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(number, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "已用 $usedPct% · 应用数据 ${formatFileSize(state.totalAppDataSize)} · 日记内容很轻巧",
                fontSize = 12.sp, color = textSecondary, fontWeight = FontWeight.SemiBold
            )

            // stat pills
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(iconRes = com.diary.app.R.drawable.ic_database, text = "${state.entryCount} 篇日记", color = colorScheme.primary)
                StatPill(iconRes = com.diary.app.R.drawable.ic_media, text = "${state.imageCount} 张照片", color = colorScheme.primary)
                StatPill(iconRes = com.diary.app.R.drawable.ic_backup, text = "${state.backupCount} 个备份", color = colorScheme.primary)
            }

            if (!state.isLoading && state.totalSize > 0) {
                val barCategories = state.categories.filter { it.sizeBytes > 0 && it.icon != StorageIcon.STORAGE }
                if (barCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StorageSegmentedBar(barCategories, colorScheme, extendedColors)
                }
            }
        }
    }
}

@Composable
private fun StatPill(iconRes: Int, text: String, color: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(cs.onSurfaceVariant.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
    }
}

// --- Segmented bar ---

@Composable
private fun StorageSegmentedBar(
    categories: List<StorageCategory>,
    colorScheme: androidx.compose.material3.ColorScheme,
    extendedColors: ExtendedColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.16f))
    ) {
        categories.forEach { category ->
            Box(
                modifier = Modifier
                    .weight(category.sizeBytes.toFloat())
                    .height(14.dp)
                    .background(segmentColorFor(category, colorScheme, extendedColors))
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        categories.forEach { category ->
            val dotColor = segmentColorFor(category, colorScheme, extendedColors)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(5.dp))
                Text(category.name, fontSize = 11.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(3.dp))
                Text(formatFileSize(category.sizeBytes), fontSize = 11.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

// --- Quick action row ---

@Composable
private fun QuickActionRow(
    iconRes: Int,
    iconColor: Color,
    name: String,
    size: String? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(iconColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cs.onBackground, modifier = Modifier.weight(1f))
        if (size != null) {
            Text(size, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (showArrow) {
            Text("›", fontSize = 20.sp, color = cs.onSurfaceVariant)
        }
    }
}

// --- Auto maintain row with switch ---

@Composable
private fun AutoMaintainRow(
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(iconColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = com.diary.app.R.drawable.ic_sync), contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("自动维护", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cs.onBackground, modifier = Modifier.weight(1f))
        MaintainSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MaintainSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val trackColor = if (checked) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.32f)
    Box(
        modifier = Modifier
            .width(46.dp).height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(cs.surface)
        )
    }
}

// --- Space composition row (clickable) ---

@Composable
private fun StorageCategoryRow(
    category: StorageCategory,
    totalSize: Long,
    textColor: Color,
    textSecondary: Color,
    cs: androidx.compose.material3.ColorScheme,
    ex: ExtendedColors,
    onClick: () -> Unit
) {
    val iconTint = segmentColorFor(category, cs, ex)
    val iconBg = iconTint.copy(alpha = 0.16f)
    val percentage = if (totalSize > 0) (category.sizeBytes * 100 / totalSize).toInt() else 0
    if (category.sizeBytes <= 0) return

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, cornerRadius = 18.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    val iconRes = when (category.icon) {
                        StorageIcon.DATABASE -> com.diary.app.R.drawable.ic_database
                        StorageIcon.IMAGE -> com.diary.app.R.drawable.ic_media
                        StorageIcon.THUMBNAIL -> com.diary.app.R.drawable.ic_thumb
                        StorageIcon.BACKUP -> com.diary.app.R.drawable.ic_backup
                        StorageIcon.CACHE -> com.diary.app.R.drawable.ic_cache
                        StorageIcon.STORAGE -> com.diary.app.R.drawable.ic_database
                    }
                    Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(category.description, fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatFileSize(category.sizeBytes), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                    Text("$percentage%", fontSize = 11.sp, color = textSecondary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("›", fontSize = 20.sp, color = textSecondary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            val progress = (category.sizeBytes.toFloat() / totalSize).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(iconTint.copy(alpha = 0.12f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(iconTint))
            }
        }
    }
}

// --- Storage trend card ---

private val trendPoints = listOf(0.52f, 0.60f, 0.68f, 0.78f, 0.88f, 1.0f)

@Composable
private fun StorageTrendCard(
    state: StorageState,
    textColor: Color,
    textSecondary: Color,
    cs: androidx.compose.material3.ColorScheme,
    ex: ExtendedColors
) {
    val lineColor = cs.primary
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("近半年占用", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text("1 月 → 6 月稳步增长", fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.height(12.dp))
            StorageTrendChart(points = trendPoints, lineColor = lineColor, surfaceColor = cs.surface)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("1月", "2月", "3月", "4月", "5月", "6月").forEach {
                    Text(it, fontSize = 9.sp, color = textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StorageTrendChart(points: List<Float>, lineColor: Color, surfaceColor: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
        val w = size.width
        val h = size.height
        val n = points.size
        val padX = (w * 0.0375f).coerceAtLeast(6.dp.toPx())
        val padY = 8.dp.toPx()
        val max = points.maxOrNull() ?: 1f
        val min = points.minOrNull() ?: 0f
        val span = (max - min).coerceAtLeast(0.0001f)
        val xs = points.indices.map { padX + it * (w - 2 * padX) / (n - 1) }
        val ys = points.map { h - padY - ((it - min) / span) * (h - 2 * padY) }

        // area fill
        val fillPath = Path().apply {
            moveTo(xs.first(), h)
            xs.zip(ys).forEach { (x, y) -> lineTo(x, y) }
            lineTo(xs.last(), h)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0.0f))))

        // line
        val linePath = Path().apply {
            xs.zip(ys).forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        }
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // dots
        xs.zip(ys).forEach { (x, y) ->
            drawCircle(color = surfaceColor, radius = 5.dp.toPx())
            drawCircle(color = lineColor, radius = 3.dp.toPx())
        }
    }
}

// --- Cloud sync row ---

@Composable
private fun CloudSyncRow(
    textColor: Color,
    textSecondary: Color,
    cs: androidx.compose.material3.ColorScheme,
    ex: ExtendedColors,
    lastBackupTime: Long,
    onSync: () -> Unit
) {
    val context = LocalContext.current
    val sub = if (lastBackupTime > 0) "${formatRelativeTime(context, lastBackupTime)} · 自动同步已开启" else "尚未同步 · 自动同步已开启"
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(ex.gradientEnd.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(id = com.diary.app.R.drawable.ic_sync), contentDescription = null, tint = ex.gradientEnd, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("云端同步", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(sub, fontSize = 11.sp, color = textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 1.dp))
            }
            Text("同步", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary, modifier = Modifier.clickable { onSync() })
        }
    }
}

// --- Tip row ---

@Composable
private fun TipRow(text: String, textSecondary: Color, ex: ExtendedColors) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ex.info.copy(alpha = 0.08f))
            .border(1.dp, ex.info.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(ex.info.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("i", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ex.info, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.weight(1f))
    }
}

// --- Confirm dialog types ---

private enum class ConfirmDialogType { CLEAR_CACHE, CLEAR_THUMBNAILS, EMPTY_TRASH, CREATE_BACKUP }

@Composable
private fun ConfirmDialog(
    type: ConfirmDialogType,
    state: StorageState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val (title, message) = when (type) {
        ConfirmDialogType.CLEAR_CACHE -> "清理缓存" to "确定要清理所有临时缓存数据吗？这不会影响你的日记内容。"
        ConfirmDialogType.CLEAR_THUMBNAILS -> "清理缩略图" to "确定要清理所有图片缩略图吗？缩略图会在浏览日记时自动重新生成。"
        ConfirmDialogType.EMPTY_TRASH -> "清空回收站" to "确定要永久删除回收站中的 ${state.trashCount} 条日记吗？此操作不可撤销。"
        ConfirmDialogType.CREATE_BACKUP -> "创建备份" to "将当前所有数据打包备份到本地存储。备份文件可在设置中导出。"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (type == ConfirmDialogType.CREATE_BACKUP) "开始备份" else "确定",
                    color = if (type == ConfirmDialogType.CREATE_BACKUP) cs.primary else cs.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
