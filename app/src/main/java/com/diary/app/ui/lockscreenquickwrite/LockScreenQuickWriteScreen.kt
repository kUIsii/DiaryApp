package com.diary.app.ui.lockscreenquickwrite

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenQuickWriteScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LockScreenQuickWriteViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val viewModelSearchQuery by viewModel.searchQuery.collectAsState()
    val aiCategory by viewModel.aiCategory.collectAsState()
    val isClassifying by viewModel.isClassifying.collectAsState()
    val contextualPrompt by viewModel.contextualPrompt.collectAsState()
    val followUpSuggestion by viewModel.followUpSuggestion.collectAsState()
    val smartLinkSuggestion by viewModel.smartLinkSuggestion.collectAsState()

    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("快速笔记") }
    var localSearchQuery by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayNotes = remember(notes, sortMode, localSearchQuery) {
        var result = if (localSearchQuery.isNotBlank()) {
            notes.filter { it.content.contains(localSearchQuery, ignoreCase = true) }
        } else notes
        when (sortMode) {
            NoteSortMode.TIME_DESC -> result
            NoteSortMode.TIME_ASC -> result.sortedBy { it.createdAt }
            NoteSortMode.CATEGORY -> result.sortedBy { it.category }
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PageHeader(title = "锁屏快写", onNavigateBack = onNavigateBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.SpacingLg),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
                ) {
                    Text(
                        text = "快速记录灵感，可同步到日记本",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = DesignTokens.SpacingXs,
                            top = DesignTokens.SpacingSm
                        )
                    )

                    val promptText = contextualPrompt
                    if (noteText.isEmpty() && promptText != null) {
                        Text(
                            text = promptText,
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(
                                start = DesignTokens.SpacingXs,
                                bottom = DesignTokens.SpacingXs
                            )
                        )
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerXLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = {
                                    noteText = it
                                    viewModel.onTextChanged(it)
                                },
                                placeholder = { Text("输入快速笔记...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(DesignTokens.CornerLarge),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.3f
                                    )
                                )
                            )
                            if (noteText.isNotBlank()) {
                                Text(
                                    text = "已输入 ${noteText.length} 字",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.7f
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = DesignTokens.SpacingXs)
                                )
                            }

                            if (aiCategory != null || isClassifying) {
                                Row(
                                    modifier = Modifier.padding(
                                        top = DesignTokens.SpacingSm
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isClassifying) {
                                        Text(
                                            text = "AI分析中...",
                                            fontSize = DesignTokens.FontCaption,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    } else if (aiCategory != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                                .clickable { selectedCategory = aiCategory!! }
                                                .padding(
                                                    horizontal = DesignTokens.SpacingSm,
                                                    vertical = 4.dp
                                                )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "AI: $aiCategory",
                                                    fontSize = DesignTokens.FontCaption,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                                                Text(
                                                    text = "点击切换",
                                                    fontSize = DesignTokens.FontCaption,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                                NOTE_CATEGORIES.forEach { cat ->
                                    CategoryChip(
                                        label = cat,
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Button(
                                onClick = {
                                    viewModel.addNote(noteText, selectedCategory)
                                    noteText = ""
                                    selectedCategory = "快速笔记"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(DesignTokens.IconMedium)
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                Text("保存笔记")
                            }
                        }
                    }

                    if (followUpSuggestion != null) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerMedium,
                            innerPadding = DesignTokens.SpacingMd
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = followUpSuggestion!!,
                                    fontSize = DesignTokens.FontBody,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissFollowUp() },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "关闭",
                                        modifier = Modifier.size(DesignTokens.IconMedium)
                                    )
                                }
                            }
                        }
                    }

                    if (smartLinkSuggestion != null) {
                        val (linkedId, linkText) = smartLinkSuggestion!!
                        val quickWriteId = remember {
                            notes.firstOrNull()?.id
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerMedium,
                            innerPadding = DesignTokens.SpacingMd
                        ) {
                            Column {
                                Text(
                                    text = linkText,
                                    fontSize = DesignTokens.FontBody,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.dismissSmartLink() }
                                    ) {
                                        Text("忽略")
                                    }
                                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                    TextButton(
                                        onClick = {
                                            if (quickWriteId != null) {
                                                viewModel.acceptSmartLink(quickWriteId, linkedId)
                                            }
                                        }
                                    ) {
                                        Text("关联")
                                    }
                                }
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerXLarge,
                        innerPadding = DesignTokens.SpacingMd
                    ) {
                        Column {
                            OutlinedTextField(
                                value = localSearchQuery,
                                onValueChange = {
                                    viewModel.setSearchQuery(it)
                                    localSearchQuery = it
                                },
                                placeholder = { Text("搜索笔记...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(DesignTokens.IconLarge)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "排序:",
                                    fontSize = DesignTokens.FontSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                NoteSortMode.values().forEach { mode ->
                                    val label = when (mode) {
                                        NoteSortMode.TIME_DESC -> "最新"
                                        NoteSortMode.TIME_ASC -> "最早"
                                        NoteSortMode.CATEGORY -> "分类"
                                    }
                                    CategoryChip(
                                        label = label,
                                        selected = sortMode == mode,
                                        onClick = { viewModel.setSortMode(mode) }
                                    )
                                }
                            }
                        }
                    }

                    if (notes.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = DesignTokens.CornerXLarge,
                            innerPadding = DesignTokens.SpacingLg
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "已保存的笔记 (${displayNotes.size})",
                                        fontSize = DesignTokens.FontMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.syncAllToDiary { ok ->
                                                scope.launch {
                                                    snackbar.showSnackbar(
                                                        if (ok) "已同步到日记" else "同步失败"
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(DesignTokens.CornerMedium),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(DesignTokens.IconMedium)
                                        )
                                        Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                                        Text(
                                            "同步到日记",
                                            fontSize = DesignTokens.FontSmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                                displayNotes.forEach { note ->
                                    QuickNoteCard(
                                        note = note,
                                        onSync = {
                                            viewModel.syncToDiary(note) { ok ->
                                                scope.launch {
                                                    snackbar.showSnackbar(
                                                        if (ok) "已同步" else "同步失败"
                                                    )
                                                }
                                            }
                                        },
                                        onDelete = { viewModel.deleteNote(note) }
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.CornerXLarge,
                        innerPadding = DesignTokens.SpacingLg
                    ) {
                        Column {
                            Text(
                                text = "使用说明",
                                fontSize = DesignTokens.FontMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = "1. 在通知栏或锁屏界面快速输入灵感\n2. 笔记自动保存，重启不丢失\n3. 点击同步按钮将笔记转为日记\n4. 支持单条同步或全部同步",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(DesignTokens.CornerSmall))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.SpacingMd),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = DesignTokens.FontSmall,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickNoteCard(
    note: QuickWriteEntry,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    }
    val categoryColor = when (note.category) {
        "快速笔记" -> MaterialTheme.colorScheme.primary
        "灵感" -> MaterialTheme.colorScheme.secondary
        "待办" -> MaterialTheme.colorScheme.tertiary
        "今日感想" -> MaterialTheme.colorScheme.secondary
        "梦境" -> MaterialTheme.colorScheme.tertiary
        "摘录" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.SpacingXs),
        cornerRadius = DesignTokens.CornerMedium,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(categoryColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.content,
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.category,
                        fontSize = DesignTokens.FontCaption,
                        color = categoryColor
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(
                        text = dateFormat.format(Date(note.createdAt)),
                        fontSize = DesignTokens.FontCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (note.linkedEntryId != null) {
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已关联",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(DesignTokens.IconSmall)
                        )
                    }
                }
            }
            IconButton(
                onClick = onSync,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = "同步",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconLarge)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(DesignTokens.IconLarge)
                )
            }
        }
    }
}
