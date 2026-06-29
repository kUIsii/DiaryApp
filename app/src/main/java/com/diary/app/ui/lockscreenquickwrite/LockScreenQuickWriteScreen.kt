@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.diary.app.ui.lockscreenquickwrite

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
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
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("快速笔记") }
    var localSearchQuery by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayNotes = remember(notes, sortMode, localSearchQuery) {
        var result = if (localSearchQuery.isNotBlank()) notes.filter { it.text.contains(localSearchQuery, ignoreCase = true) } else notes
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
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("快速记录灵感，可同步到日记本", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp))

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
                    Column {
                        OutlinedTextField(value = noteText, onValueChange = { noteText = it },
                            placeholder = { Text("输入快速笔记...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                        if (noteText.isNotBlank()) {
                            Text(
                                text = "已输入 ${noteText.length} 字",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NOTE_CATEGORIES.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.addNote(noteText, selectedCategory); noteText = ""; selectedCategory = "快速笔记" },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp)); Text("保存笔记")
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 12.dp) {
                    Column {
                        OutlinedTextField(
                            value = localSearchQuery,
                            onValueChange = { viewModel.setSearchQuery(it); localSearchQuery = it },
                            placeholder = { Text("搜索笔记...") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("排序:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            NoteSortMode.values().forEach { mode ->
                                val label = when (mode) {
                                    NoteSortMode.TIME_DESC -> "最新"
                                    NoteSortMode.TIME_ASC -> "最早"
                                    NoteSortMode.CATEGORY -> "分类"
                                }
                                FilterChip(
                                    selected = sortMode == mode,
                                    onClick = { viewModel.setSortMode(mode) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                if (notes.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("已保存的笔记 (${displayNotes.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Button(onClick = {
                                    viewModel.syncAllToDiary { ok ->
                                        scope.launch { snackbar.showSnackbar(if (ok) "已同步到日记" else "同步失败") }
                                    }
                                }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp)); Text("同步到日记", fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            displayNotes.forEach { note ->
                                QuickNoteCard(note = note,
                                    onSync = {
                                        viewModel.syncToDiary(note) { ok ->
                                            scope.launch { snackbar.showSnackbar(if (ok) "已同步" else "同步失败") }
                                        }
                                    },
                                    onDelete = { viewModel.deleteNote(note) })
                            }
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
                    Column {
                        Text("使用说明", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. 在通知栏或锁屏界面快速输入灵感\n2. 笔记自动保存，重启不丢失\n3. 点击同步按钮将笔记转为日记\n4. 支持单条同步或全部同步",
                            fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun QuickNoteCard(note: QuickNote, onSync: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val categoryColor = when (note.category) {
        "快速笔记" -> MaterialTheme.colorScheme.primary
        "灵感" -> MaterialTheme.colorScheme.secondary
        "待办" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(categoryColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(note.category, fontSize = 10.sp, color = categoryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateFormat.format(Date(note.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onSync) { Icon(Icons.Default.Sync, contentDescription = "同步", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}
