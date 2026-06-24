package com.diary.app.ui.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.diary.app.data.PetHiddenStateType
import com.diary.app.data.PetState
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.CrossSystemManager
import com.diary.app.data.PetGrowthStage
import com.diary.app.data.PetPersonalityAnalyzer
import com.diary.app.data.PetStateMachine
import com.diary.app.data.TitleDefinition
import com.diary.app.data.ActiveCombination
import com.diary.app.data.CombinationEffect
import com.diary.app.ui.title.TitleViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    onBack: () -> Unit,
    viewModel: PetViewModel = viewModel(),
    titleViewModel: TitleViewModel = viewModel()
) {
    val petProfile by viewModel.petProfile.collectAsState()
    val personality by viewModel.personality.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val feedbackText by viewModel.feedbackText.collectAsState()
    val lastTrigger by viewModel.lastTrigger.collectAsState()
    val interactionType by viewModel.interactionType.collectAsState()
    val interactionCounter by viewModel.interactionCounter.collectAsState()
    val moodHistory by viewModel.moodHistory.collectAsState()
    val moodDistribution by viewModel.moodDistribution.collectAsState()
    val memoryTrigger by viewModel.memoryTrigger.collectAsState()
    val growthStage by viewModel.growthStage.collectAsState()
    val evolutionHint by viewModel.evolutionHint.collectAsState()
    val activeHiddenState by viewModel.activeHiddenState.collectAsState()
    val discoveredHiddenCount by viewModel.discoveredHiddenCount.collectAsState()
    val activeEffects by viewModel.activeEffects.collectAsState()
    val activeCombinations by viewModel.activeCombinations.collectAsState()
    val combinationNotification by viewModel.combinationNotification.collectAsState()

    // 小岛等级（影响宠物外观装饰）
    val islandLevel by CrossSystemManager.islandLevel.collectAsState()

    // 当前称号数据
    val titleProfile by titleViewModel.titleProfile.collectAsState()
    val allDefinitions by titleViewModel.allDefinitions.collectAsState()
    val activeTitle = remember(titleProfile?.activeTitleKey, allDefinitions) {
        titleProfile?.activeTitleKey?.let { key ->
            allDefinitions.find { it.key == key }
        }
    }

    var isEditingName by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.getDailyGreeting()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("情绪宠物", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = petProfile?.name ?: "小记",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 宠物展示区（带手势交互）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.onPetTapped() },
                            onLongPress = { viewModel.onPetLongPressed() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val (dx, dy) = dragAmount
                            when {
                                dy > 20f -> viewModel.onPetFed()
                                kotlin.math.abs(dx) > kotlin.math.abs(dy) && kotlin.math.abs(dx) > 10f -> viewModel.onPetGroomed()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                PetComposable(
                    state = currentState,
                    modifier = Modifier.size(200.dp),
                    interactionType = interactionType,
                    interactionCounter = interactionCounter,
                    appearanceLevel = islandLevel,
                    growthStage = growthStage,
                    hiddenState = activeHiddenState,
                    activeEffects = activeEffects
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 宠物名称
            if (isEditingName) {
                TextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updatePetName(editingName)
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "保存")
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editingName = petProfile?.name ?: "小记"
                        isEditingName = true
                    }
                ) {
                    Text(
                        text = petProfile?.name ?: "小记",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑名称",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 当前称号标签
            activeTitle?.let { title ->
                Spacer(modifier = Modifier.height(8.dp))
                ActiveTitleBadge(title = title)
            }

            // 成长阶段标签
            Spacer(modifier = Modifier.height(8.dp))
            GrowthStageBadge(
                stage = growthStage,
                evolvedAt = petProfile?.evolvedAt
            )

            // 进化提示
            evolutionHint?.let { hint ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 隐藏状态发现进度
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "已发现 $discoveredHiddenCount/5 种形态",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // 称号组合信息
            if (activeCombinations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                CombinationSection(combinations = activeCombinations)
            }

            // 组合通知
            AnimatedVisibility(
                visible = combinationNotification != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                combinationNotification?.let { notification ->
                    LaunchedEffect(notification) {
                        delay(3000)
                        viewModel.clearCombinationNotification()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = notification,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 状态和反馈
            AnimatedVisibility(
                visible = feedbackText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = feedbackText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 记忆触发文案
            AnimatedVisibility(
                visible = memoryTrigger != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                memoryTrigger?.let { trigger ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "记忆",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trigger,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 宠物信息卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 好感度
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    title = "好感度",
                    value = "${petProfile?.affection ?: 0}",
                    color = MaterialTheme.colorScheme.primary
                )

                // 连续记录
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Info,
                    title = "连续记录",
                    value = "${petProfile?.streakDays ?: 0}天",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 性格信息
            personality?.let { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "宠物性格",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = com.diary.app.data.PetPersonalityAnalyzer.getDominantTrait(p),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PersonalityBar("外向性", p.extraversion)
                        PersonalityBar("开放性", p.openness)
                        PersonalityBar("尽责性", p.conscientiousness)
                        PersonalityBar("宜人性", p.agreeableness)
                        PersonalityBar("情绪稳定", p.emotionalStability)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            MoodHistorySection(moodHistory, moodDistribution, currentState)
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PersonalityBar(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = value)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ActiveTitleBadge(title: TitleDefinition) {
    val tierColor = when (title.tier) {
        3 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val tierLabel = when (title.tier) {
        3 -> "传说"
        2 -> "稀有"
        else -> "普通"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 稀有度颜色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tierColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tierColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tierLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun GrowthStageBadge(
    stage: PetGrowthStage,
    evolvedAt: Long?
) {
    val stageColor = when (stage) {
        PetGrowthStage.JUVENILE -> Color(0xFF7BC9A0)  // 幼年期: 绿色
        PetGrowthStage.GROWING -> Color(0xFF7BA7C9)   // 成长期: 蓝色
        PetGrowthStage.MATURE -> Color(0xFFA88BC9)    // 成熟期: 紫色
    }

    val daysInStage = PetStateMachine.getDaysInStage(evolvedAt, stage)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(stageColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 阶段颜色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(stageColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stage.displayName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = stageColor
        )
        if (daysInStage > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${daysInStage}天",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CombinationSection(
    combinations: List<ActiveCombination>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFD700).copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(
            text = "称号组合",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB8860B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        combinations.forEach { activeCombination ->
            val effectColor = when (activeCombination.combination.effectType) {
                CombinationEffect.WISDOM_AURA -> Color(0xFFFFEB3B)
                CombinationEffect.WARM_GLOW -> Color(0xFFFFC107)
                CombinationEffect.ADVENTURE_BADGE -> Color(0xFFFFD700)
                CombinationEffect.PERSISTENCE_AURA -> Color(0xFFFFD700)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(effectColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeCombination.combination.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB8860B)
                )
            }
        }
    }
}

// ==================== 心情历史图表 ====================

private fun stateColor(state: PetState): Color = when (state) {
    PetState.CALM -> Color(0xFF7BA7C9)
    PetState.HAPPY -> Color(0xFF7BC9A0)
    PetState.SLEEPY -> Color(0xFF8899AA)
    PetState.WORRIED -> Color(0xFFD99AB8)
    PetState.SAD -> Color(0xFF818CF8)
    PetState.EXCITED -> Color(0xFFA88BC9)
    PetState.CURIOUS -> Color(0xFFD4A06A)
    PetState.TIRED -> Color(0xFF9088A8)
}

private fun moodRank(state: PetState): Int = when (state) {
    PetState.SAD -> 0
    PetState.WORRIED -> 1
    PetState.TIRED -> 2
    PetState.SLEEPY -> 3
    PetState.CALM -> 4
    PetState.CURIOUS -> 5
    PetState.HAPPY -> 6
    PetState.EXCITED -> 7
}

private fun moodGradientColor(rank: Int): Color {
    val fraction = rank.toFloat() / 7f
    return lerp(Color(0xFF6BAED6), Color(0xFF74C476), fraction)
}

private data class SectorData(
    val state: PetState,
    val startAngle: Float,
    val sweepAngle: Float,
    val count: Int
)

@Composable
private fun MoodHistorySection(
    moodHistory: List<MoodDayData>,
    moodDistribution: List<MoodDistributionItem>,
    currentState: PetState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "心情趋势",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoodTrendChart(moodHistory)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "心情分布",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoodDistributionChart(moodDistribution, currentState)
        }
    }
}

@Composable
private fun MoodTrendChart(
    moodHistory: List<MoodDayData>,
    modifier: Modifier = Modifier
) {
    val hasData = moodHistory.any { it.state != null }

    if (!hasData) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "开始记录后这里会显示心情趋势~",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            val leftPad = 8.dp.toPx()
            val rightPad = 8.dp.toPx()
            val topPad = 16.dp.toPx()
            val bottomPad = 8.dp.toPx()

            val plotWidth = size.width - leftPad - rightPad
            val plotHeight = size.height - topPad - bottomPad
            val stateCount = 8

            // 水平网格线
            for (i in 0 until stateCount) {
                val y = topPad + plotHeight * (1f - i.toFloat() / (stateCount - 1))
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y))
            }

            // 计算数据点
            data class ChartPoint(val x: Float, val y: Float, val rank: Int)

            val points = moodHistory.mapIndexed { index, day ->
                if (day.state != null) {
                    val x = leftPad + plotWidth * index.toFloat() / (moodHistory.size - 1).coerceAtLeast(1)
                    val rank = moodRank(day.state)
                    val y = topPad + plotHeight * (1f - rank.toFloat() / (stateCount - 1))
                    ChartPoint(x, y, rank)
                } else {
                    null
                }
            }

            // 绘制折线（渐变色）
            for (i in 0 until points.size - 1) {
                val p1 = points[i] ?: continue
                val p2 = points[i + 1] ?: continue
                val midColor = lerp(
                    moodGradientColor(p1.rank),
                    moodGradientColor(p2.rank),
                    0.5f
                )

                drawLine(
                    color = midColor,
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // 绘制数据点
            points.forEachIndexed { index, point ->
                if (point != null) {
                    val isToday = moodHistory[index].isToday
                    val color = moodGradientColor(point.rank)

                    if (isToday) {
                        drawCircle(
                            color = color.copy(alpha = 0.2f),
                            radius = 10.dp.toPx(),
                            center = Offset(point.x, point.y)
                        )
                        drawCircle(
                            color = color,
                            radius = 6.dp.toPx(),
                            center = Offset(point.x, point.y)
                        )
                    } else {
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = Offset(point.x, point.y)
                        )
                    }
                }
            }
        }

        // 日期标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            moodHistory.forEach { day ->
                Text(
                    text = day.dayLabel.takeLast(1),
                    fontSize = 11.sp,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MoodDistributionChart(
    distribution: List<MoodDistributionItem>,
    currentState: PetState,
    modifier: Modifier = Modifier
) {
    if (distribution.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "开始记录后这里会显示心情分布~",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf(-1) }
    val total = distribution.sumOf { it.count }.toFloat()

    val sectors = remember(distribution) {
        var startAngle = -90f
        distribution.map { item ->
            val sweep = (item.count / total) * 360f
            val sector = SectorData(
                state = item.state,
                startAngle = startAngle,
                sweepAngle = sweep,
                count = item.count
            )
            startAngle += sweep
            sector
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .pointerInput(distribution) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)

                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius * 0.55f

                        if (distance in innerRadius..outerRadius) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            val hitIndex = sectors.indexOfFirst { sector ->
                                val end = sector.startAngle + sector.sweepAngle
                                if (end > 360f) {
                                    angle >= sector.startAngle || angle <= end - 360f
                                } else {
                                    angle >= sector.startAngle && angle <= end
                                }
                            }

                            selectedIndex = if (hitIndex >= 0) {
                                if (selectedIndex == hitIndex) -1 else hitIndex
                            } else {
                                -1
                            }
                        }
                    }
                }
        ) {
            val outerRadius = size.width / 2f
            val innerRadius = outerRadius * 0.55f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 绘制扇区
            sectors.forEachIndexed { index, sector ->
                val color = stateColor(sector.state)
                val isSelected = selectedIndex == index
                val drawRadius = if (isSelected) outerRadius + 4.dp.toPx() else outerRadius

                drawArc(
                    color = if (isSelected) color.copy(alpha = 0.9f) else color,
                    startAngle = sector.startAngle,
                    sweepAngle = sector.sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - drawRadius, center.y - drawRadius),
                    size = Size(drawRadius * 2, drawRadius * 2)
                )
            }

            // 中心圆（甜甜圈空心）
            drawCircle(
                color = surfaceColor,
                radius = innerRadius,
                center = center
            )
        }

        // 中心文字（叠加在 Canvas 上方）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selectedIndex >= 0 && selectedIndex < sectors.size) {
                val sector = sectors[selectedIndex]
                Text(
                    text = sector.state.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor(sector.state)
                )
                Text(
                    text = "${sector.count}天",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = currentState.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = stateColor(currentState)
                )
            }
        }
    }
}
