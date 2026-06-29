package com.diary.app.ui.writinglab

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WritingLabScreen(onNavigateBack: () -> Unit, viewModel: WritingLabViewModel = viewModel()) {
    val activeExperiment by viewModel.activeExperiment.collectAsState()
    val participations by viewModel.participations.collectAsState()
    val completedExperiments by viewModel.completedExperiments.collectAsState()
    val showPicker by viewModel.showPresetPicker.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    var inputText by remember { mutableStateOf("") }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "写作工坊", onNavigateBack = onNavigateBack)

            TabRow(currentTab = currentTab, onTabSelected = { viewModel.setTab(it) })

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DesignTokens.SpacingLg),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
            ) {
                when (currentTab) {
                    WritingLabTab.EXPERIMENTS -> ExperimentsSection(
                        showPicker = showPicker,
                        activeExperiment = activeExperiment,
                        participations = participations,
                        completedExperiments = completedExperiments,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onStartExperiment = { viewModel.startExperiment(it) },
                        onLogParticipation = { text -> viewModel.logParticipation(text.trim()); inputText = "" }
                    )
                    WritingLabTab.STYLE_TRANSFER -> StyleTransferSection(viewModel)
                    WritingLabTab.CHALLENGES -> ChallengeSection(viewModel)
                    WritingLabTab.RHETORICAL -> RhetoricalSection(viewModel)
                    WritingLabTab.TEMPLATES -> TemplateSection(viewModel)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TabRow(currentTab: WritingLabTab, onTabSelected: (WritingLabTab) -> Unit) {
    val tabs = listOf(
        WritingLabTab.EXPERIMENTS to "写作实验",
        WritingLabTab.STYLE_TRANSFER to "风格转换",
        WritingLabTab.CHALLENGES to "写作挑战",
        WritingLabTab.RHETORICAL to "修辞建议",
        WritingLabTab.TEMPLATES to "创意模板"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingLg, vertical = DesignTokens.SpacingSm),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXs)
    ) {
        tabs.forEach { (tab, label) ->
            val selected = currentTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = DesignTokens.FontSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ExperimentsSection(
    showPicker: Boolean,
    activeExperiment: com.diary.app.data.WritingExperiment?,
    participations: List<com.diary.app.data.ExperimentParticipation>,
    completedExperiments: List<com.diary.app.data.WritingExperiment>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onStartExperiment: (ExperimentPreset) -> Unit,
    onLogParticipation: (String) -> Unit
) {
    if (showPicker) {
        Text("选择一个实验开始", fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        experimentPresets.forEach { preset ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(DesignTokens.SpacingLg)) {
                    Text(preset.title, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(preset.description, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${preset.days}天 · ${preset.rules}", fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { onStartExperiment(preset) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignTokens.CornerMedium)) { Text("开始实验") }
                }
            }
        }
    }

    activeExperiment?.let { exp ->
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                Text("本周实验", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Text(exp.title, fontSize = DesignTokens.FontTitle, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(exp.description, fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(exp.rules, fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            val totalDays = experimentPresets.find { it.title == exp.title }?.days ?: 7
            val progress = participations.size.toFloat() / totalDays.toFloat()

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingSm),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2), size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f, sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2), size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("已完成 ${participations.size}/${totalDays} 天", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                Text("${(progress * 100).toInt()}%", fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            val hasWrittenToday = participations.any { p ->
                val cal = Calendar.getInstance()
                val partCal = Calendar.getInstance().apply { timeInMillis = p.completedAt }
                cal.get(Calendar.DAY_OF_YEAR) == partCal.get(Calendar.DAY_OF_YEAR) &&
                cal.get(Calendar.YEAR) == partCal.get(Calendar.YEAR)
            }
            if (!hasWrittenToday && participations.size < totalDays) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingSm), contentAlignment = Alignment.Center) {
                    Text("今天还没有写作，记得来完成今日记录",
                        fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (participations.size < totalDays) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                OutlinedTextField(value = inputText, onValueChange = onInputChange,
                    placeholder = { Text("写下今天的实验记录...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(DesignTokens.CornerMedium))
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Button(onClick = { if (inputText.isNotBlank()) onLogParticipation(inputText) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(DesignTokens.CornerMedium)) { Text("提交第${participations.size + 1}天记录") }
            } else {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                val totalChars = participations.sumOf { it.note.length }
                val firstDate = remember(participations) {
                    SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(participations.firstOrNull()?.completedAt ?: exp.startDate))
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerXLarge,
                    innerPadding = DesignTokens.SpacingXl
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            modifier = Modifier.size(48.dp), tint = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text("恭喜完成实验！", fontSize = DesignTokens.FontLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("获得「${exp.badgeName}」徽章", fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Divider()
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                        Text("实验统计", fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("写作天数：${participations.size} 天", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("总字数：${totalChars} 字", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("开始日期：$firstDate", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (completedExperiments.isNotEmpty()) {
        Text("过往实验", fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        completedExperiments.forEach { exp ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exp.title, fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium)
                        Text(exp.description, fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("已完成", fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StyleTransferSection(viewModel: WritingLabViewModel) {
    val aiInputText by viewModel.aiInputText.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val styleResult by viewModel.styleResult.collectAsState()
    val isStyleLoading by viewModel.isStyleLoading.collectAsState()
    val currentRating by viewModel.currentRating.collectAsState()
    val styleHistory by viewModel.styleHistory.collectAsState()

    Text("AI 风格转换", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Text("输入一段文字，选择目标风格，AI将为您重写", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    OutlinedTextField(value = aiInputText, onValueChange = { viewModel.setAiInputText(it) },
        placeholder = { Text("输入要转换的文字...") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(DesignTokens.CornerMedium))

    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
    Text("选择风格", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
    ) {
        styleTransferOptions.forEach { style ->
            val isSelected = style == selectedStyle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { viewModel.setSelectedStyle(style) }
                    .padding(vertical = DesignTokens.SpacingSm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.take(2),
                    fontSize = DesignTokens.FontSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Button(
        onClick = { viewModel.performStyleTransfer() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        enabled = aiInputText.isNotBlank() && !isStyleLoading
    ) {
        if (isStyleLoading) Text("转换中...")
        else Text("开始转换")
    }

    styleResult?.let { result ->
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingMd) {
            Text("原文", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(aiInputText, fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Divider()
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Text(selectedStyle, fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(result, fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onBackground)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Text("这个改写对你有帮助吗？", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                listOf("有用" to 1, "一般" to 0, "不适用" to -1).forEach { (label, value) ->
                    val isActive = currentRating == value
                    Button(
                        onClick = { viewModel.setRating(if (isActive) null else value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignTokens.CornerMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = DesignTokens.FontSmall,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            OutlinedButton(
                onClick = { viewModel.clearStyleResult() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerMedium)
            ) { Text("重新输入") }
        }
    }

    if (styleHistory.isNotEmpty()) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        Text("转换记录", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
        styleHistory.take(3).forEachIndexed { index, record ->
            GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingMd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.metadata["style"] ?: "", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
                        Text(record.originalText?.take(50) ?: "", fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    record.rating?.let { r ->
                        Text(
                            when (r) { 1 -> "有用"; 0 -> "一般"; else -> "不适用" },
                            fontSize = DesignTokens.FontCaption,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeSection(viewModel: WritingLabViewModel) {
    val currentChallenge by viewModel.currentChallenge.collectAsState()
    val challengeStreak by viewModel.challengeStreak.collectAsState()
    val isChallengeLoading by viewModel.isChallengeLoading.collectAsState()
    val completedChallenges by viewModel.completedChallenges.collectAsState()

    Text("AI 写作挑战", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Text("基于您的写作习惯，生成个性化挑战", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingMd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("连续完成", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
            Text("$challengeStreak", fontSize = DesignTokens.FontTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(" 天", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(DesignTokens.SpacingXxl))
            Text("已完成", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
            Text("$completedChallenges", fontSize = DesignTokens.FontTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(" 个", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (currentChallenge != null) {
        val challenge = currentChallenge ?: return
        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingLg) {
            Text("今日挑战", fontSize = DesignTokens.FontBody, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(challenge.text, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text("原因: ${challenge.reason}", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                Button(
                    onClick = { viewModel.completeChallenge() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.CornerMedium)
                ) { Text("完成挑战") }
                OutlinedButton(
                    onClick = { viewModel.skipChallenge() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.CornerMedium)
                ) { Text("跳过") }
            }
        }
    } else {
        if (!isChallengeLoading) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Button(
                onClick = { viewModel.generateChallenge() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerMedium)
            ) { Text("生成新挑战") }
        }
    }

    if (isChallengeLoading) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Text("正在根据您的写作习惯生成挑战...", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RhetoricalSection(viewModel: WritingLabViewModel) {
    val rhetoricalInput by viewModel.rhetoricalInput.collectAsState()
    val rhetoricalSuggestions by viewModel.rhetoricalSuggestions.collectAsState()
    val isRhetoricalLoading by viewModel.isRhetoricalLoading.collectAsState()
    val showRhetoricalDots by viewModel.showRhetoricalDots.collectAsState()
    val expandedSuggestionIndex by viewModel.expandedSuggestionIndex.collectAsState()

    Text("AI 修辞建议", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Text("输入段落，AI 将分析并给出改进建议", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    OutlinedTextField(value = rhetoricalInput, onValueChange = { viewModel.setRhetoricalInput(it) },
        placeholder = { Text("输入要分析的段落...") },
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(DesignTokens.CornerMedium))

    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
    Button(
        onClick = { viewModel.analyzeRhetorical() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerMedium),
        enabled = rhetoricalInput.isNotBlank() && !isRhetoricalLoading
    ) {
        if (isRhetoricalLoading) Text("分析中...")
        else Text("开始分析")
    }

    if (isRhetoricalLoading) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    if (showRhetoricalDots) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        Text("检测到可改进之处", fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        rhetoricalSuggestions.forEachIndexed { index, suggestion ->
            val isExpanded = expandedSuggestionIndex == index
            val typeColor = when (suggestion.type) {
                "修辞建议" -> MaterialTheme.colorScheme.primary
                "结构建议" -> Color(0xFF4CAF50)
                "词汇建议" -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.CornerSmall))
                    .background(typeColor.copy(alpha = 0.08f))
                    .clickable {
                        viewModel.setExpandedSuggestionIndex(if (isExpanded) null else index)
                    }
                    .padding(DesignTokens.SpacingMd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(typeColor)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(suggestion.type, fontSize = DesignTokens.FontSmall, fontWeight = FontWeight.Medium, color = typeColor)
                    Spacer(modifier = Modifier.weight(1f))
                    if (suggestion.isApplied) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(suggestion.text, fontSize = DesignTokens.FontBody)
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text("建议: ${suggestion.suggestion}", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                        Button(
                            onClick = { viewModel.applySuggestion(index) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(DesignTokens.CornerSmall),
                            enabled = !suggestion.isApplied
                        ) { Text("采纳", fontSize = DesignTokens.FontSmall) }
                        OutlinedButton(
                            onClick = { viewModel.dismissSuggestion(index) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(DesignTokens.CornerSmall)
                        ) { Text("忽略", fontSize = DesignTokens.FontSmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateSection(viewModel: WritingLabViewModel) {
    val templates by viewModel.templates.collectAsState()
    val isTemplateLoading by viewModel.isTemplateLoading.collectAsState()

    Text("创意写作模板", fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Text("AI 根据您的写作习惯生成的模板", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (templates.isEmpty()) {
        Button(
            onClick = { viewModel.generateTemplates() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DesignTokens.CornerMedium),
            enabled = !isTemplateLoading
        ) {
            if (isTemplateLoading) Text("生成中...")
            else Text("生成模板")
        }
    }

    if (isTemplateLoading) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Text("正在分析您的写作风格并生成模板...", fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    templates.forEach { template ->
        var showExample by remember { mutableStateOf(false) }
        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = DesignTokens.SpacingMd) {
            Text(template.title, fontSize = DesignTokens.FontMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(template.type, fontSize = DesignTokens.FontCaption, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(template.description, fontSize = DesignTokens.FontBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Button(
                onClick = { showExample = !showExample },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    if (showExample) "收起示例" else "查看示例",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showExample) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = DesignTokens.CornerSmall,
                    innerPadding = DesignTokens.SpacingSm
                ) {
                    Text(template.example, fontSize = DesignTokens.FontSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
