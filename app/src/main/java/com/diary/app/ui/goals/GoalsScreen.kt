package com.diary.app.ui.goals

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
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
    val subGoals by viewModel.subGoals.collectAsState()
    val expandedGoals by viewModel.expandedGoals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var addSubGoalParentId by remember { mutableStateOf<Long?>(null) }

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
                IconButton(onClick = { 
                    addSubGoalParentId = null
                    showAddDialog = true 
                }) {
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
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(goals.size) { index ->
                        val goal = goals[index]
                        GoalCard(
                            goal = goal,
                            subGoals = subGoals[goal.id] ?: emptyList(),
                            isExpanded = goal.id in expandedGoals,
                            onProgressChange = { viewModel.updateProgress(goal.id, it) },
                            onToggleExpand = { viewModel.toggleExpanded(goal.id) },
                            onDelete = { viewModel.deleteGoal(goal) },
                            onAddSubGoal = { 
                                addSubGoalParentId = goal.id
                                showAddDialog = true 
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddDialog) {
            AddGoalDialog(
                parentTitle = if (addSubGoalParentId != null) {
                    goals.find { it.id == addSubGoalParentId }?.title
                } else null,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description ->
                    viewModel.addGoal(title, description, addSubGoalParentId)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    subGoals: List<Goal>,
    isExpanded: Boolean,
    onProgressChange: (Int) -> Unit,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onAddSubGoal: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress
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

            // Sub-goals section
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleExpand) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "收起子目标" else "子目标 (${subGoals.size})",
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onAddSubGoal) {
                    Icon(
                        Icons.Default.SubdirectoryArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加子目标", fontSize = 12.sp)
                }
            }

            // Show sub-goals if expanded
            if (isExpanded && subGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                subGoals.forEach { subGoal ->
                    SubGoalItem(
                        goal = subGoal,
                        onProgressChange = { onProgressChange(it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SubGoalItem(
    goal: Goal,
    onProgressChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (goal.description.isNotBlank()) {
                Text(
                    text = goal.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = goal.progress.toFloat() / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${goal.progress}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddGoalDialog(
    parentTitle: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (parentTitle != null) "添加子目标: $parentTitle" else "添加目标") },
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
