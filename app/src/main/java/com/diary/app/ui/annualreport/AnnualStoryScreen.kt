package com.diary.app.ui.annualreport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DesignTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualStoryScreen(
    year: Int,
    report: AnnualReport,
    onNavigateBack: () -> Unit,
    viewModel: AnnualStoryViewModel = viewModel()
) {
    val story by viewModel.story.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()

    LaunchedEffect(year) {
        viewModel.loadStory(year, report)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text("${year} 年度故事") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val showLoading = loadingState.chaptersLoading
                val chapters = story.chapters
                if (showLoading && chapters.isEmpty()) {
                    StorySkeleton()
                } else if (showLoading && chapters.isNotEmpty()) {
                    StoryContent(
                        story = story,
                        loadingState = loadingState,
                        viewModel = viewModel
                    )
                } else if (chapters.isNotEmpty()) {
                    StoryContent(
                        story = story,
                        loadingState = loadingState,
                        viewModel = viewModel
                    )
                } else if (!viewModel.aiEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
                            Text(
                                text = "需要开启AI功能",
                                fontSize = DesignTokens.FontMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = "在设置中配置AI服务后，即可生成个性化年度故事",
                                fontSize = DesignTokens.FontBody,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryContent(
    story: AnnualStory,
    loadingState: StoryLoadingState,
    viewModel: AnnualStoryViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DesignTokens.SpacingLg)
    ) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

        Text(
            text = "${story.year} 年的故事",
            fontSize = DesignTokens.FontHeadline,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        Text(
            text = "AI 从你的日记中发现了 ${story.chapters.size} 个故事章节",
            fontSize = DesignTokens.FontSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))

        story.patterns.forEachIndexed { index, pattern ->
            PatternCard(pattern = pattern)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        story.chapters.forEachIndexed { index, chapter ->
            if (index < story.patterns.size) {
                val pattern = story.patterns[index]
                PatternCard(pattern = pattern)
                Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
            }
            ChapterCard(
                chapter = chapter,
                index = index,
                isExpanded = viewModel.expandedChapterIndex.collectAsState().value == index,
                onToggleExpand = { viewModel.setExpandedChapter(if (it) index else null) },
                annotations = story.userAnnotations.filter { it.chapterTitle == chapter.title },
                onAnnotate = { paraIdx ->
                    viewModel.addAnnotation(chapter.title, paraIdx, "待补充")
                }
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        if (loadingState.patternsLoading && story.patterns.isEmpty()) {
            PatternSkeleton()
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        if (story.crossYearInsights != null && story.crossYearInsights.isNotEmpty()) {
            CrossYearSection(insights = story.crossYearInsights)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        if (story.blindSpotNotes.isNotEmpty()) {
            BlindSpotSection(blindSpots = story.blindSpotNotes)
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        if (story.userAnnotations.isNotEmpty()) {
            AnnotationsSection(
                annotations = story.userAnnotations,
                onRemove = { viewModel.removeAnnotation(it) }
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
    }
}

@Composable
private fun ChapterCard(
    chapter: StoryChapter,
    index: Int,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    annotations: List<UserAnnotation>,
    onAnnotate: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    GlassCard(
        cornerRadius = DesignTokens.CornerXLarge,
        innerPadding = DesignTokens.SpacingLg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(DesignTokens.CornerSmall),
                    color = primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = DesignTokens.FontSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                Column {
                    Text(
                        text = chapter.title,
                        fontSize = DesignTokens.FontLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (chapter.style.isNotBlank()) {
                        Text(
                            text = chapter.style,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            Text(
                text = chapter.summary,
                fontSize = DesignTokens.FontBody,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            if (chapter.emotionSparkline.size >= 3) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                EmotionSparkline(chapter.emotionSparkline)
            }

            if (chapter.entryIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Text(
                    text = "相关日记 (${chapter.entryIds.size})",
                    fontSize = DesignTokens.FontSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (annotations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                annotations.forEach { annotation ->
                    AnnotationBubble(annotation)
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                Surface(
                    shape = RoundedCornerShape(DesignTokens.CornerSmall),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onAnnotate(0) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                        Text(
                            text = "添加注释",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(DesignTokens.CornerSmall),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onToggleExpand(!isExpanded) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingXs))
                        Text(
                            text = "展开更多",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Divider()
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    Text(
                        text = "支持性日记内容将在此展开",
                        fontSize = DesignTokens.FontBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternCard(pattern: DiscoveredPattern) {
    val primary = MaterialTheme.colorScheme.primary

    GlassCard(
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = CircleShape,
                color = primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(DesignTokens.SpacingSm).size(DesignTokens.IconMedium)
                )
            }
            Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
            Column {
                Text(
                    text = "你知道吗？",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Text(
                    text = pattern.description,
                    fontSize = DesignTokens.FontBody,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CrossYearSection(insights: List<CrossYearInsight>) {
    SectionHeader(icon = Icons.Default.Timeline, title = "跨年对比", subtitle = "与去年的差异")

    insights.forEach { insight ->
        GlassCard(
            cornerRadius = DesignTokens.CornerMedium,
            innerPadding = DesignTokens.SpacingLg,
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingSm)
        ) {
            Column {
                Text(
                    text = dimensionLabel(insight.dimension),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (insight.changePercent > 0) "+${String.format("%.0f", insight.changePercent)}%" else "${String.format("%.0f", insight.changePercent)}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = if (insight.changePercent >= 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                    Column {
                        Text(
                            text = "去年: ${insight.priorYearValue}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "今年: ${insight.currentYearValue}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(
                    text = insight.description,
                    fontSize = DesignTokens.FontBody,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun BlindSpotSection(blindSpots: List<BlindSpot>) {
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日") }

    SectionHeader(icon = Icons.Default.Info, title = "被忽略的时光", subtitle = "沉默期里可能发生了什么")

    blindSpots.forEach { spot ->
        GlassCard(
            cornerRadius = DesignTokens.CornerMedium,
            innerPadding = DesignTokens.SpacingLg,
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingSm)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingMd))
                Column {
                    Text(
                        text = "${spot.periodStart.format(formatter)} - ${spot.periodEnd.format(formatter)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
                    Text(
                        text = spot.inferredReason,
                        fontSize = DesignTokens.FontBody,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotationsSection(
    annotations: List<UserAnnotation>,
    onRemove: (String) -> Unit
) {
    SectionHeader(icon = Icons.Default.Edit, title = "我的注释", subtitle = "你添加的年度注解")

    annotations.forEach { annotation ->
        GlassCard(
            cornerRadius = DesignTokens.CornerMedium,
            innerPadding = DesignTokens.SpacingMd,
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingSm)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = annotation.chapterTitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    IconButton(
                        onClick = { onRemove(annotation.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "删除注释",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = annotation.note,
                    fontSize = DesignTokens.FontBody,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AnnotationBubble(annotation: UserAnnotation) {
    Surface(
        shape = RoundedCornerShape(
            topStart = DesignTokens.CornerSmall,
            topEnd = DesignTokens.CornerLarge,
            bottomEnd = DesignTokens.CornerLarge,
            bottomStart = DesignTokens.CornerLarge
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Text(
            text = annotation.note,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm)
        )
    }
}

@Composable
private fun EmotionSparkline(values: List<Float>) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(
                x = i * step,
                y = size.height * (1f - v.coerceIn(0f, 1f))
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val mid = Offset((prev.x + points[i].x) / 2f, (prev.y + points[i].y) / 2f)
                quadraticBezierTo(prev.x, mid.y, mid.x, mid.y)
                quadraticBezierTo(points[i].x, mid.y, points[i].x, points[i].y)
            }
        }
        drawPath(path, primary, style = Stroke(width = 2.5f))

        points.forEach { p ->
            drawCircle(color = primary, radius = 3f, center = p)
            drawCircle(color = Color.White, radius = 1.5f, center = p)
        }
    }
}

@Composable
private fun StorySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignTokens.SpacingLg)
    ) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXxl))
        repeat(4) {
            SkeletonCard()
            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))
        }
    }
}

@Composable
private fun PatternSkeleton() {
    GlassCard(
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = DesignTokens.SpacingLg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun SkeletonCard() {
    GlassCard(
        cornerRadius = DesignTokens.CornerXLarge,
        innerPadding = DesignTokens.SpacingLg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
            }
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(DesignTokens.IconMedium)
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        Column {
            Text(
                text = title,
                fontSize = DesignTokens.FontTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = DesignTokens.FontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
    Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    )
}

private fun dimensionLabel(dimension: String): String = when (dimension) {
    "emotion" -> "情绪变化"
    "topic" -> "话题分布"
    "volume" -> "写作量"
    "social" -> "社交提及"
    else -> dimension
}
