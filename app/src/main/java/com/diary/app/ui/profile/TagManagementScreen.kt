package com.diary.app.ui.profile

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.Tag
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.ErrorColor
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TagBackup(
    @SerializedName("name") val name: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("tags") val tags: List<Tag>
)

private val presetColors = listOf(
    0xFF667EEA, 0xFFE74C3C, 0xFF2ECC71, 0xFFE67E22,
    0xFF9B59B6, 0xFF1ABC9C, 0xFFF1C40F, 0xFFE91E63,
    0xFF3498DB, 0xFF95A5A6, 0xFF34495E, 0xFFD35400
)

@Composable
fun TagManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.diary.app.DiaryApplication
    val database = app.database
    val dao = database.diaryDao()
    val scope = rememberCoroutineScope()

    val allTags by dao.getAllTags().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backups by remember { mutableStateOf(loadBackupList(context)) }
    var showBackupListDialog by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<TagBackup?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }

    // Stagger animation state
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    if (showAddDialog) {
        TagEditDialog(
            initialName = "",
            initialColor = presetColors.random(),
            title = "新增分类",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                scope.launch { dao.insertTag(Tag(name = name, color = color)) }
                showAddDialog = false
            }
        )
    }

    editingTag?.let { tag ->
        TagEditDialog(
            initialName = tag.name,
            initialColor = tag.color,
            title = "编辑分类",
            onDismiss = { editingTag = null },
            onConfirm = { name, color ->
                scope.launch { dao.updateTagById(tag.id, name, color) }
                editingTag = null
            }
        )
    }

    if (showBackupDialog) {
        BackupNameDialog(
            onDismiss = { showBackupDialog = false },
            onConfirm = { name ->
                scope.launch {
                    saveBackup(context, name, allTags)
                    backups = loadBackupList(context)
                }
                showBackupDialog = false
            }
        )
    }

    val currentPendingRestoreBackup = pendingRestoreBackup
    if (showRestoreDialog && currentPendingRestoreBackup != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false; pendingRestoreBackup = null },
            title = { Text("恢复确认") },
            text = { Text("恢复将覆盖当前所有分类，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        allTags.forEach { dao.deleteTag(it) }
                        currentPendingRestoreBackup.tags.forEach { dao.insertTag(it.copy(id = 0)) }
                    }
                    showRestoreDialog = false
                    pendingRestoreBackup = null
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false; pendingRestoreBackup = null }) { Text("取消") }
            }
        )
    }

    val currentDeletingTag = deletingTag
    if (currentDeletingTag != null) {
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text("删除分类") },
            text = { Text("确定要删除「${currentDeletingTag.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteTag(currentDeletingTag) }
                    deletingTag = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) { Text("取消") }
            }
        )
    }

    if (showBackupListDialog) {
        BackupListDialog(
            backups = backups,
            onDismiss = { showBackupListDialog = false },
            onRestore = { backup ->
                pendingRestoreBackup = backup
                showRestoreDialog = true
                showBackupListDialog = false
            },
            onRename = { backup, newName ->
                renameBackup(context, backup, newName)
                backups = loadBackupList(context)
            },
            onDelete = { backup ->
                deleteBackup(context, backup)
                backups = loadBackupList(context)
            }
        )
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopBar(
                textColor = textColor,
                textSecondary = textSecondary,
                accent = accent,
                onNavigateBack = onNavigateBack,
                onBackup = { showBackupDialog = true },
                onRestore = { showBackupListDialog = true }
            )

            // Tag list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(allTags) { index, tag ->
                    StaggeredTagItem(
                        index = index,
                        showContent = showContent,
                        tag = tag,
                        onEdit = { editingTag = tag },
                        onDelete = { deletingTag = tag }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    StaggeredAddButton(
                        index = allTags.size,
                        showContent = showContent,
                        accent = accent,
                        onClick = { showAddDialog = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// --- Top Bar ---

@Composable
private fun TopBar(
    textColor: Color,
    textSecondary: Color,
    accent: Color,
    onNavigateBack: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            "分类管理",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.weight(1f))

        // Capsule backup button
        CapsuleButton(text = "备份", accent = accent, onClick = onBackup)
        Spacer(modifier = Modifier.width(8.dp))
        CapsuleButton(text = "恢复", accent = accent, onClick = onRestore)
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CapsuleButton(text: String, accent: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "capsuleScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accent)
    }
}

// --- Staggered tag item wrapper ---

@Composable
private fun StaggeredTagItem(
    index: Int,
    showContent: Boolean,
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(showContent) {
        if (showContent) {
            delay(index * 50L)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "tagAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tagOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        TagItem(tag = tag, onEdit = onEdit, onDelete = onDelete)
    }
}

// --- Tag Item ---

@Composable
private fun TagItem(
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tagScale"
    )

    val textColor = MaterialTheme.colorScheme.onBackground
    val tagColor = Color(tag.color)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onEdit() },
        cornerRadius = 20.dp,
        innerPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot with outer glow ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                // Outer ring / glow
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.2f))
                )
                // Inner solid dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tag.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            if (!tag.isPreset) {
                DeleteButton(onClick = onDelete)
            }
        }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "deleteScale"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(ErrorColor.copy(alpha = 0.1f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "删除",
            tint = ErrorColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

// --- Add Button with dashed border ---

@Composable
private fun StaggeredAddButton(
    index: Int,
    showContent: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(showContent) {
        if (showContent) {
            delay(index * 50L + 100L)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "addAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "addOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        AddTagButton(accent = accent, onClick = onClick)
    }
}

@Composable
private fun AddTagButton(accent: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "addScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dashed border via Canvas
        val dashColor = accent.copy(alpha = 0.4f)
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
            val stroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f
                )
            )
            drawRoundRect(
                color = dashColor,
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = stroke
            )
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 14.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("新增分类", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = accent)
        }
    }
}

