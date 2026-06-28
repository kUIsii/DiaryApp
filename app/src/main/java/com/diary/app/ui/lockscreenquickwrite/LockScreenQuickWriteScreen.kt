package com.diary.app.ui.lockscreenquickwrite

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
    var noteText by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.addNote(noteText); noteText = "" },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp)); Text("保存笔记")
                        }
                    }
                }

                if (notes.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, innerPadding = 16.dp) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("已保存的笔记 (${notes.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                            notes.forEach { note ->
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(note.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(dateFormat.format(Date(note.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSync) { Icon(Icons.Default.Sync, contentDescription = "同步", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
    }
}
