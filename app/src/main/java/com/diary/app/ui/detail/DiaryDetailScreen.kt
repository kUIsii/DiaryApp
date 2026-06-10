package com.diary.app.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebSettings
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.R
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryPreview
import com.diary.app.data.Tag
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.theme.isDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiaryDetailScreen(
    diaryId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val themeMode by app.themeMode.collectAsState()
    val isDark = themeMode.isDark()

    val viewModel: DiaryDetailViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val entry by viewModel.entry.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val relatedEntries by viewModel.relatedEntries.collectAsState()
    val loadError by viewModel.loadError.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    val fontSizePx = remember {
        val prefs = context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
        when (prefs.getString("editor_font_size", "small")) {
            "tiny" -> 10
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Simple fade-in for content
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(entry) {
        if (entry != null) {
            contentVisible = true
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar - minimal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = textSecondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Error state
            if (loadError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "日记加载失败",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "内容可能已损坏，请返回重试",
                            fontSize = 13.sp,
                            color = textSecondary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onNavigateBack) {
                            Text("返回", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                return@GradientBackground
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.delete_diary)) },
                    text = { Text(stringResource(R.string.delete_diary_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                viewModel.deleteEntry()
                                onNavigateBack()
                            }
                        }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            entry?.let { currentEntry ->
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(400))
                ) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        // Header: centered date + mood
                        DetailHeader(
                            entry = currentEntry,
                            textColor = textColor,
                            textSecondary = textSecondary
                        )

                        // Tags
                        if (tags.isNotEmpty()) {
                            DetailTags(tags = tags)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Content WebView
                        val maxContentSize = 2 * 1024 * 1024 // 2MB limit for WebView content
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                                            if (currentEntry.content.isNotBlank()) {
                                                try {
                                                    val safeContent = currentEntry.content
                                                    if (safeContent.length > maxContentSize) {
                                                        val fallback = currentEntry.plainText.take(5000)
                                                        evaluateJavascript("setContent(${org.json.JSONObject.quote(fallback)})", null)
                                                    } else {
                                                        val encoded = android.util.Base64.encodeToString(
                                                            safeContent.toByteArray(Charsets.UTF_8),
                                                            android.util.Base64.NO_WRAP
                                                        )
                                                        evaluateJavascript("setContentFromBase64('$encoded')", null)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    val fallback = currentEntry.plainText.take(5000)
                                                    evaluateJavascript("setContent(${org.json.JSONObject.quote(fallback)})", null)
                                                }
                                            } else if (currentEntry.plainText.isNotBlank()) {
                                                val fallback = currentEntry.plainText.take(5000)
                                                evaluateJavascript("setContent(${org.json.JSONObject.quote(fallback)})", null)
                                            }
                                            evaluateJavascript("setFontSize($fontSizePx)", null)
                                        }
                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: android.webkit.WebResourceRequest?
                                        ): android.webkit.WebResourceResponse? {
                                            val reqUrl = request?.url?.toString() ?: return null
                                            if (reqUrl.startsWith("file://") && reqUrl.contains("diary_media")) {
                                                try {
                                                    val path = reqUrl.removePrefix("file://")
                                                    val file = java.io.File(path)
                                                    if (file.exists() && file.canRead()) {
                                                        val mime = when {
                                                            path.endsWith(".mp4") -> "video/mp4"
                                                            path.endsWith(".mp3") || path.endsWith(".aac") -> "audio/mpeg"
                                                            path.endsWith(".png") -> "image/png"
                                                            path.endsWith(".webp") -> "image/webp"
                                                            else -> "image/jpeg"
                                                        }
                                                        return android.webkit.WebResourceResponse(mime, null, file.inputStream())
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                            return null
                                        }
                                    }
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    setBackgroundColor(0)
                                    loadUrl("file:///android_asset/viewer.html")
                                    webView = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp)
                                .padding(horizontal = 24.dp)
                        )

                        // Timestamps
                        DetailTimestamps(
                            createdAt = currentEntry.createdAt,
                            updatedAt = currentEntry.updatedAt,
                            textSecondary = textSecondary,
                            plainText = currentEntry.plainText
                        )

                        // Related entries (same day in previous years)
                        if (relatedEntries.isNotEmpty()) {
                            RelatedEntriesSection(
                                entries = relatedEntries,
                                onEntryClick = { onNavigateToDetail(it.id) },
                                textSecondary = textSecondary
                            )
                        }

                        // Bottom action bar
                        DetailBottomBar(
                            isFavorite = currentEntry.isFavorite,
                            onEdit = { onNavigateToEditor(diaryId) },
                            onDelete = { showDeleteDialog = true },
                            onToggleFavorite = { viewModel.toggleFavorite() }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date - centered
        Text(
            text = dateText,
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Day of week + time
        Text(
            text = "$dayOfWeek  $timeText",
            fontSize = 13.sp,
            color = textSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Mood and weather row - compact
        if (entry.moodLevel != null || entry.weather != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (entry.moodLevel != null) {
                    val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                    val moodLabel = moodLabelForLevel(entry.moodLevel)
                    Icon(
                        imageVector = moodIcon,
                        contentDescription = moodLabel,
                        tint = moodTint.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = moodLabel,
                        fontSize = 12.sp,
                        color = moodTint.copy(alpha = 0.7f)
                    )
                }

                if (entry.moodLevel != null && entry.weather != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "·",
                        fontSize = 12.sp,
                        color = textSecondary.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = entry.weather,
                        tint = weatherTint.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.weather,
                        fontSize = 12.sp,
                        color = weatherTint.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Location
        val locationText = entry.location?.trim().takeUnless { it.isNullOrEmpty() }
        if (locationText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "位置",
                    tint = textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = locationText,
                    fontSize = 12.sp,
                    color = textSecondary.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTags(tags: List<Tag>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            val tagColor = Color(tag.color)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tagColor.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = tag.name,
                    fontSize = 12.sp,
                    color = tagColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DetailTimestamps(
    createdAt: Long,
    updatedAt: Long,
    textSecondary: Color,
    plainText: String = ""
) {
    val createdText = formatFullTimestamp(createdAt)
    val updatedText = formatFullTimestamp(updatedAt)
    val isEdited = updatedAt - createdAt > 60_000

    // Estimate reading time (average 300 Chinese characters per minute)
    val readingTimeMinutes = if (plainText.isNotBlank()) {
        maxOf(1, plainText.length / 300)
    } else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "创建于 $createdText",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.55f)
            )
            if (readingTimeMinutes > 0) {
                Text(
                    text = "约${readingTimeMinutes}分钟阅读",
                    fontSize = 11.sp,
                    color = textSecondary.copy(alpha = 0.5f)
                )
            }
        }
        if (isEdited) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "修改于 $updatedText",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun DetailBottomBar(
    isFavorite: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Edit
        BottomActionButton(
            icon = Icons.Default.Edit,
            label = "编辑",
            tint = textSecondary.copy(alpha = 0.7f),
            onClick = onEdit
        )

        // Delete
        BottomActionButton(
            icon = Icons.Default.Delete,
            label = "删除",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            onClick = onDelete
        )

        // Favorite
        BottomActionButton(
            icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            label = if (isFavorite) "已收藏" else "收藏",
            tint = if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else textSecondary.copy(alpha = 0.7f),
            onClick = onToggleFavorite
        )
    }
}

@Composable
private fun BottomActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint
        )
    }
}

@Composable
private fun RelatedEntriesSection(
    entries: List<DiaryPreview>,
    onEntryClick: (DiaryPreview) -> Unit,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "历年今日",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        entries.forEach { entry ->
            val entryDate = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val year = entryDate.year
            val preview = entry.plainText.take(50)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEntryClick(entry) }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${year}年",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Text(
                    text = preview,
                    fontSize = 12.sp,
                    color = textSecondary.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE))
}
