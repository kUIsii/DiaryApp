package com.diary.app.ui.profile

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diary.app.DiaryApplication
import com.diary.app.data.Tag
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.ErrorColor
import kotlinx.coroutines.launch

private val presetColors = listOf(
    0xFF667EEA, 0xFF4E8EF7, 0xFF4A90D9, 0xFF3AAFA9,
    0xFF6C8AE4, 0xFF7C6EE6, 0xFF6FB98F, 0xFF5C7AEA,
    0xFF4C956C, 0xFF5B7CFA, 0xFF7081D4, 0xFF557CFF
)

@Composable
fun TagManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication ?: return
    val dao = app.database.diaryDao()
    val scope = rememberCoroutineScope()

    val allTags by dao.getAllTags().collectAsStateWithLifecycle(initialValue = emptyList())
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TagHeader(
                onNavigateBack = onNavigateBack,
                onCreate = { showCreateDialog = true }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    TagOverviewCard(
                        totalCount = allTags.size,
                        customCount = allTags.count { !it.isPreset },
                        presetCount = allTags.count { it.isPreset }
                    )
                }

                items(allTags, key = { it.id }) { tag ->
                    TagRow(
                        tag = tag,
                        onEdit = { editingTag = tag },
                        canDelete = !tag.isPreset,
                        onDelete = { deletingTag = tag }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    val existingColorSet = remember(allTags) { allTags.map { it.color }.toSet() }

    if (showCreateDialog) {
        TagEditDialog(
            title = "新建标签",
            initialName = "",
            initialColor = presetColors.random(),
            existingColors = existingColorSet,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                scope.launch {
                    val trimmed = name.trim()
                    val existing = dao.getTagByName(trimmed)
                    if (existing == null) {
                        dao.insertTag(Tag(name = trimmed, color = color))
                    } else {
                        dao.updateTagById(existing.id, trimmed, color)
                    }
                }
                showCreateDialog = false
            }
        )
    }

    editingTag?.let { tag ->
        TagEditDialog(
            title = "编辑标签",
            initialName = tag.name,
            initialColor = tag.color,
            existingColors = existingColorSet,
            editingTagId = tag.id,
            onDismiss = { editingTag = null },
            onConfirm = { name, color ->
                scope.launch { dao.updateTagById(tag.id, name.trim(), color) }
                editingTag = null
            }
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text("删除标签") },
            text = {
                Text(
                    if (tag.isPreset) "默认标签不能删除。"
                    else "确定删除“${tag.name}”吗？标签本身会被移除，但已有关联内容不会被删除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!tag.isPreset) {
                            scope.launch { dao.deleteTag(tag) }
                        }
                        deletingTag = null
                    },
                    enabled = !tag.isPreset
                ) {
                    Text("删除", color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TagHeader(
    onNavigateBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "标签管理",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(
            onClick = onCreate,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建标签",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TagOverviewCard(
    totalCount: Int,
    customCount: Int,
    presetCount: Int
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewPill(text = "共 $totalCount 个")
                OverviewPill(text = "$customCount 个自定义")
                OverviewPill(text = "$presetCount 个预设")
            }
        }
    }
}

@Composable
private fun OverviewPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    onEdit: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tagRowScale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEdit
            ),
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(tag.color).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(tag.color))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (tag.isPreset) "预设标签" else "自定义标签",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (canDelete) MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                    .clickable(enabled = canDelete, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除标签",
                    tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTagRow(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        innerPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Label,
                contentDescription = "新建标签",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "新建标签",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: Long,
    existingColors: Set<Long> = emptySet(),
    editingTagId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("标签名称") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "颜色",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // HSV Color Wheel
                val selectedHsv = FloatArray(3)
                android.graphics.Color.colorToHSV(selectedColor.toInt(), selectedHsv)
                var hue by remember { mutableStateOf(selectedHsv[0]) }
                var sat by remember { mutableStateOf(selectedHsv[1]) }
                var bri by remember { mutableStateOf(selectedHsv[2]) }

                HsvColorWheel(
                    hue = hue,
                    saturation = sat,
                    brightness = bri,
                    onHueChange = { newHue ->
                        hue = newHue
                        selectedColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri)).toLong() and 0xFFFFFFFFL
                    },
                    onSatBriChange = { newSat, newBri ->
                        sat = newSat
                        bri = newBri
                        selectedColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri)).toLong() and 0xFFFFFFFFL
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color preview + duplicate warning
                val isDuplicate = existingColors.contains(selectedColor) && selectedColor != initialColor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    )
                    Column {
                        Text(
                            text = "#${selectedColor.toULong().toString(16).uppercase().takeLast(6)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isDuplicate) {
                            Text(
                                text = "该颜色已被其他标签使用",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) {
                        onConfirm(trimmed, selectedColor)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit,
    onSatBriChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Hue ring
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val outerR = size.width / 2f
                            val ringWidth = outerR * 0.18f
                            if (dist in (outerR - ringWidth)..outerR || dist in (outerR - ringWidth * 2)..outerR) {
                                val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
                                val newHue = (angle + 360f) % 360f
                                onHueChange(newHue)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
                            val newHue = (angle + 360f) % 360f
                            onHueChange(newHue)
                            change.consume()
                        }
                    }
            ) {
                val outerR = size.width / 2f
                val ringWidth = outerR * 0.18f
                val innerR = outerR - ringWidth

                // Draw hue ring using sweep gradient approximation via individual arcs
                for (i in 0 until 360) {
                    val startAngle = i.toFloat() - 90f
                    val sweepAngle = 1.5f
                    val arcColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)))
                    drawArc(
                        color = arcColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset.Zero,
                        size = size.copy(width = size.width, height = size.height),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = ringWidth)
                    )
                }

                // Hue indicator
                val indicatorAngle = Math.toRadians((hue - 90f).toDouble())
                val indicatorR = outerR - ringWidth / 2f
                val ix = center.x + indicatorR * kotlin.math.cos(indicatorAngle).toFloat()
                val iy = center.y + indicatorR * kotlin.math.sin(indicatorAngle).toFloat()
                drawCircle(color = Color.White, radius = ringWidth * 0.55f, center = Offset(ix, iy))
                drawCircle(
                    color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                    radius = ringWidth * 0.4f,
                    center = Offset(ix, iy)
                )

                // Saturation-brightness square in the center
                val sqSize = innerR * 1.2f
                val sqLeft = center.x - sqSize / 2f
                val sqTop = center.y - sqSize / 2f

                // Draw SB square: horizontal = saturation, vertical = brightness
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))
                    ),
                    topLeft = Offset(sqLeft, sqTop),
                    size = androidx.compose.ui.geometry.Size(sqSize, sqSize)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    ),
                    topLeft = Offset(sqLeft, sqTop),
                    size = androidx.compose.ui.geometry.Size(sqSize, sqSize)
                )

                // SB indicator
                val sbX = sqLeft + saturation * sqSize
                val sbY = sqTop + (1f - brightness) * sqSize
                drawCircle(color = Color.White, radius = 8f, center = Offset(sbX, sbY))
                drawCircle(
                    color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))),
                    radius = 5.5f,
                    center = Offset(sbX, sbY)
                )
            }

            // Tap/drag on SB square (overlay for gesture detection)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newSat = (offset.x / size.width).coerceIn(0f, 1f)
                            val newBri = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            onSatBriChange(newSat, newBri)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newSat = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newBri = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            onSatBriChange(newSat, newBri)
                            change.consume()
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick preset colors
        val currentColorLong = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)).toLong() and 0xFFFFFFFFL
        val presets = listOf(
            0xFF667EEA, 0xFF4E8EF7, 0xFF4A90D9, 0xFF3AAFA9,
            0xFF6FB98F, 0xFF7C6EE6, 0xFFF06292, 0xFFFF8A65,
            0xFFFFD54F, 0xFF90A4AE, 0xFF78909C, 0xFF546E7A
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (color == currentColorLong) 2.dp else 0.dp,
                            color = if (color == currentColorLong) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(color.toInt(), hsv)
                            onHueChange(hsv[0])
                            onSatBriChange(hsv[1], hsv[2])
                        }
                )
            }
        }
    }
}
