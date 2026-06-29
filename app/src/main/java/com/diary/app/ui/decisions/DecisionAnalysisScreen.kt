package com.diary.app.ui.decisions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.Decision
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionAnalysisScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DecisionViewModel = viewModel()
) {
    val decisions by viewModel.decisions.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val aiInsight by viewModel.aiInsight.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isAiExtracting by viewModel.isAiExtracting.collectAsState()
    val extractionProgress by viewModel.extractionProgress.collectAsState()
    val extractionTotal by viewModel.extractionTotal.collectAsState()
    val extractionMessage by viewModel.extractionMessage.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Long?>(null) }
    var showInsight by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val filterLabels = listOf("All" to "全部", "Pending" to "进行中", "Resolved" to "已解决")

    val filtered = when (selectedFilter) {
        "Pending" -> decisions.filter { it.outcome.isNullOrBlank() }
        "Resolved" -> decisions.filter { !it.outcome.isNullOrBlank() }
        else -> decisions
    }

    GradientBackground {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加决策")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = DesignTokens.SpacingLg)
                ) {
                    PageHeader(title = "决策分析", onNavigateBack = onNavigateBack)

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    StatsBar(stats)

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filterLabels.forEach { (key, label) ->
                            FilterChip(
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 13.sp) },
                                selected = selectedFilter == key
                            )
                        }
                    }

                    if (isAiExtracting) {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = if (extractionTotal > 0) extractionProgress.toFloat() / 100f else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentItem = if (extractionTotal > 0) extractionProgress * extractionTotal / 100 else 0
                            Text(
                                text = "正在扫描第 ${currentItem}/${extractionTotal} 篇日记...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    extractionMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = msg,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 21.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

                    if (decisions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "还没有决策记录",
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                    Text(
                                        text = "AI会自动从日记中识别决策，也可以手动添加",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        lineHeight = 21.sp
                                    )
                                    Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                                    if (isAiExtracting) {
                                        Text(
                                            text = "AI扫描中...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.aiExtractDecisions() },
                                            enabled = !isAiExtracting
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                                            Text("AI扫描日记")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "该分类下暂无决策",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filtered, key = { it.id }) { decision ->
                                DecisionCard(
                                    decision = decision,
                                    isExpanded = expandedId == decision.id,
                                    dateFormat = dateFormat,
                                    onToggleExpand = {
                                        expandedId = if (expandedId == decision.id) null else decision.id
                                    },
                                    onUpdateOutcome = { outcome, satisfaction ->
                                        viewModel.updateDecisionOutcome(decision.id, outcome, satisfaction)
                                    },
                                    onEdit = { showEditDialog = decision.id },
                                    onDelete = { viewModel.deleteDecision(decision.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                                AiInsightSection(
                                    insight = aiInsight,
                                    isAnalyzing = isAnalyzing,
                                    expanded = showInsight,
                                    onToggle = { showInsight = !showInsight },
                                    onAnalyze = { viewModel.analyzeDecisions() }
                                )
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDecisionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, context, options, chosen, concerns, followUp ->
                viewModel.addRichDecision(title, context, options, chosen, concerns, followUp)
                showAddDialog = false
            }
        )
    }

    val editTarget = showEditDialog?.let { id -> decisions.find { it.id == id } }
    if (editTarget != null) {
        AddDecisionDialog(
            existingDecision = editTarget,
            onDismiss = { showEditDialog = null },
            onConfirm = { title, context, options, chosen, concerns, followUp ->
                viewModel.updateDecisionFull(editTarget.id, title, context, options, chosen, concerns, followUp)
                showEditDialog = null
            }
        )
    }
}

@Composable
private fun StatsBar(stats: DecisionStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatCard(
            label = "总计",
            value = "${stats.total}",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "进行中",
            value = "${stats.pending}",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "已解决",
            value = "${stats.resolved}",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "满意度",
            value = if (stats.averageSatisfaction > 0f) String.format("%.1f", stats.averageSatisfaction) else "--",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        innerPadding = DesignTokens.SpacingSm
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DecisionCard(
    decision: Decision,
    isExpanded: Boolean,
    dateFormat: SimpleDateFormat,
    onToggleExpand: () -> Unit,
    onUpdateOutcome: (String?, Int?) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (outcomeText, satisfaction) = decodeOutcome(decision.outcome)
    val optionsList = remember(decision.options) {
        decision.options.split("\n").filter { it.isNotBlank() }
    }
    val hasOutcome = decision.outcome != null && decision.outcome.isNotBlank()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = decision.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(Date(decision.madeAt)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasOutcome) {
                            Text(
                                text = "  ·  已解决",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (satisfaction != null && satisfaction > 0) {
                            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                            SatisfactionDot(satisfaction)
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (decision.context.isNotBlank() && !isExpanded) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = decision.context.take(80) + if (decision.context.length > 80) "..." else "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            if (optionsList.isNotEmpty() && !isExpanded) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = "${optionsList.size}个选项",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DecisionDetail(
                    decision = decision,
                    optionsList = optionsList,
                    outcomeText = outcomeText,
                    satisfaction = satisfaction,
                    dateFormat = dateFormat,
                    onUpdateOutcome = onUpdateOutcome,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun DecisionDetail(
    decision: Decision,
    optionsList: List<String>,
    outcomeText: String?,
    satisfaction: Int?,
    dateFormat: SimpleDateFormat,
    onUpdateOutcome: (String?, Int?) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var editOutcome by remember(outcomeText) { mutableStateOf(outcomeText ?: "") }
    var editSatisfaction by remember(satisfaction) { mutableStateOf(satisfaction ?: 0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = DesignTokens.SpacingMd)) {
        if (decision.context.isNotBlank()) {
            Text(
                text = decision.context,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        }

        if (optionsList.isNotEmpty()) {
            Text(
                text = "考虑的选项",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            optionsList.forEach { option ->
                val isChosen = option.trim() == decision.chosenOption.trim()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    if (isChosen) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                    }
                    Text(
                        text = option.trim(),
                        fontSize = 14.sp,
                        fontWeight = if (isChosen) FontWeight.Medium else FontWeight.Normal,
                        color = if (isChosen) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        }

        if (decision.concerns.isNotBlank()) {
            Text(
                text = "顾虑",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Text(
                text = decision.concerns,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        }

        decision.followUpAt?.let { followUp ->
            val now = System.currentTimeMillis()
            val daysUntil = ((followUp - now) / (1000 * 60 * 60 * 24)).toInt()
            val (followUpColor, followUpText) = when {
                daysUntil < 0 -> Color(0xFFEF5350) to "已过期 ${-daysUntil} 天"
                daysUntil <= 7 -> Color(0xFF66BB6A) to "距离回顾日还有 ${daysUntil} 天"
                daysUntil <= 30 -> Color(0xFFFFB74D) to "距离回顾日还有 ${daysUntil} 天"
                else -> Color.Gray to "距离回顾日还有 ${daysUntil} 天"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = followUpColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                Text(
                    text = "${dateFormat.format(Date(followUp))} ${followUpText}",
                    fontSize = 14.sp,
                    color = followUpColor
                )
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        }

        Text(
            text = "结果",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
        OutlinedTextField(
            value = editOutcome,
            onValueChange = { editOutcome = it },
            placeholder = { Text("记录决策结果...", fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "满意度",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            SatisfactionRating(
                rating = editSatisfaction,
                onRatingChange = { editSatisfaction = it }
            )
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

        Button(
            onClick = {
                onUpdateOutcome(
                    editOutcome.takeIf { it.isNotBlank() },
                    editSatisfaction.takeIf { it > 0 }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DesignTokens.CornerMedium)
        ) {
            Text("保存结果", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                Text("编辑", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            TextButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                Text("删除决策", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个决策吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SatisfactionRating(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(44.dp)
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(if (rating == i) 0 else i) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$i 星",
                    tint = if (i <= rating) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SatisfactionDot(satisfaction: Int) {
    val color = when {
        satisfaction >= 4 -> Color(0xFF66BB6A)
        satisfaction >= 3 -> Color(0xFFFFB74D)
        else -> Color(0xFFEF5350)
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .padding(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(0.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDecisionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, context: String, options: String, chosenOption: String, concerns: String, followUpAt: Long?) -> Unit,
    existingDecision: Decision? = null
) {
    var title by remember(existingDecision) { mutableStateOf(existingDecision?.title ?: "") }
    var context by remember(existingDecision) { mutableStateOf(existingDecision?.context ?: "") }
    var options by remember(existingDecision) { mutableStateOf(existingDecision?.options ?: "") }
    var chosenOption by remember(existingDecision) { mutableStateOf(existingDecision?.chosenOption ?: "") }
    var concerns by remember(existingDecision) { mutableStateOf(existingDecision?.concerns ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var followUpMillis by remember(existingDecision) { mutableStateOf(existingDecision?.followUpAt) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingDecision != null) "编辑决策" else "添加决策", fontWeight = FontWeight.Medium) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it },
                    label = { Text("背景") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = options,
                    onValueChange = { options = it },
                    label = { Text("选项（每行一个）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = chosenOption,
                    onValueChange = { chosenOption = it },
                    label = { Text("最终选择") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = concerns,
                    onValueChange = { concerns = it },
                    label = { Text("顾虑") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = if (followUpMillis != null) dateFormat.format(Date(followUpMillis!!)) else "",
                    onValueChange = {},
                    label = { Text("回顾日期") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "选择日期")
                        }
                    }
                )
                if (followUpMillis != null) {
                    TextButton(onClick = {
                        followUpMillis = null
                        showDatePicker = false
                    }) {
                        Text("清除日期", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title, context, options, chosenOption, concerns, followUpMillis)
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (existingDecision != null) "保存" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = followUpMillis ?: System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { followUpMillis = it }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AiInsightSection(
    insight: String?,
    isAnalyzing: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAnalyze: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(
                        text = "AI 决策洞察",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = DesignTokens.SpacingMd)) {
                    if (insight != null) {
                        Text(
                            text = insight,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 21.sp
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    }
                    Button(
                        onClick = onAnalyze,
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.CornerMedium)
                    ) {
                        if (isAnalyzing) {
                            Text("分析中...", fontSize = 14.sp)
                        } else {
                            Text("分析决策模式", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
