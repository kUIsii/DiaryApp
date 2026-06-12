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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.WebViewAssetHelper
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.theme.isDark
import kotlinx.coroutines.launch
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

    val detailJsBridge = remember { DetailJsBridge() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var webViewContentHeight by remember { mutableFloatStateOf(0f) }
    var contentReady by remember { mutableStateOf(false) }

    // 图片查看器状态
    var imageViewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageViewerIndex by remember { mutableIntStateOf(0) }

    // 分享对话框状态
    var showShareDialog by remember { mutableStateOf(false) }

    // 监听图片点击事件
    LaunchedEffect(Unit) {
        detailJsBridge.imageClicks.collect { event ->
            imageViewerUrls = event.allUrls
            imageViewerIndex = event.allUrls.indexOf(event.clickedUrl).coerceAtLeast(0)
        }
    }

    // 监听 WebView 内容高度
    val densityValue = LocalDensity.current.density
    LaunchedEffect(Unit) {
        detailJsBridge.contentHeight.collect { heightPx ->
            val newHeight = heightPx / densityValue + 8f
            if (newHeight > webViewContentHeight) {
                webViewContentHeight = newHeight
            }
            if (!contentReady && heightPx > 0) {
                contentReady = true
            }
        }
    }

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

            // Share format dialog
            if (showShareDialog) {
                ShareFormatDialog(
                    onDismiss = { showShareDialog = false },
                    onShareText = {
                        showShareDialog = false
                        val text = viewModel.getShareText()
                        if (text != null) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享日记"))
                        }
                    },
                    onShareImage = {
                        showShareDialog = false
                        scope.launch {
                            try {
                                val path = viewModel.exportAsImage(context)
                                if (path != null) {
                                    Toast.makeText(context, "已保存到 $path", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShareHtml = {
                        showShareDialog = false
                        scope.launch {
                            try {
                                val path = viewModel.exportAsHtml(context)
                                if (path != null) {
                                    Toast.makeText(context, "已保存到 $path", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            entry?.let { currentEntry ->
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(400))
                ) {
                    val maxContentSize = 2 * 1024 * 1024
                    val assetLoader = remember { WebViewAssetHelper.createAssetLoader(context) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Everything in one scrollable card
                        GlassCard(
                            cornerRadius = 16.dp,
                            innerPadding = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column {
                                // Header (title + date + mood/weather/location/tags)
                                DetailHeaderCompact(
                                    entry = currentEntry,
                                    tags = tags,
                                    textColor = textColor,
                                    textSecondary = textSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // WebView content
                                Box {
                                    if (!contentReady) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "加载中...",
                                                fontSize = 13.sp,
                                                color = textSecondary.copy(alpha = 0.4f)
                                            )
                                        }
                                    }

                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                isVerticalScrollBarEnabled = false
                                                overScrollMode = WebView.OVER_SCROLL_NEVER
                                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                                                )
                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                                                        if (currentEntry.content.isNotBlank()) {
                                                            try {
                                                                var safeContent = currentEntry.content.replace(
                                                                    Regex("\"file://([^\"]*diary_media[^\"]*?)\"")
                                                                ) { match ->
                                                                    "\"${WebViewAssetHelper.toWebViewUrlFromFileUrl("file://${match.groupValues[1]}")}\""
                                                                }
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
                                                        val assetResponse = WebViewAssetHelper.interceptRequest(assetLoader, request)
                                                        if (assetResponse != null) return assetResponse
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
                                                        return super.shouldInterceptRequest(view, request)
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
                                                addJavascriptInterface(detailJsBridge, "DiaryBridge")
                                                loadUrl("file:///android_asset/viewer.html")
                                                webView = this
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .alpha(if (contentReady) 1f else 0f)
                                    )
                                }

                                // Timestamps
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailTimestamps(
                                    createdAt = currentEntry.createdAt,
                                    updatedAt = currentEntry.updatedAt,
                                    textSecondary = textSecondary,
                                    plainText = currentEntry.plainText
                                )
                            }
                        }

                        // Related entries (outside the main card)
                        if (relatedEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            RelatedEntriesSection(
                                entries = relatedEntries,
                                onEntryClick = { onNavigateToDetail(it.id) },
                                textSecondary = textSecondary
                            )
                        }

                        // Bottom action bar
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            cornerRadius = 16.dp,
                            innerPadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DetailBottomBar(
                                isFavorite = currentEntry.isFavorite,
                                onEdit = { onNavigateToEditor(diaryId) },
                                onDelete = { showDeleteDialog = true },
                                onToggleFavorite = { viewModel.toggleFavorite() },
                                onShare = { showShareDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // 全屏图片查看器
    if (imageViewerUrls.isNotEmpty()) {
        ImageViewerScreen(
            imageUrls = imageViewerUrls,
            initialIndex = imageViewerIndex,
            onDismiss = {
                imageViewerUrls = emptyList()
                imageViewerIndex = 0
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeaderCompact(
    entry: DiaryEntry,
    tags: List<Tag>,
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
    val titleText = entry.title.trim().takeUnless { it.isNullOrEmpty() }
    val hasMoodOrWeather = entry.moodLevel != null || entry.weather != null
    val locationText = entry.location?.trim().takeUnless { it.isNullOrEmpty() }
    val hasMeta = hasMoodOrWeather || locationText != null || tags.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        if (titleText != null) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Date + weekday + time
        Text(
            text = "$dateText  $dayOfWeek  $timeText",
            fontSize = 13.sp,
            color = textSecondary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        // Meta row: mood + weather + location + tags
        if (hasMeta) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mood
                if (entry.moodLevel != null) {
                    val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                    val moodLabel = moodLabelForLevel(entry.moodLevel)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = moodLabel,
                            tint = moodTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = moodLabel,
                            fontSize = 12.sp,
                            color = moodTint.copy(alpha = 0.7f)
                        )
                    }
                }

                if (entry.moodLevel != null && (entry.weather != null || locationText != null || tags.isNotEmpty())) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "|",
                        fontSize = 12.sp,
                        color = textSecondary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Weather
                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = entry.weather,
                            tint = weatherTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = entry.weather,
                            fontSize = 12.sp,
                            color = weatherTint.copy(alpha = 0.7f)
                        )
                    }
                }

                if (entry.weather != null && (locationText != null || tags.isNotEmpty())) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "|",
                        fontSize = 12.sp,
                        color = textSecondary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Location
                if (locationText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "位置",
                            tint = textSecondary.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = locationText,
                            fontSize = 12.sp,
                            color = textSecondary.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }

                if (locationText != null && tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "|",
                        fontSize = 12.sp,
                        color = textSecondary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Tags
                tags.forEach { tag ->
                    val tagColor = Color(tag.color)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(tagColor.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = tag.name,
                            fontSize = 12.sp,
                            color = tagColor.copy(alpha = 0.7f)
                        )
                    }
                }
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

    val readingTimeMinutes = if (plainText.isNotBlank()) {
        maxOf(1, plainText.length / 300)
    } else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "创建于 $createdText",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.5f)
            )
            if (readingTimeMinutes > 0) {
                Text(
                    text = "约${readingTimeMinutes}分钟阅读",
                    fontSize = 11.sp,
                    color = textSecondary.copy(alpha = 0.45f)
                )
            }
        }
        if (isEdited) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "修改于 $updatedText",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DetailBottomBar(
    isFavorite: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomActionButton(
            icon = Icons.Default.Edit,
            label = "编辑",
            tint = textSecondary.copy(alpha = 0.7f),
            onClick = onEdit
        )

        BottomActionButton(
            icon = Icons.Default.Delete,
            label = "删除",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            onClick = onDelete
        )

        BottomActionButton(
            icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            label = if (isFavorite) "已收藏" else "收藏",
            tint = if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else textSecondary.copy(alpha = 0.7f),
            onClick = onToggleFavorite
        )

        BottomActionButton(
            icon = Icons.Default.Share,
            label = "分享",
            tint = textSecondary.copy(alpha = 0.7f),
            onClick = onShare
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "历年今日",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary.copy(alpha = 0.55f),
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
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEntryClick(entry) }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${year}年",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                )
                Text(
                    text = preview,
                    fontSize = 12.sp,
                    color = textSecondary.copy(alpha = 0.55f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun formatFullTimestamp(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINESE))
}
