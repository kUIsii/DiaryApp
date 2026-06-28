package com.diary.app.ui.memoryanchors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryAnchorsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemoryAnchorsViewModel = viewModel()
) {
    val anchors by viewModel.anchors.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "记忆锚点",
                onNavigateBack = onNavigateBack,
                action = {
                    IconButton(onClick = { viewModel.setShowAddDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加锚点")
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "什么是记忆锚点？",
                        fontSize = DesignTokens.FontBody,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                    Text(
                        text = "标记重要日记为锚点，之后提到相关主题的新日记会自动关联。追踪一个决定如何被不断重新解读。",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "我的锚点 (${anchors.size})",
                fontSize = DesignTokens.FontMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = DesignTokens.SpacingSm)
            )

            if (anchors.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "还没有记忆锚点，点击右上角 + 添加。",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
                ) {
                    items(anchors) { item ->
                        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        AnchorItem(
                            title = item.anchor.topic,
                            date = dateFormat.format(Date(item.anchor.createdAt)),
                            relatedCount = item.relatedCount
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (viewModel.showAddDialog.collectAsState().value) {
            AddAnchorDialog(
                onDismiss = { viewModel.setShowAddDialog(false) },
                onConfirm = { topic, description ->
                    viewModel.addAnchor(topic, description, 0L)
                }
            )
        }
    }
}

@Composable
private fun AnchorItem(title: String, date: String, relatedCount: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "$date · $relatedCount 篇关联",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddAnchorDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记忆锚点") },
        text = {
            Column {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("主题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(topic, description) },
                enabled = topic.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
