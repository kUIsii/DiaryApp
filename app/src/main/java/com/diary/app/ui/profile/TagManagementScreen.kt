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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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

private enum class TagViewMode { FLAT, TREE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication ?: return
    val dao = app.database.tagDao()
    val scope = rememberCoroutineScope()

    val allTags by dao.getAllTags().collectAsStateWithLifecycle(initialValue = emptyList())
    val rootTags by dao.getRootTags().collectAsStateWithLifecycle(initialValue = emptyList())
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }
    var mergingTag by remember { mutableStateOf<Tag?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(TagViewMode.FLAT) }
    var showMergeDialog by remember { mutableStateOf(false) }

    // Track expanded state for tree view
    val expandedTags = remember { mutableStateMapOf<Long, Boolean>() }

    // Build parent-children map for tree view
    val childrenMap = remember(allTags) {
        allTags.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            TagHeader(
                onNavigateBack = onNavigateBack,
                onCreate = { showCreateDialog = true },
                onMerge = { showMergeDialog = true }
            )

            // View mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewMode == TagViewMode.FLAT,
                    onClick = { viewMode = TagViewMode.FLAT },
                    label = { Text("列表", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ViewList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = viewMode == TagViewMode.TREE,
                    onClick = { viewMode = TagViewMode.TREE },
                    label = { Text("树形", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }

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

                if (viewMode == TagViewMode.FLAT) {
                    // Flat list view
                    items(allTags, key = { it.id }) { tag ->
                        TagRow(
                            tag = tag,
                            onEdit = { editingTag = tag },
                            canDelete = !tag.isPreset,
                            onDelete = { deletingTag = tag },
                            onSetParent = { parentTag ->
                                scope.launch {
                                    dao.setTagParent(tag.id, parentTag?.id)
                                }
                            },
                            allTags = allTags
                        )
                    }
                } else {
                    // Tree view: root tags with expandable children
                    val orphanTags = allTags.filter { it.parentId == null && it !in rootTags }
                    items(rootTags, key = { it.id }) { tag ->
                        TagTreeNode(
                            tag = tag,
                            children = childrenMap[tag.id] ?: emptyList(),
                            childrenMap = childrenMap,
                            isExpanded = expandedTags[tag.id] ?: false,
                            onToggleExpand = {
                                expandedTags[tag.id] = !(expandedTags[tag.id] ?: false)
                            },
                            onEdit = { editingTag = it },
                            onDelete = { deletingTag = it },
                            onSetParent = { child, newParent ->
                                scope.launch {
                                    dao.setTagParent(child.id, newParent?.id)
                                }
                            },
                            allTags = allTags,
                            expandedTags = expandedTags,
                            depth = 0
                        )
                    }
                    // Orphan tags (no parent, not in rootTags due to query filter)
                    if (orphanTags.isNotEmpty()) {
                        items(orphanTags, key = { "orphan_${it.id}" }) { tag ->
                            TagRow(
                                tag = tag,
                                onEdit = { editingTag = tag },
                                canDelete = !tag.isPreset,
                                onDelete = { deletingTag = tag },
                                onSetParent = { parentTag ->
                                    scope.launch {
                                        dao.setTagParent(tag.id, parentTag?.id)
                                    }
                                },
                                allTags = allTags
                            )
                        }
                    }
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
                    else "确定删除「${tag.name}」吗？标签本身会被移除，但已有关联内容不会被删除。"
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

    if (showMergeDialog) {
        TagMergeDialog(
            allTags = allTags,
            onDismiss = { showMergeDialog = false },
            onMerge = { sourceTag, targetTag ->
                scope.launch {
                    dao.reassignTags(sourceTag.id, targetTag.id)
                    dao.deleteAllRefsForTag(sourceTag.id)
                    dao.deleteTag(sourceTag)
                    dao.refreshUsageCount(targetTag.id)
                }
                showMergeDialog = false
            }
        )
    }
}

@Composable
private fun TagHeader(
    onNavigateBack: () -> Unit,
    onCreate: () -> Unit,
    onMerge: () -> Unit
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
            onClick = onMerge,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Merge,
                contentDescription = "合并标签",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

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
    onDelete: () -> Unit,
    onSetParent: (Tag?) -> Unit = {},
    allTags: List<Tag> = emptyList(),
    showParentHint: Boolean = false
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

    var showParentMenu by remember { mutableStateOf(false) }

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
                val subText = buildString {
                    if (tag.isPreset) append("预设标签")
                    else append("自定义标签")
                    if (tag.usageCount > 0) append(" · ${tag.usageCount}篇")
                }
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Set parent button (long press or dedicated button)
            if (allTags.size > 1) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { showParentMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "设置父标签",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
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

    // Parent selection dialog
    if (showParentMenu) {
        TagParentSelectionDialog(
            currentTag = tag,
            allTags = allTags,
            onDismiss = { showParentMenu = false },
            onSelect = { parentTag ->
                onSetParent(parentTag)
                showParentMenu = false
            },
            onRemoveParent = {
                onSetParent(null)
                showParentMenu = false
            }
        )
    }
}

@Composable
private fun TagParentSelectionDialog(
    currentTag: Tag,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onSelect: (Tag) -> Unit,
    onRemoveParent: () -> Unit
) {
    val availableParents = filterAvailableParentTags(currentTag, allTags)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置父标签") },
        text = {
            Column {
                Text(
                    text = "为「${currentTag.name}」选择父标签：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (currentTag.parentId != null) {
                    TextButton(
                        onClick = onRemoveParent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("移除父标签（设为顶级）")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableParents, key = { it.id }) { parent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(parent) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(parent.color))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = parent.name,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TagTreeNode(
    tag: Tag,
    children: List<Tag>,
    childrenMap: Map<Long, List<Tag>>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: (Tag) -> Unit,
    onDelete: (Tag) -> Unit,
    onSetParent: (Tag, Tag?) -> Unit,
    allTags: List<Tag>,
    expandedTags: MutableMap<Long, Boolean>,
    depth: Int
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 24).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse button
            if (children.isNotEmpty()) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            // Tag row
            Box(modifier = Modifier.weight(1f)) {
                TagRow(
                    tag = tag,
                    onEdit = { onEdit(tag) },
                    canDelete = !tag.isPreset,
                    onDelete = { onDelete(tag) },
                    onSetParent = { parentTag -> onSetParent(tag, parentTag) },
                    allTags = allTags
                )
            }
        }

        // Children (animated visibility)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                children.forEach { child ->
                    val grandChildren = childrenMap[child.id] ?: emptyList()
                    TagTreeNode(
                        tag = child,
                        children = grandChildren,
                        childrenMap = childrenMap,
                        isExpanded = expandedTags[child.id] ?: false,
                        onToggleExpand = {
                            expandedTags[child.id] = !(expandedTags[child.id] ?: false)
                        },
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onSetParent = onSetParent,
                        allTags = allTags,
                        expandedTags = expandedTags,
                        depth = depth + 1
                    )
                }
            }
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

    // Auto-suggest color when name changes
    val suggestedColors = remember(name) {
        if (name.isNotBlank()) TagColorSuggester.suggestColors(name)
        else emptyList()
    }

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

                // Color suggestions based on name
                if (suggestedColors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "推荐颜色",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (color == selectedColor) 2.dp else 0.dp,
                                        color = if (color == selectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = color
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(color.toInt(), hsv)
                                    }
                            )
                        }
                        Text(
                            text = "根据名称推荐",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

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
private fun TagMergeDialog(
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onMerge: (Tag, Tag) -> Unit
) {
    var sourceTag by remember { mutableStateOf<Tag?>(null) }
    var targetTag by remember { mutableStateOf<Tag?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("合并标签", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "将一个标签的所有关联转移到另一个标签，然后删除源标签。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Source tag selection
                Text(
                    text = "要合并的标签（会被删除）",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                TagDropdown(
                    selectedTag = sourceTag,
                    tags = allTags.filter { it.id != targetTag?.id },
                    onTagSelected = { sourceTag = it },
                    placeholder = "选择源标签"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target tag selection
                Text(
                    text = "目标标签（保留）",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                TagDropdown(
                    selectedTag = targetTag,
                    tags = allTags.filter { it.id != sourceTag?.id },
                    onTagSelected = { targetTag = it },
                    placeholder = "选择目标标签"
                )

                // Preview
                if (sourceTag != null && targetTag != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        innerPadding = 12.dp
                    ) {
                        Column {
                            Text(
                                text = "合并预览",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "「${sourceTag!!.name}」 -> 「${targetTag!!.name}」",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "源标签的所有关联将转移到目标标签",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (sourceTag != null && targetTag != null) {
                        onMerge(sourceTag!!, targetTag!!)
                    }
                },
                enabled = sourceTag != null && targetTag != null
            ) {
                Text("合并", color = if (sourceTag != null && targetTag != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun TagDropdown(
    selectedTag: Tag?,
    tags: List<Tag>,
    onTagSelected: (Tag) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedTag != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(selectedTag.color))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedTag.name,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(tag.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tag.name, fontSize = 14.sp)
                        }
                    },
                    onClick = {
                        onTagSelected(tag)
                        expanded = false
                    }
                )
            }
        }
    }
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
                            if (dist in (outerR - ringWidth * 2)..outerR) {
                                val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
                                val newHue = (angle + 360f) % 360f
                                onHueChange(newHue)
                            }
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
