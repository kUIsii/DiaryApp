package com.diary.app.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebSettings
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.diary.app.ui.components.IconWithTint
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherIconFor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.Tag
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.sharedElementTransition
import com.diary.app.ui.theme.isDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiaryDetailScreen(
    diaryId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val themeMode by app.themeMode.collectAsState()
    val isDark = themeMode.isDark()

    val viewModel: DiaryDetailViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val entry by viewModel.entry.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    val fontSizePx = remember {
        val prefs = context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
        when (prefs.getString("editor_font_size", "medium")) {
            "small" -> 14
            "large" -> 18
            "extra_large" -> 20
            else -> 16
        }
    }

    LaunchedEffect(diaryId) {
        viewModel.loadEntry(diaryId)
    }

    LaunchedEffect(themeMode) {
        webView?.evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
    }

    // Cleanup WebView on dispose to prevent memory leak
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    var showShareMenu by remember { mutableStateOf(false) }
    var isExportingImage by remember { mutableStateOf(false) }
    var isExportingMarkdown by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = textSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showShareMenu = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("分享文字") },
                            onClick = {
                                showShareMenu = false
                                val shareText = viewModel.getShareText()
                                if (shareText != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_SUBJECT, "日记 - ${viewModel.getDateTitle()}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享日记"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isExportingImage) "生成图片中..." else "分享为图片")
                            },
                            enabled = !isExportingImage,
                            onClick = {
                                showShareMenu = false
                                isExportingImage = true
                                scope.launch {
                                    try {
                                        val path = viewModel.exportAsImage(context)
                                        isExportingImage = false
                                        if (path != null) {
                                            Toast.makeText(context, "图片已保存到 $path", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        isExportingImage = false
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isExportingMarkdown) "导出中..." else "导出为 Markdown")
                            },
                            enabled = !isExportingMarkdown,
                            onClick = {
                                showShareMenu = false
                                isExportingMarkdown = true
                                scope.launch {
                                    try {
                                        val path = viewModel.exportToMarkdown(context)
                                        isExportingMarkdown = false
                                        if (path != null) {
                                            Toast.makeText(context, "已导出到 $path", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        isExportingMarkdown = false
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onNavigateToEditor(diaryId) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "编辑",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("删除日记") },
                    text = { Text("确定要删除这篇日记吗？此操作无法撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            haptic.warning()
                            scope.launch {
                                viewModel.deleteEntry()
                                onNavigateBack()
                            }
                        }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            entry?.let { currentEntry ->
                // Stagger animation states
                var headerVisible by remember { mutableStateOf(false) }
                var tagsVisible by remember { mutableStateOf(false) }
                var contentVisible by remember { mutableStateOf(false) }

                LaunchedEffect(currentEntry) {
                    headerVisible = true
                    delay(100)
                    tagsVisible = true
                    delay(100)
                    contentVisible = true
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: date, time, mood, weather with shared element transition
                    Box(
                        modifier = Modifier.sharedElementTransition(
                            visible = headerVisible,
                            durationMillis = 300
                        )
                    ) {
                        DetailHeader(
                            entry = currentEntry,
                            textColor = textColor,
                            textSecondary = textSecondary
                        )
                    }

                    // Tags
                    if (tags.isNotEmpty()) {
                        AnimatedVisibility(
                            visible = tagsVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 }
                        ) {
                            DetailTags(tags = tags)
                        }
                    }

                    // Content WebView - takes remaining space, scrolls internally
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(tween(400))
                    ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                setBackgroundColor(0)
                                loadUrl("file:///android_asset/viewer.html")
                                webView = this
                                post {
                                    evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                                    if (currentEntry.content.isNotBlank()) {
                                        val escaped = currentEntry.content
                                            .replace("\\", "\\\\")
                                            .replace("'", "\\'")
                                            .replace("\n", "\\n")
                                            .replace("\r", "")
                                        evaluateJavascript("setContent('$escaped')", null)
                                    }
                                    evaluateJavascript("setFontSize($fontSizePx)", null)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Bottom timestamps
                    DetailTimestamps(
                        createdAt = currentEntry.createdAt,
                        updatedAt = currentEntry.updatedAt,
                        textSecondary = textSecondary
                    )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    entry: DiaryEntry,
    textColor: Color,
    textSecondary: Color
) {
    val entryDate = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val entryTime = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault()).toLocalTime()
    val dateText = "${entryDate.year}年${entryDate.monthValue}月${entryDate.dayOfMonth}日"
    val dayOfWeek = entryDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINESE))
    val timeText = entryTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    // Mood color accent
    val moodColor = if (entry.moodLevel != null) {
        moodIconForLevel(entry.moodLevel).tint
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Mood color accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(moodColor, moodColor.copy(alpha = 0.3f))
                    )
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            // Date
            Text(
                text = dateText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$dayOfWeek $timeText",
                    fontSize = 14.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Mood icon
                if (entry.moodLevel != null) {
                    val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                    val moodLabel = moodLabelForLevel(entry.moodLevel)
                    Icon(
                        imageVector = moodIcon,
                        contentDescription = moodLabel,
                        tint = moodTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = moodLabel,
                        fontSize = 12.sp,
                        color = moodTint,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Weather icon
                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = entry.weather,
                        tint = weatherTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.weather,
                        fontSize = 12.sp,
                        color = weatherTint,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTags(tags: List<Tag>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val tagColor = Color(tag.color)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tagColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tag.name,
                    fontSize = 13.sp,
                    color = tagColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailTimestamps(
    createdAt: Long,
    updatedAt: Long,
    textSecondary: Color
) {
    val createdText = formatFullTimestamp(createdAt)
    val updatedText = formatFullTimestamp(updatedAt)
    val isEdited = updatedAt - createdAt > 60_000 // More than 1 minute difference

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Column {
            Text(
                text = "创建于 $createdText",
                fontSize = 12.sp,
                color = textSecondary.copy(alpha = 0.6f)
            )
            if (isEdited) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "修改于 $updatedText",
                    fontSize = 12.sp,
                    color = textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE))
}