// --- Tag Edit Dialog ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditDialog(
    initialName: String,
    initialColor: Long,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "选择颜色",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetColors.forEach { color ->
                        ColorCircle(
                            color = Color(color),
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "colorScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected)
                    Modifier.border(2.5.dp, Color.White, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- Backup Name Dialog ---

@Composable
private fun BackupNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("备份分类", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("备份名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// --- Backup List Dialog ---

@Composable
private fun BackupListDialog(
    backups: List<TagBackup>,
    onDismiss: () -> Unit,
    onRestore: (TagBackup) -> Unit,
    onRename: (TagBackup, String) -> Unit,
    onDelete: (TagBackup) -> Unit
) {
    var renamingBackup by remember { mutableStateOf<TagBackup?>(null) }

    val currentRenamingBackup = renamingBackup
    if (currentRenamingBackup != null) {
        RenameDialog(
            initialName = currentRenamingBackup.name,
            onDismiss = { renamingBackup = null },
            onConfirm = { newName ->
                onRename(currentRenamingBackup, newName)
                renamingBackup = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("备份管理", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            if (backups.isEmpty()) {
                Text("暂无备份", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    itemsIndexed(backups) { index, backup ->
                        BackupCardItem(
                            backup = backup,
                            onRestore = { onRestore(backup) },
                            onRename = { renamingBackup = backup },
                            onDelete = { onDelete(backup) }
                        )
                        if (index < backups.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun BackupCardItem(
    backup: TagBackup,
    onRestore: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        backup.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(backup.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${backup.tags.size} 个分类",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallCapsuleButton(
                    text = "恢复",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onRestore
                )
                SmallCapsuleButton(
                    text = "重命名",
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onRename
                )
                SmallCapsuleButton(
                    text = "删除",
                    accent = ErrorColor,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun SmallCapsuleButton(text: String, accent: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "smallCapsuleScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.1f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accent)
    }
}

// --- Rename Dialog ---

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("重命名备份", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("备份名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// --- Utility functions ---

private fun getBackupDir(context: Context): File {
    val dir = File(context.filesDir, "tag_backups")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun saveBackup(context: Context, name: String, tags: List<Tag>) {
    val backup = TagBackup(name = name, timestamp = System.currentTimeMillis(), tags = tags)
    val json = Gson().toJson(backup)
    val fileName = "${name}_${System.currentTimeMillis()}.json"
    File(getBackupDir(context), fileName).writeText(json)
}

private fun loadBackupList(context: Context): List<TagBackup> {
    val dir = getBackupDir(context)
    return dir.listFiles()
        ?.filter { it.extension == "json" }
        ?.mapNotNull {
            try {
                Gson().fromJson(it.readText(), TagBackup::class.java)
            } catch (e: Exception) { null }
        }
        ?.sortedByDescending { it.timestamp }
        ?: emptyList()
}

private fun renameBackup(context: Context, backup: TagBackup, newName: String) {
    val dir = getBackupDir(context)
    val oldFile = dir.listFiles()?.find {
        try {
            Gson().fromJson(it.readText(), TagBackup::class.java).timestamp == backup.timestamp
        } catch (e: Exception) { false }
    }
    if (oldFile != null) {
        val newBackup = backup.copy(name = newName)
        val json = Gson().toJson(newBackup)
        val newFile = File(dir, "${newName}_${backup.timestamp}.json")
        newFile.writeText(json)
        oldFile.delete()
    }
}

private fun deleteBackup(context: Context, backup: TagBackup) {
    val dir = getBackupDir(context)
    dir.listFiles()?.find {
        try {
            Gson().fromJson(it.readText(), TagBackup::class.java).timestamp == backup.timestamp
        } catch (e: Exception) { false }
    }?.delete()
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
