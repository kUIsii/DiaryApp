package com.diary.app.ui.capsule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.TimeCapsule
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadCapsuleScreen(
    capsuleId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimeCapsuleViewModel
) {
    var capsule by remember { mutableStateOf<TimeCapsule?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(capsuleId) {
        capsule = viewModel.getCapsuleById(capsuleId)
        if (capsule != null && !capsule!!.isRead) {
            viewModel.markRead(capsuleId)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后将无法恢复这封时间胶囊") },
            confirmButton = {
                TextButton(onClick = {
                    capsule?.let { viewModel.deleteCapsule(it) }
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val currentCapsule = capsule

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("来自过去的信") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        ) { innerPadding ->
            if (currentCapsule != null) {
                val createdDate = remember(currentCapsule) {
                    val date = Instant.ofEpochMilli(currentCapsule.createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val time = Instant.ofEpochMilli(currentCapsule.createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalTime()
                    "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                }
                val unlockDate = remember(currentCapsule) {
                    val date = Instant.ofEpochMilli(currentCapsule.unlockDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    GlassCard(
                        cornerRadius = 16.dp,
                        innerPadding = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = currentCapsule.title,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "写于 $createdDate",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "解锁于 $unlockDate",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = currentCapsule.content,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
