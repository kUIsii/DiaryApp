package com.diary.app.ui.pastself

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PastSelfScreen(
    viewModel: PastSelfViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val observations by viewModel.observations.collectAsState()
    val debateMode by viewModel.debateMode.collectAsState()
    val letters by viewModel.letters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val growthTopics by viewModel.growthTopics.collectAsState()
    val growthLoading by viewModel.growthLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(selectedPeriod == null) }
    var showDebateDialog by remember { mutableStateOf(false) }
    var showGrowthDialog by remember { mutableStateOf(false) }
    var showLetterMenu by remember { mutableStateOf(false) }
    var editingLetterId by remember { mutableStateOf<String?>(null) }
    var editingLetterContent by remember { mutableStateOf("") }

    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    // debate state
    var debateYear1 by remember { mutableStateOf(year) }
    var debateMonth1 by remember { mutableStateOf(month) }
    var debateYear2 by remember { mutableStateOf(year) }
    var debateMonth2 by remember { mutableStateOf(month) }
    var debateTopic by remember { mutableStateOf("") }

    // growth state
    var growthTopicInput by remember { mutableStateOf("") }

    LaunchedEffect(observations.size) {
        if (observations.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(observations.lastIndex) }
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                cornerRadius = DesignTokens.CornerLarge,
                innerPadding = DesignTokens.SpacingMd
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DesignTokens.IconMedium)
                        )
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "与过去的自己对话",
                            fontSize = DesignTokens.FontTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedPeriod != null) {
                            val fmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
                            Text(
                                text = "${fmt.format(Date(selectedPeriod!!.first))} - ${fmt.format(Date(selectedPeriod!!.second))}",
                                fontSize = DesignTokens.FontSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (selectedPeriod != null) {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "选择时间",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DesignTokens.IconMedium)
                            )
                        }
                    }
                }

                if (debateMode) {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    Text(
                        text = "跨时空辩论模式已开启",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showDatePicker) {
                DateRangePickerCard(
                    currentYear = year,
                    currentMonth = month,
                    debateMode = debateMode,
                    debateYear1 = debateYear1, debateMonth1 = debateMonth1,
                    debateYear2 = debateYear2, debateMonth2 = debateMonth2,
                    debateTopic = debateTopic,
                    onYearChange = { year = it },
                    onMonthChange = { month = it },
                    onDebateYear1Change = { debateYear1 = it },
                    onDebateMonth1Change = { debateMonth1 = it },
                    onDebateYear2Change = { debateYear2 = it },
                    onDebateMonth2Change = { debateMonth2 = it },
                    onDebateTopicChange = { debateTopic = it },
                    onConfirm = {
                        if (debateMode) {
                            val cal1 = Calendar.getInstance().apply { set(debateYear1, debateMonth1 - 1, 1, 0, 0, 0) }
                            val cal2 = Calendar.getInstance().apply { set(debateYear2, debateMonth2 - 1, 1, 0, 0, 0) }
                            val end1 = Calendar.getInstance().apply {
                                set(debateYear1, debateMonth1 - 1, 1, 23, 59, 59)
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            val end2 = Calendar.getInstance().apply {
                                set(debateYear2, debateMonth2 - 1, 1, 23, 59, 59)
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            viewModel.startDebate(
                                cal1.timeInMillis to end1.timeInMillis,
                                cal2.timeInMillis to end2.timeInMillis,
                                debateTopic
                            )
                            showDatePicker = false
                        } else {
                            val cal = Calendar.getInstance().apply { set(year, month - 1, 1, 0, 0, 0) }
                            val end = Calendar.getInstance().apply {
                                set(year, month - 1, 1, 23, 59, 59)
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            viewModel.selectPeriod(cal.timeInMillis, end.timeInMillis)
                            showDatePicker = false
                        }
                    },
                    onDismiss = { showDatePicker = false },
                    onToggleDebate = { viewModel.toggleDebateMode() }
                )
            }

            if (showDebateDialog) {
                DebateConfigDialog(
                    year1 = debateYear1, month1 = debateMonth1,
                    year2 = debateYear2, month2 = debateMonth2,
                    topic = debateTopic,
                    onYear1Change = { debateYear1 = it },
                    onMonth1Change = { debateMonth1 = it },
                    onYear2Change = { debateYear2 = it },
                    onMonth2Change = { debateMonth2 = it },
                    onTopicChange = { debateTopic = it },
                    onStart = {
                        val cal1 = Calendar.getInstance().apply { set(debateYear1, debateMonth1 - 1, 1, 0, 0, 0) }
                        val cal2 = Calendar.getInstance().apply { set(debateYear2, debateMonth2 - 1, 1, 0, 0, 0) }
                        val end1 = Calendar.getInstance().apply {
                            set(debateYear1, debateMonth1 - 1, 1, 23, 59, 59)
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        }
                        val end2 = Calendar.getInstance().apply {
                            set(debateYear2, debateMonth2 - 1, 1, 23, 59, 59)
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        }
                        viewModel.startDebate(
                            cal1.timeInMillis to end1.timeInMillis,
                            cal2.timeInMillis to end2.timeInMillis,
                            debateTopic
                        )
                        showDebateDialog = false
                    },
                    onDismiss = { showDebateDialog = false }
                )
            }

            if (showGrowthDialog) {
                GrowthTopicDialog(
                    topic = growthTopicInput,
                    onTopicChange = { growthTopicInput = it },
                    onAnalyze = {
                        viewModel.analyzeGrowth(growthTopicInput)
                        growthTopicInput = ""
                        showGrowthDialog = false
                    },
                    onDismiss = { showGrowthDialog = false }
                )
            }

            if (showLetterMenu) {
                LetterMenuCard(
                    onGeneratePastToPresent = {
                        viewModel.generateLetter("past_to_present")
                        showLetterMenu = false
                    },
                    onGeneratePresentToFuture = {
                        viewModel.generateLetter("present_to_future")
                        showLetterMenu = false
                    },
                    onDismiss = { showLetterMenu = false }
                )
            }

            if (editingLetterId != null) {
                LetterEditCard(
                    currentContent = editingLetterContent,
                    onSave = { newContent ->
                        viewModel.editLetter(editingLetterId!!, newContent)
                        editingLetterId = null
                        editingLetterContent = ""
                    },
                    onDismiss = {
                        editingLetterId = null
                        editingLetterContent = ""
                    }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingMd),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                if (selectedPeriod == null) {
                    item {
                        EmptyPrompt()
                    }
                }

                // Action buttons
                if (selectedPeriod != null) {
                    item {
                        ActionButtonRow(
                            onDebate = { showDebateDialog = true },
                            onLetter = { showLetterMenu = true },
                            onGrowth = { showGrowthDialog = true },
                            debateMode = debateMode
                        )
                    }
                }

                // Observations
                items(observations) { obs ->
                    ObservationCard(observation = obs)
                }

                // Letters
                items(letters) { letter ->
                    LetterCard(
                        letter = letter,
                        onEdit = {
                            editingLetterId = letter.id
                            editingLetterContent = letter.content
                        }
                    )
                }

                // Growth topics
                items(growthTopics) { topic ->
                    GrowthTopicCard(topic = topic)
                }

                if (isLoading) {
                    item {
                        SkeletonLoading()
                    }
                }

                if (growthLoading) {
                    item {
                        SkeletonLoading()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                }
            }

            if (selectedPeriod != null) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                    cornerRadius = DesignTokens.CornerLarge,
                    innerPadding = DesignTokens.SpacingMd
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (debateMode) "加入辩论..." else "回应观察...",
                                    fontSize = DesignTokens.FontBody,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(DesignTokens.CornerLarge),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = DesignTokens.FontBody),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendResponse(inputText.trim())
                                        inputText = ""
                                    }
                                }
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendResponse(inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isLoading) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (inputText.isNotBlank() && !isLoading) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(DesignTokens.IconMedium)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPrompt() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        Text(
            text = "与过去的自己对话",
            fontSize = DesignTokens.FontTitle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Text(
            text = "选择一个时间段，AI会阅读你的日记\n发起一场有深度的对话",
            fontSize = DesignTokens.FontBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateRangePickerCard(
    currentYear: Int,
    currentMonth: Int,
    debateMode: Boolean,
    debateYear1: Int,
    debateMonth1: Int,
    debateYear2: Int,
    debateMonth2: Int,
    debateTopic: String,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDebateYear1Change: (Int) -> Unit,
    onDebateMonth1Change: (Int) -> Unit,
    onDebateYear2Change: (Int) -> Unit,
    onDebateMonth2Change: (Int) -> Unit,
    onDebateTopicChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onToggleDebate: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择时间段",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(DesignTokens.IconMedium))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = if (debateMode) "跨时空辩论：选择两个时间段" else "选择一个月份查看",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            if (!debateMode) {
                MonthYearSelector(
                    label = "选择时间",
                    year = currentYear,
                    month = currentMonth,
                    onYearChange = onYearChange,
                    onMonthChange = onMonthChange
                )
            } else {
                MonthYearSelector(
                    label = "时间段 1",
                    year = debateYear1,
                    month = debateMonth1,
                    onYearChange = onDebateYear1Change,
                    onMonthChange = onDebateMonth1Change
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                MonthYearSelector(
                    label = "时间段 2",
                    year = debateYear2,
                    month = debateMonth2,
                    onYearChange = onDebateYear2Change,
                    onMonthChange = onDebateMonth2Change
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                OutlinedTextField(
                    value = debateTopic,
                    onValueChange = onDebateTopicChange,
                    label = { Text("辩论话题") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignTokens.CornerMedium),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = DesignTokens.FontBody)
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleDebate() },
                    shape = RoundedCornerShape(DesignTokens.CornerSmall),
                    color = if (debateMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.SpacingSm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSmall),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (debateMode) "普通模式" else "辩论模式",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onConfirm() },
                    shape = RoundedCornerShape(DesignTokens.CornerSmall),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.padding(DesignTokens.SpacingSm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "确认",
                            fontSize = DesignTokens.FontSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthYearSelector(
    label: String,
    year: Int,
    month: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onYearChange(year - 1) },
                shape = RoundedCornerShape(DesignTokens.CornerSmall),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = DesignTokens.SpacingSm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$year 年",
                        fontSize = DesignTokens.FontBody,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMonthChange(if (month == 1) 12 else month - 1) },
                shape = RoundedCornerShape(DesignTokens.CornerSmall),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = DesignTokens.SpacingSm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$month 月",
                        fontSize = DesignTokens.FontBody,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun DebateConfigDialog(
    year1: Int,
    month1: Int,
    year2: Int,
    month2: Int,
    topic: String,
    onYear1Change: (Int) -> Unit,
    onMonth1Change: (Int) -> Unit,
    onYear2Change: (Int) -> Unit,
    onMonth2Change: (Int) -> Unit,
    onTopicChange: (String) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "跨时空辩论",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(DesignTokens.IconMedium))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = "AI会基于两个时期日记中的观点，构建角色进行辩论",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            MonthYearSelector("时期 1", year1, month1, onYear1Change, onMonth1Change)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            MonthYearSelector("时期 2", year2, month2, onYear2Change, onMonth2Change)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                label = { Text("辩论话题") },
                placeholder = { Text("比如：工作、感情、人生选择...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = DesignTokens.FontBody)
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = topic.isNotBlank()) { onStart() },
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                color = if (topic.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.padding(DesignTokens.SpacingMd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "开始辩论",
                        fontSize = DesignTokens.FontBody,
                        color = if (topic.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthTopicDialog(
    topic: String,
    onTopicChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "主题成长追踪",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(DesignTokens.IconMedium))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = "输入一个主题，查看它在不同时间的提及情况",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                label = { Text("主题") },
                placeholder = { Text("比如：健身、读书、换工作...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = DesignTokens.FontBody)
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = topic.isNotBlank()) { onAnalyze() },
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                color = if (topic.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.padding(DesignTokens.SpacingMd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "分析",
                        fontSize = DesignTokens.FontBody,
                        color = if (topic.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    onDebate: () -> Unit,
    onLetter: () -> Unit,
    onGrowth: () -> Unit,
    debateMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
    ) {
        ActionButton(
            text = if (debateMode) "辩论中" else "跨时空辩论",
            icon = Icons.Default.Forum,
            onClick = onDebate,
            enabled = !debateMode,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            text = "写信",
            icon = Icons.Default.MailOutline,
            onClick = onLetter,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            text = "成长追踪",
            icon = Icons.Default.Timeline,
            onClick = onGrowth,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .then(
                if (enabled) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(DesignTokens.CornerSmall),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingSm, vertical = DesignTokens.SpacingSm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSmall),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = DesignTokens.FontCaption,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ObservationCard(observation: AIObservation) {
    val typeLabel = when (observation.type) {
        "debate_point" -> "辩论观点"
        "inference" -> "推断"
        "growth" -> "成长观察"
        else -> "观察"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = typeLabel,
                    fontSize = DesignTokens.FontCaption,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = observation.content,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (observation.sourceEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(
                    text = "引用条目: ${observation.sourceEntries.joinToString(", ") { "#$it" }}",
                    fontSize = DesignTokens.FontCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LetterCard(
    letter: TimeLetter,
    onEdit: () -> Unit
) {
    val directionLabel = if (letter.direction == "past_to_present") "过去的信" else "给未来的信"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MailOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = directionLabel,
                        fontSize = DesignTokens.FontCaption,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = letter.content,
                fontSize = DesignTokens.FontBody,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (letter.userEdits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = "已编辑 ${letter.userEdits.size} 次",
                    fontSize = DesignTokens.FontCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LetterMenuCard(
    onGeneratePastToPresent: () -> Unit,
    onGeneratePresentToFuture: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "写信",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(DesignTokens.IconMedium))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGeneratePastToPresent() },
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(DesignTokens.SpacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconLarge)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                    Column {
                        Text(
                            text = "来自过去的信",
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI基于日记内容和写作风格生成",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGeneratePresentToFuture() },
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(DesignTokens.SpacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(DesignTokens.IconLarge)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                    Column {
                        Text(
                            text = "给未来的信",
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "写给未来的自己，记录此刻的想法",
                            fontSize = DesignTokens.FontSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterEditCard(
    currentContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember { mutableStateOf(currentContent) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SpacingMd),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑信",
                    fontSize = DesignTokens.FontMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row {
                    Surface(
                        modifier = Modifier
                            .clickable { onSave(editedText) },
                        shape = RoundedCornerShape(DesignTokens.CornerSmall),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "保存",
                            modifier = Modifier.padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                            fontSize = DesignTokens.FontSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "取消", modifier = Modifier.size(DesignTokens.IconMedium))
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(DesignTokens.CornerMedium),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = DesignTokens.FontBody),
                maxLines = 15
            )
        }
    }
}

@Composable
private fun GrowthTopicCard(topic: GrowthTopic) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = topic.topic,
                    fontSize = DesignTokens.FontBody,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            // Growth points as simple horizontal bars
            topic.points.forEach { point ->
                val fmt = SimpleDateFormat("M月d日", Locale.CHINA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fmt.format(Date(point.date)),
                        fontSize = DesignTokens.FontCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(50.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(point.intensity)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                    Text(
                        text = point.label.take(20),
                        fontSize = DesignTokens.FontCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }

            if (topic.points.isEmpty()) {
                Text(
                    text = "未在日记中找到相关提及",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (topic.trend.isNotBlank()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(
                    text = topic.trend,
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SkeletonLoading() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingMd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
            Text(
                text = "思考中...",
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
