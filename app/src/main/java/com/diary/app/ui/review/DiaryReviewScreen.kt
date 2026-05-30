package com.diary.app.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.home.ReviewEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryReviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: com.diary.app.ui.home.HomeViewModel = viewModel()
) {
    val reviewEntries by viewModel.reviewEntries.collectAsState()
    val onThisDayEntries by viewModel.onThisDayEntries.collectAsState()

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "日记回顾",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            val allReviewItems = buildList {
                addAll(reviewEntries)
                if (onThisDayEntries.isNotEmpty()) {
                    onThisDayEntries.forEach { add(ReviewEntry("去年今日", it)) }
                }
            }

            if (allReviewItems.isEmpty()) {
                ReviewEmptyState(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    itemsIndexed(allReviewItems, key = { _, item -> item.entry.id }) { index, reviewItem ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + slideInVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { it / 3 }
                        ) {
                            ReviewCard(
                                reviewItem = reviewItem,
                                onClick = { onNavigateToDetail(reviewItem.entry.id) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    reviewItem: ReviewEntry,
    onClick: () -> Unit
) {
    val entry = reviewItem.entry
    val entryDate = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val now = LocalDate.now()
    val yearDiff = now.year - entryDate.year

    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val accentBarColor = entry.moodLevel?.let { moodColorForLevel(it) } ?: primary

    val labelColor = when (reviewItem.label) {
        "去年今日" -> primary
        "一周前" -> MaterialTheme.colorScheme.tertiary
        else -> onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            cornerRadius = 16.dp
        ) {
            Column {
                // Label and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = labelColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reviewItem.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor
                        )
                    }
                    Text(
                        text = formatDate(entryDate, now),
                        fontSize = 12.sp,
                        color = onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text preview
                if (entry.plainText.isNotBlank()) {
                    Text(
                        text = entry.plainText,
                        fontSize = 14.sp,
                        color = onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                // Bottom row: mood + weather
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.moodLevel != null) {
                        val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = null,
                            tint = moodTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (entry.weather != null) {
                        val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = null,
                            tint = weatherTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (reviewItem.label == "去年今日" && yearDiff > 0) {
                        Text(
                            text = "${yearDiff}年前",
                            fontSize = 11.sp,
                            color = onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Left accent bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentBarColor.copy(alpha = 0.7f),
                            accentBarColor.copy(alpha = 0.25f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ReviewEmptyState(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "emptyFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    val floatAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = floatOffset.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primary.copy(alpha = floatAlpha),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "暂无回顾日记",
                fontSize = 18.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "坚持记录，未来的你会感谢现在的自己",
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "过段时间再来看看吧",
                fontSize = 12.sp,
                color = onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}

private fun formatDate(date: LocalDate, today: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
    return date.format(formatter)
}
