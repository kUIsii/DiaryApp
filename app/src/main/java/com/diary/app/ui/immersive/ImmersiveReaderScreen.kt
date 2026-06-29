package com.diary.app.ui.immersive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImmersiveReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFocusMode: () -> Unit = {},
    viewModel: ImmersiveReaderViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val warmLightEnabled by viewModel.warmLightEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontType by viewModel.fontType.collectAsState()
    val sessionReadCount by viewModel.sessionReadCount.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    var direction by remember { mutableIntStateOf(1) }

    LaunchedEffect(currentPage) {
        if (entries.isNotEmpty()) viewModel.trackPageRead()
    }

    val warmOverlay = Color(0xFFFFF3E0).copy(alpha = 0.3f)
    val darkOverlay = Color(0xFF1A1A2E).copy(alpha = 0.85f)
    val pageBgColor = if (darkModeEnabled) Color(0xFF1E1E2E) else Color.Transparent
    val textColorPrimary = if (darkModeEnabled) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onBackground
    val textColorSecondary = if (darkModeEnabled) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("还没有日记可以阅读", fontSize = 16.sp, color = textColorPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "试试专注模式，在安静环境中记录你的想法",
                                fontSize = 13.sp,
                                color = textColorSecondary
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProgressBar(
                            currentPage = currentPage,
                            totalPages = entries.size,
                            color = if (darkModeEnabled) Color(0xFFBB86FC) else MaterialTheme.colorScheme.primary,
                            textColor = textColorSecondary
                        )

                        PageContent(
                            entries = entries,
                            currentPage = currentPage,
                            direction = direction,
                            fontSize = fontSize,
                            fontType = fontType,
                            pageBgColor = pageBgColor,
                            textColorPrimary = textColorPrimary,
                            textColorSecondary = textColorSecondary,
                            onSwipeLeft = {
                                if (currentPage < entries.size - 1) {
                                    direction = 1; currentPage++
                                }
                            },
                            onSwipeRight = {
                                if (currentPage > 0) {
                                    direction = -1; currentPage--
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        BottomBar(
                            currentPage = currentPage,
                            totalPages = entries.size,
                            sessionReadCount = sessionReadCount,
                            elapsedSeconds = elapsedSeconds,
                            textColor = textColorSecondary
                        )
                    }

                    TopControls(
                        onNavigateBack = onNavigateBack,
                        warmLightEnabled = warmLightEnabled,
                        darkModeEnabled = darkModeEnabled,
                        fontSize = fontSize,
                        fontType = fontType,
                        onToggleWarmLight = { viewModel.toggleWarmLight() },
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onCycleFontType = { viewModel.cycleFontType() },
                        onChangeFontSize = { dir -> viewModel.setFontSize(fontSize + dir) },
                        onNavigateToFocusMode = onNavigateToFocusMode
                    )

                    NavButtons(
                        currentPage = currentPage,
                        totalPages = entries.size,
                        textColor = textColorSecondary,
                        onPrev = { direction = -1; currentPage-- },
                        onNext = { direction = 1; currentPage++ },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        if (warmLightEnabled) {
            Box(modifier = Modifier.fillMaxSize().background(warmOverlay))
        }
        if (darkModeEnabled) {
            Box(modifier = Modifier.fillMaxSize().background(darkOverlay))
        }
    }
}

@Composable
private fun ProgressBar(
    currentPage: Int,
    totalPages: Int,
    color: Color,
    textColor: Color
) {
    val progress = if (totalPages > 0) (currentPage + 1).toFloat() / totalPages else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = color,
            trackColor = Color.Transparent
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}

@Composable
private fun PageContent(
    entries: List<DiaryPreview>,
    currentPage: Int,
    direction: Int,
    fontSize: Int,
    fontType: FontType,
    pageBgColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            val animDir = if (direction > 0) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(380),
                initialOffsetX = { fullWidth -> animDir * fullWidth }
            ) + fadeIn(animationSpec = tween(380))) togetherWith
            (slideOutHorizontally(
                animationSpec = tween(300),
                targetOffsetX = { fullWidth -> -animDir * fullWidth }
            ) + fadeOut(animationSpec = tween(200)))
        },
        label = "pageFlip",
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(currentPage) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onHorizontalDrag = { _, _ -> }
                )
            }
    ) { page ->
        val entry = entries.getOrNull(page) ?: return@AnimatedContent
        DiaryPage(
            entry = entry,
            fontSize = fontSize,
            fontType = fontType,
            pageBgColor = pageBgColor,
            textColorPrimary = textColorPrimary,
            textColorSecondary = textColorSecondary,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DiaryPage(
    entry: DiaryPreview,
    fontSize: Int,
    fontType: FontType,
    pageBgColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
    val dateStr = dateFormat.format(Date(entry.createdAt))
    val fontFamily = when (fontType) {
        FontType.DEFAULT -> FontFamily.Default
        FontType.SERIF -> FontFamily.Serif
        FontType.MONOSPACE -> FontFamily.Monospace
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBgColor)
            .padding(horizontal = 32.dp, vertical = 80.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dateStr,
            fontSize = (fontSize - 4).sp,
            color = textColorSecondary,
            fontFamily = fontFamily,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (entry.title.isNotBlank()) {
            Text(
                text = entry.title,
                fontSize = (fontSize + 4).sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary,
                fontFamily = fontFamily,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Text(
            text = entry.plainText,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.8).sp,
            color = textColorPrimary,
            fontFamily = fontFamily
        )

        entry.moodLevel?.let { mood ->
            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (mood) {
                                1 -> Color(0xFFE53935)
                                2 -> Color(0xFFFF9800)
                                3 -> Color(0xFFFFC107)
                                4 -> Color(0xFF8BC34A)
                                5 -> Color(0xFF4CAF50)
                                6 -> Color(0xFF00BCD4)
                                else -> Color.Gray
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "心情 $mood/6",
                    fontSize = 12.sp,
                    color = textColorSecondary,
                    fontFamily = fontFamily
                )
            }
        }
    }
}

@Composable
private fun TopControls(
    onNavigateBack: () -> Unit,
    warmLightEnabled: Boolean,
    darkModeEnabled: Boolean,
    fontSize: Int,
    fontType: FontType,
    onToggleWarmLight: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onCycleFontType: () -> Unit,
    onChangeFontSize: (Int) -> Unit,
    onNavigateToFocusMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
        }

        Row {
            IconButton(onClick = onToggleWarmLight) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "暖光",
                    tint = if (warmLightEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = "深色模式",
                    tint = if (darkModeEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCycleFontType) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            color = if (fontType != FontType.DEFAULT) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (fontType) {
                            FontType.DEFAULT -> "D"
                            FontType.SERIF -> "S"
                            FontType.MONOSPACE -> "M"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            IconButton(onClick = { onChangeFontSize(2) }) {
                Icon(Icons.Default.TextFields, contentDescription = "字体大小")
            }
            IconButton(onClick = onNavigateToFocusMode) {
                Icon(Icons.Default.Timer, contentDescription = "专注模式")
            }
        }
    }
}

@Composable
private fun BottomBar(
    currentPage: Int,
    totalPages: Int,
    sessionReadCount: Int,
    elapsedSeconds: Long,
    textColor: Color
) {
    val minutes = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timeStr = "%d分%02d秒".format(minutes, secs)
    val remainingPages = totalPages - currentPage - 1
    val avgSecondsPerPage = if (sessionReadCount > 0) elapsedSeconds / sessionReadCount else 0L
    val estimatedRemaining = avgSecondsPerPage * remainingPages
    val estMin = estimatedRemaining / 60
    val estSec = estimatedRemaining % 60
    val estStr = if (remainingPages > 0) "约${estMin}分${estSec}秒" else "即将完成"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$timeStr · 已读${sessionReadCount}篇",
            fontSize = 11.sp,
            color = textColor
        )
        Text(
            text = "${currentPage + 1} / $totalPages · $estStr",
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
private fun NavButtons(
    currentPage: Int,
    totalPages: Int,
    textColor: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        if (currentPage > 0) {
            Button(
                onClick = onPrev,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.size(width = 100.dp, height = 48.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一页", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("上页", fontSize = 13.sp)
            }
        } else {
            Spacer(modifier = Modifier.size(width = 100.dp, height = 48.dp))
        }

        if (currentPage < totalPages - 1) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.size(width = 100.dp, height = 48.dp)
            ) {
                Text("下页", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "下一页", modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(modifier = Modifier.size(width = 100.dp, height = 48.dp))
        }
        }
    }
}
