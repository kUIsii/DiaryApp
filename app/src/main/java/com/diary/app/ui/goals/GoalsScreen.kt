package com.diary.app.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.Goal
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalsViewModel = viewModel()
) {
    val goals by viewModel.goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "目标追踪",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加目标")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goals list
            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有目标，点击右上角添加",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        onProgressChange = { viewModel.updateProgress(goal.id, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description ->
                    viewModel.addGoal(title, description)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    onProgressChange: (Int) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            Text(
                text = goal.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = goal.progress.toFloat() / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${goal.progress}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加目标") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("目标标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description) },
                enabled = title.isNotBlank()
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
