package com.diary.app.ui.title

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.TitleDefinition
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TitleCategory(
    val key: String?,
    val label: String,
    val icon: ImageVector
)

private val categories = listOf(
    TitleCategory(null, "全部", Icons.Default.Star),
    TitleCategory("time", "时间旅人", Icons.Default.DateRange),
    TitleCategory("mood", "情绪画师", Icons.Default.Face),
    TitleCategory("weather", "风雨行者", Icons.Default.Cloud),
    TitleCategory("writing", "文字匠人", Icons.Default.TextFields),
    TitleCategory("habit", "习惯先锋", Icons.Default.Park),
    TitleCategory("hidden", "隐藏彩蛋", Icons.Default.Favorite),
)

/**
 * 根据 iconName 获取对应的 Material Icon
 */
private fun getTitleIcon(iconName: String): ImageVector {
    return when (iconName) {
        "NightsStay" -> Icons.Default.Favorite
        "DarkMode" -> Icons.Default.Cloud
        "LightMode" -> Icons.Default.Star
        "WbSunny" -> Icons.Default.Star
        "DateRange" -> Icons.Default.DateRange
        "CalendarMonth" -> Icons.Default.DateRange
        "SentimentVerySatisfied" -> Icons.Default.Face
        "Psychology" -> Icons.Default.Face
        "Palette" -> Icons.Default.Face
        "TrendingUp" -> Icons.Default.Star
        "Water" -> Icons.Default.Cloud
        "Insights" -> Icons.Default.Star
        "WaterDrop" -> Icons.Default.Cloud
        "AcUnit" -> Icons.Default.Cloud
        "Storm" -> Icons.Default.Cloud
        "CloudDone" -> Icons.Default.Cloud
        "SevereWeather" -> Icons.Default.Cloud
        "TextFields" -> Icons.Default.TextFields
        "ShortText" -> Icons.Default.TextFields
        "Collections" -> Icons.Default.Bookmark
        "Label" -> Icons.Default.Bookmark
        "Bookmark" -> Icons.Default.Bookmark
        "TextSnippet" -> Icons.Default.TextFields
        "Whatshot" -> Icons.Default.Park
        "MilitaryTech" -> Icons.Default.Star
        "Replay" -> Icons.Default.Star
        "FlashOn" -> Icons.Default.Star
        "Timelapse" -> Icons.Default.Star
        "TwoMp" -> Icons.Default.Star
        "Celebration" -> Icons.Default.Favorite
        "NewReleases" -> Icons.Default.Favorite
        "Schedule" -> Icons.Default.DateRange
        "AutoFixHigh" -> Icons.Default.Star
        else -> Icons.Default.Star
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    onBack: () -> Unit,
    viewModel: TitleViewModel = viewModel()
) {
    val allDefinitions by viewModel.allDefinitions.collectAsState()
    val unlockedTitles by viewModel.unlockedTitles.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()
    val recentlyUnlocked by viewModel.recentlyUnlocked.collectAsState()
    val selectedTitleForDetail by viewModel.selectedTitleForDetail.collectAsState()
    val titleProfile by viewModel.titleProfile.collectAsState()
    val unlockTimeMap by viewModel.unlockTimeMap.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.checkAllTitles()
    }

    // 筛选后的称号列表
    val filteredTitles = remember(allDefinitions, selectedCategory) {
        if (selectedCategory == null) allDefinitions
        else allDefinitions.filter { it.category == selectedCategory }
    }

    val unlockedKeys = remember(unlockedTitles) {
        unlockedTitles.map { it.key }.toSet()
    }

    // 标题解锁动画
    recentlyUnlocked?.let { title ->
        TitleUnlockAnimation(
            title = title,
            onDismiss = { viewModel.clearRecentlyUnlocked() }
        )
    }

    // 称号详情弹窗
    selectedTitleForDetail?.let { detailTitle ->
        TitleDetailSheet(
            title = detailTitle,
            isUnlocked = detailTitle.key in unlockedKeys,
            isActive = titleProfile?.activeTitleKey == detailTitle.key,
            unlockTime = unlockTimeMap[detailTitle.key],
            progress = progressMap[detailTitle.key],
            onDismiss = { viewModel.dismissTitleDetail() },
            onSetActive = { viewModel.setActiveTitle(detailTitle.key) },
            onClearActive = { viewModel.setActiveTitle(null) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("趣味称号", fontWeight = FontWeight.Bold)
                    val unlocked = unlockedTitles.size
                    val total = allDefinitions.size
                    if (total > 0) {
                        Text(
                            text = "已解锁 $unlocked / $total",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
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

        // 分类标签
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            categories.forEachIndexed { index, category ->
                val (unlockedCount, totalCount) = viewModel.getCategoryProgress(category.key)
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        viewModel.selectCategory(category.key)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (category.key == null) category.label
                                else "${category.label} $unlockedCount/$totalCount",
                                fontSize = 13.sp
                            )
                        }
                    }
                )
            }
        }

        // 称号列表
        if (filteredTitles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无称号数据",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredTitles, key = { _, item -> item.key }) { index, title ->
                    val isUnlocked = title.key in unlockedKeys
                    val isActive = titleProfile?.activeTitleKey == title.key
                    val progress = progressMap[title.key]

                    // 交错入场动画
                    var appeared by remember { mutableStateOf(false) }
                    val alpha = remember { Animatable(0f) }
                    val translationY = remember { Animatable(30f) }

                    LaunchedEffect(Unit) {
                        delay(index.coerceAtMost(10) * 50L)
                        appeared = true
                        alpha.animateTo(1f, animationSpec = tween(durationMillis = 300))
                        translationY.animateTo(0f, animationSpec = tween(durationMillis = 300))
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                this.alpha = alpha.value
                                this.translationY = translationY.value
                            }
                    ) {
                        TitleCard(
                            title = title,
                            isUnlocked = isUnlocked,
                            isActive = isActive,
                            progress = progress,
                            onClick = { viewModel.showTitleDetail(title) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleDetailSheet(
    title: TitleDefinition,
    isUnlocked: Boolean,
    isActive: Boolean,
    unlockTime: Long?,
    progress: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onSetActive: () -> Unit,
    onClearActive: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val unlockTimeText = remember(unlockTime) {
        if (unlockTime != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(unlockTime))
        } else ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 顶部图标和名称
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 大图标
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) tierColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getTitleIcon(title.iconName),
                        contentDescription = null,
                        tint = if (isUnlocked) tierColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 名称和稀有度
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (title.isHidden && !isUnlocked) "???" else title.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tierColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tierLabel,
                            fontSize = 12.sp,
                            color = tierColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 描述
            Text(
                text = if (title.isHidden && !isUnlocked) "完成特定条件解锁此隐藏称号"
                else title.description,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )

            // 风味文字（已解锁才显示）
            if (isUnlocked && title.flavorText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title.flavorText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isUnlocked) {
                // 已解锁：显示解锁时间和状态
                // 解锁时间
                if (unlockTimeText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "解锁时间",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = unlockTimeText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 使用状态和按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "使用中",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // 取消使用按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onClearActive() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "取消使用",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        // 设为当前称号按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onSetActive() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "设为当前称号",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // 未解锁：显示进度
                if (progress != null) {
                    val (current, target) = progress
                    val progressFraction = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "解锁进度",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$current / $target",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tierColor)
                            )
                        }
                    }
                } else {
                    // 无进度数据时的提示
                    Text(
                        text = "继续使用应用即可解锁此称号",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TitleCard(
    title: TitleDefinition,
    isUnlocked: Boolean,
    isActive: Boolean,
    progress: Pair<Int, Int>?,
    onClick: () -> Unit
) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isUnlocked) {
                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 称号图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) tierColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTitleIcon(title.iconName),
                    contentDescription = null,
                    tint = if (isUnlocked) tierColor
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 称号信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (title.isHidden && !isUnlocked) "???" else title.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tierColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tierLabel,
                            fontSize = 10.sp,
                            color = tierColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (title.isHidden && !isUnlocked) "完成特定条件解锁"
                    else title.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 解锁状态
            if (isUnlocked) {
                if (isActive) {
                    // 使用中标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "使用中",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "已解锁",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 解锁进度条（未解锁时显示）
        if (!isUnlocked && progress != null) {
            Spacer(modifier = Modifier.height(12.dp))
            val (current, target) = progress
            val progressFraction = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "解锁进度",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "$current / $target",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tierColor)
                    )
                }
            }
        }

        // 已解锁的风味文字
        if (isUnlocked && title.flavorText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title.flavorText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
