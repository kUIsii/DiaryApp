package com.diary.app.ui.profile

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.Tag
import com.diary.app.ui.components.GradientBackground
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

    if (showRestoreDialog && pendingRestoreBackup != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false; pendingRestoreBackup = null },
            title = { Text("恢复确认") },
            text = { Text("恢复将覆盖当前所有分类，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        allTags.forEach { dao.deleteTag(it) }
                        pendingRestoreBackup!!.tags.forEach { dao.insertTag(it.copy(id = 0)) }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = textSecondary)
                }
                Text("分类管理", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showBackupDialog = true }) {
                    Text("备份", fontSize = 13.sp, color = accent)
                }
                IconButton(onClick = { showBackupListDialog = true }) {
                    Text("恢复", fontSize = 13.sp, color = accent)
                }
            }

            // Tag list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(allTags) { tag ->
                    TagItem(
                        tag = tag,
                        onEdit = { editingTag = tag },
                        onDelete = {
                            scope.launch { dao.deleteTag(tag) }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Add button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showAddDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新增分类", fontSize = 14.sp, color = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagItem(
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onEdit() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(tag.color))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = tag.name, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
        if (!tag.isPreset) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

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
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("选择颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.take(8).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
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
private fun BackupNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("备份名称") },
                singleLine = true,
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

@Composable
private fun BackupListDialog(
    backups: List<TagBackup>,
    onDismiss: () -> Unit,
    onRestore: (TagBackup) -> Unit,
    onRename: (TagBackup, String) -> Unit,
    onDelete: (TagBackup) -> Unit
) {
    var renamingBackup by remember { mutableStateOf<TagBackup?>(null) }

    if (renamingBackup != null) {
        RenameDialog(
            initialName = renamingBackup!!.name,
            onDismiss = { renamingBackup = null },
            onConfirm = { newName ->
                onRename(renamingBackup!!, newName)
                renamingBackup = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份管理") },
        text = {
            if (backups.isEmpty()) {
                Text("暂无备份", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    backups.forEach { backup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(backup.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = formatTimestamp(backup.timestamp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { renamingBackup = backup }) {
                                Text("重命名", fontSize = 12.sp)
                            }
                            TextButton(onClick = { onDelete(backup) }) {
                                Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            if (backups.isNotEmpty()) {
                TextButton(onClick = { onRestore(backups.first()) }) {
                    Text("恢复最新备份")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名备份") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("备份名称") },
                singleLine = true,
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
