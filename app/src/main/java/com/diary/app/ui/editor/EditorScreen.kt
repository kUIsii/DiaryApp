package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryTemplate
import com.diary.app.data.TemplateCategory
import com.diary.app.data.TemplateManager
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.isDark
import kotlinx.coroutines.launch
import android.util.Base64
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Properly unescape a JSON-encoded string returned by WebView.evaluateJavascript().
 * The callback value is a JSON string literal: outer quotes + escaped inner characters.
 * We need to handle: \" -> ", \\ -> \, \n -> newline, \t -> tab, \r -> carriage return
 */
private fun unescapeEvaluateJsResult(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    // Remove surrounding quotes
    val s = if (raw.startsWith("\"") && raw.endsWith("\"")) raw.substring(1, raw.length - 1) else raw
    // Unescape JSON escape sequences
    return s.replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\r", "\r")
        .replace("\\\\", "\u0000")  // temp placeholder for literal backslash
        .replace("\u0000", "\\")     // restore literal backslash
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val today = LocalDate.now()
    val currentTime = LocalTime.now()
    val dateTitle = "${today.year}年${today.monthValue}月${today.dayOfMonth}日"
    val timeText = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val themeMode by app.themeMode.collectAsState()
    val isDark = themeMode.isDark()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isWebViewReady by remember { mutableStateOf(false) }
    val jsBridge = remember { DiaryJsBridge() }
    val viewModel: EditorViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allTags by viewModel.allTags.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val currentEntry by viewModel.currentEntry.collectAsState()

    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var selectedWeather by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Which metadata panel is open: null = none, "mood", "weather", "tags"
    var activePanel by remember { mutableStateOf<String?>(null) }

    // Toolbar state
    var showToolbar by remember { mutableStateOf(true) }
    var activeCategory by remember { mutableIntStateOf(-1) }

    // Word count state
    var charCount by remember { mutableIntStateOf(0) }
    var wordCount by remember { mutableIntStateOf(0) }
    var latestPlainText by remember { mutableStateOf("") }
    var contentVersion by remember { mutableIntStateOf(0) }

    // Auto-save and unsaved changes
    val autoSaveVisible by viewModel.autoSaveVisible.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    // Dialogs
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }
    var pendingDraft by remember { mutableStateOf<DraftData?>(null) }

    // Writing duration and prompt
    val writingDuration by viewModel.writingDuration.collectAsState()
    val writingPrompt by viewModel.writingPrompt.collectAsState()

    LaunchedEffect(diaryId) {
        if (diaryId != null) viewModel.loadEntry(diaryId)
        viewModel.startWritingTimer()
        viewModel.loadWritingPrompt()
    }

    // Update writing duration periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000) // Update every 10 seconds
            viewModel.updateWritingDuration()
        }
    }

    LaunchedEffect(currentEntry, isWebViewReady) {
        if (!isWebViewReady) return@LaunchedEffect
        currentEntry?.let { entry ->
            selectedMood = entry.moodLevel
            selectedWeather = entry.weather
            selectedLocation = entry.location
            locationLat = entry.latitude
            locationLng = entry.longitude
            // Inject saved content into WebView when editing existing entry
            if (diaryId != null && entry.content.isNotBlank()) {
                // Strip inline Base64 data URLs to prevent memory crash
                val safeContent = entry.content.replace(
                    Regex("\"data:image/[^\"]{0,5000000}\""),
                    "\"\""
                )
                // Use Base64 encoding to avoid escaping issues
                val base64Content = Base64.encodeToString(
                    safeContent.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
                webView?.evaluateJavascript("setContentBase64('$base64Content')", null)
            }
        }
    }

    // Refresh prompt when mood changes
    LaunchedEffect(selectedMood) {
        if (diaryId == null && charCount == 0) {
            viewModel.loadWritingPrompt(selectedMood)
        }
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

    // Read editor font size from SharedPreferences
    val prefs = remember { context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE) }
    var editorFontSize by remember { mutableIntStateOf(getEditorFontSize(prefs)) }

    // Read font size once on enter; editor screen recreates when returning from settings
    LaunchedEffect(Unit) {
        editorFontSize = getEditorFontSize(prefs)
    }

    LaunchedEffect(themeMode, isWebViewReady) {
        if (isWebViewReady) {
            webView?.evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
        }
    }

    LaunchedEffect(editorFontSize, isWebViewReady) {
        if (isWebViewReady) {
            webView?.evaluateJavascript("setFontSize($editorFontSize)", null)
        }
    }

    // Media pickers - save images as files to avoid Base64 bloat in Delta JSON
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            try {
                val imagesDir = java.io.File(context.filesDir, "diary_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val outputFile = java.io.File(imagesDir, fileName)

                // Read and compress image
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    // Scale down if larger than 1920px on longest side
                    val maxDim = 1920
                    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap
                    outputFile.outputStream().use { out ->
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()

                    val filePath = "file://${outputFile.absolutePath}"
                    webView?.evaluateJavascript("insertMedia('image', '${escapeForJs(filePath)}')", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { mediaUri ->
            try {
                val mediaDir = java.io.File(context.filesDir, "diary_media")
                if (!mediaDir.exists()) mediaDir.mkdirs()
                val ext = context.contentResolver.getType(mediaUri)?.substringAfterLast("/") ?: "mp4"
                val fileName = "vid_${System.currentTimeMillis()}.$ext"
                val outputFile = java.io.File(mediaDir, fileName)
                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                webView?.evaluateJavascript("insertMedia('video', '${escapeForJs("file://${outputFile.absolutePath}")}')", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { mediaUri ->
            try {
                val mediaDir = java.io.File(context.filesDir, "diary_media")
                if (!mediaDir.exists()) mediaDir.mkdirs()
                val ext = context.contentResolver.getType(mediaUri)?.substringAfterLast("/") ?: "mp3"
                val fileName = "aud_${System.currentTimeMillis()}.$ext"
                val outputFile = java.io.File(mediaDir, fileName)
                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
                webView?.evaluateJavascript("insertMedia('audio', '${escapeForJs("file://${outputFile.absolutePath}")}')", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        jsBridge.events.collect { event ->
            when (event) {
                "image" -> imageLauncher.launch("image/*")
                "video" -> videoLauncher.launch("video/*")
                "audio" -> audioLauncher.launch("audio/*")
            }
        }
    }

    // Collect content changes from bridge for word count
    LaunchedEffect(Unit) {
        jsBridge.contentChanges.collect { text ->
            latestPlainText = text
            charCount = text.length
            wordCount = countWords(text)
            viewModel.markContentChanged()
            contentVersion++
        }
    }

    // Collect link insert requests from JS bridge
    LaunchedEffect(Unit) {
        jsBridge.linkInsertRequest.collect { showLinkDialog = true }
    }

    // Auto-save with 5s debounce (softer, less aggressive)
    LaunchedEffect(contentVersion) {
        if (contentVersion > 0) {
            kotlinx.coroutines.delay(5000)
            webView?.evaluateJavascript("getContent()") { json ->
                val cleanJson = unescapeEvaluateJsResult(json)
                viewModel.updateLatestContent(cleanJson, latestPlainText, dateTitle)
                viewModel.performAutoSave(diaryId, selectedMood, selectedWeather, selectedLocation, locationLat, locationLng)
            }
        }
    }

    // Auto-hide auto-save indicator after 2s
    LaunchedEffect(autoSaveVisible) {
        if (autoSaveVisible) {
            kotlinx.coroutines.delay(2000)
            viewModel.hideAutoSaveIndicator()
        }
    }

    // Auto-save when app goes to background (focus loss)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                webView?.evaluateJavascript("getContent()") { json ->
                    webView?.evaluateJavascript("getPlainText()") { plain ->
                        val cleanJson = unescapeEvaluateJsResult(json)
                        val cleanPlain = unescapeEvaluateJsResult(plain)
                        if (cleanPlain.isNotBlank()) {
                            viewModel.updateLatestContent(cleanJson, cleanPlain, dateTitle)
                            viewModel.performAutoSave(diaryId, selectedMood, selectedWeather, selectedLocation, locationLat, locationLng)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check for draft on new entry
    LaunchedEffect(Unit) {
        if (diaryId == null) {
            val draft = viewModel.loadDraft(null)
            if (draft != null && draft.plainText.isNotBlank()) {
                pendingDraft = draft
                showDraftDialog = true
            }
        }
    }

    // Auto-show keyboard after WebView loads
    LaunchedEffect(webView) {
        webView?.let {
            kotlinx.coroutines.delay(500)
            it.requestFocus()
            it.evaluateJavascript(
                "document.querySelector('.ql-editor').focus()",
                null
            )
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    if (showTagDialog) {
        AddTagDialog(
            onDismiss = { showTagDialog = false },
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showTagDialog = false
            }
        )
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes)) },
            text = {
                Column {
                    Text(stringResource(R.string.unsaved_changes_message))
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showUnsavedDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.continue_editing), color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    webView?.evaluateJavascript("getContent()") { json ->
                        webView?.evaluateJavascript("getPlainText()") { plain ->
                            val cleanJson = unescapeEvaluateJsResult(json)
                            val cleanPlain = unescapeEvaluateJsResult(plain)
                            scope.launch {
                                viewModel.saveEntry(dateTitle, cleanJson, cleanPlain, diaryId, selectedMood, selectedWeather, selectedLocation, locationLat, locationLng)
                                showUnsavedDialog = false
                                onNavigateBack()
                            }
                        }
                    }
                }) { Text(stringResource(R.string.save_and_exit)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearDraft(null)
                    showUnsavedDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.exit_without_saving)) }
            }
        )
    }

    // Draft restoration dialog
    if (showDraftDialog && pendingDraft != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearDraft(null)
                showDraftDialog = false
            },
            title = { Text(stringResource(R.string.draft_found)) },
            text = { Text(stringResource(R.string.draft_found_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val draft = pendingDraft!!
                    val encoded = android.util.Base64.encodeToString(
                        draft.content.toByteArray(Charsets.UTF_8),
                        android.util.Base64.NO_WRAP
                    )
                    webView?.evaluateJavascript("setContentBase64('$encoded')", null)
                    selectedMood = draft.moodLevel
                    selectedWeather = draft.weather
                    showDraftDialog = false
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearDraft(null)
                    showDraftDialog = false
                }) { Text(stringResource(R.string.discard)) }
            }
        )
    }

    // Template selection dialog
    if (showTemplateDialog) {
        TemplateDialog(
            onDismiss = { showTemplateDialog = false },
            onTemplateSelected = { template ->
                webView?.evaluateJavascript("setTemplate('${escapeForJs(template.content)}')", null)
                showTemplateDialog = false
            }
        )
    }

    // Link input dialog
    if (showLinkDialog) {
        var linkUrl by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("插入链接") },
            text = {
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("链接地址") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linkUrl.isNotBlank() && linkUrl != "https://") {
                        webView?.evaluateJavascript("insertLinkFromKotlin('${escapeForJs(linkUrl)}')", null)
                    }
                    showLinkDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("取消") }
            }
        )
    }

    // Back press handler
    BackHandler {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Top bar - simplified: only undo, redo, save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (hasUnsavedChanges) showUnsavedDialog = true
                    else onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.navigate_back), tint = textSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                // Auto-save indicator dot
                AnimatedVisibility(
                    visible = autoSaveVisible,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessColor)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { webView?.evaluateJavascript("quill.undo()", null) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.undo), tint = textSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { webView?.evaluateJavascript("quill.redo()", null) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.redo), tint = textSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showTemplateDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.select_template), tint = textSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    webView?.evaluateJavascript("getContent()") { json ->
                        webView?.evaluateJavascript("getPlainText()") { plain ->
                            val cleanJson = unescapeEvaluateJsResult(json)
                            val cleanPlain = unescapeEvaluateJsResult(plain)
                            // Don't save empty entries
                            if (cleanPlain.isBlank() && dateTitle.isBlank()) {
                                onNavigateBack()
                                return@evaluateJavascript
                            }
                            scope.launch {
                                viewModel.saveEntry(
                                    title = dateTitle,
                                    content = cleanJson,
                                    plainText = cleanPlain,
                                    diaryId = diaryId,
                                    moodLevel = selectedMood,
                                    weather = selectedWeather,
                                    location = selectedLocation,
                                    latitude = locationLat,
                                    longitude = locationLng
                                )
                                haptic.success()
                                snackbarHostState.showSnackbar(
                                    message = "日记已保存",
                                    duration = SnackbarDuration.Short
                                )
                                onNavigateBack()
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.save), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }

            // Date + time (compact single line)
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = dateTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(text = timeText, fontSize = 11.sp, color = textSecondary)
            }

            // Writing prompt (only for new entries, hide after 50 chars)
            if (diaryId == null && writingPrompt.isNotBlank() && charCount < 50) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.refreshPrompt() }
                        .background(surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = writingPrompt,
                        fontSize = 13.sp,
                        color = textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Metadata buttons row - simple chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mood chip
                MetadataChip(
                    label = if (selectedMood != null) moodLabelForLevel(selectedMood!!) else "心情",
                    icon = moodIconForLevel(selectedMood ?: 3).icon,
                    isSelected = selectedMood != null,
                    isActive = activePanel == "mood",
                    onClick = { activePanel = if (activePanel == "mood") null else "mood" }
                )
                // Weather chip
                MetadataChip(
                    label = selectedWeather ?: "天气",
                    icon = weatherIconFor(selectedWeather).icon,
                    isSelected = selectedWeather != null,
                    isActive = activePanel == "weather",
                    onClick = { activePanel = if (activePanel == "weather") null else "weather" }
                )
                // Tags chip
                MetadataChip(
                    label = if (selectedTagIds.isNotEmpty()) "${selectedTagIds.size}个标签" else "标签",
                    icon = Icons.Default.Sell,
                    isSelected = selectedTagIds.isNotEmpty(),
                    isActive = activePanel == "tags",
                    onClick = { activePanel = if (activePanel == "tags") null else "tags" }
                )
                // Location chip
                MetadataChip(
                    label = selectedLocation ?: "位置",
                    icon = Icons.Default.LocationOn,
                    isSelected = selectedLocation != null,
                    isActive = activePanel == "location",
                    onClick = { activePanel = if (activePanel == "location") null else "location" }
                )
            }

            // Expandable panels - simple background
            AnimatedVisibility(
                visible = activePanel != null,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceVariant.copy(alpha = 0.5f))
                        .animateContentSize()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        when (activePanel) {
                            "mood" -> Column {
                                MoodSlider(
                                    selectedLevel = selectedMood,
                                    onLevelChange = { selectedMood = it }
                                )
                                if (selectedMood != null) {
                                    TextButton(
                                        onClick = { selectedMood = null },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("清除心情", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            "weather" -> Column {
                                WeatherSelector(
                                    selectedWeather = selectedWeather,
                                    onWeatherSelected = { selectedWeather = it }
                                )
                                if (selectedWeather != null) {
                                    TextButton(
                                        onClick = { selectedWeather = null },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("清除天气", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            "tags" -> TagEditor(
                                allTags = allTags,
                                selectedTagIds = selectedTagIds,
                                onTagToggle = { viewModel.toggleTag(it) },
                                onAddTag = { showTagDialog = true }
                            )
                            "location" -> LocationSelector(
                                selectedLocation = selectedLocation,
                                latitude = locationLat,
                                longitude = locationLng,
                                onLocationSelected = { name, lat, lng ->
                                    selectedLocation = name
                                    locationLat = lat
                                    locationLng = lng
                                }
                            )
                        }
                    }
                }
            }

            // WebView (fills remaining space)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebViewReady = true
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setBackgroundColor(0)
                        addJavascriptInterface(jsBridge, "DiaryBridge")
                        loadUrl("file:///android_asset/editor.html")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Word count and writing duration
            if (charCount > 0 || writingDuration > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (charCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${charCount}字",
                                fontSize = 11.sp,
                                color = textSecondary.copy(alpha = 0.4f)
                            )
                            // Writing milestone encouragement
                            val milestone = when {
                                charCount >= 1000 -> "长篇佳作"
                                charCount >= 500 -> "文思泉涌"
                                charCount >= 200 -> "渐入佳境"
                                charCount >= 100 -> "继续加油"
                                else -> null
                            }
                            if (milestone != null) {
                                var milestoneVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(milestone) {
                                    milestoneVisible = true
                                }
                                AnimatedVisibility(
                                    visible = milestoneVisible,
                                    enter = fadeIn(tween(300)) + expandVertically(tween(300))
                                ) {
                                    Text(
                                        text = milestone,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    if (writingDuration > 30) { // Show after 30 seconds
                        Text(
                            text = "已写${viewModel.getFormattedDuration()}",
                            fontSize = 11.sp,
                            color = textSecondary.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Bottom toolbar - redesigned with top action bar + grid
            EditorToolbar(
                showToolbar = showToolbar,
                onToggleToolbar = { showToolbar = !showToolbar; activeCategory = -1 },
                activeCategory = activeCategory,
                onCategoryChange = { cat ->
                    activeCategory = if (activeCategory == cat) -1 else cat
                },
                onFormat = { cmd -> webView?.evaluateJavascript(cmd, null) },
                onHeading = { level -> webView?.evaluateJavascript("setHeading($level)", null) },
                onInsert = { action ->
                    when (action) {
                        "divider" -> webView?.evaluateJavascript("insertDivider()", null)
                    }
                },
                onImageInsert = { imageLauncher.launch("image/*") },
                onClose = {
                    if (hasUnsavedChanges) showUnsavedDialog = true
                    else onNavigateBack()
                }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}

@Composable
private fun MetadataChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val bgColor = if (isActive) primary.copy(alpha = 0.08f) else surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected || isActive) primary else onSurfaceVariant.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    showToolbar: Boolean,
    onToggleToolbar: () -> Unit,
    activeCategory: Int,
    onCategoryChange: (Int) -> Unit,
    onFormat: (String) -> Unit,
    onHeading: (Int) -> Unit,
    onInsert: (String) -> Unit,
    onImageInsert: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary

    // Category definitions
    data class Category(val icon: ImageVector, val label: String, val index: Int)
    val categories = listOf(
        Category(Icons.Default.FormatSize, "格式", 0),
        Category(Icons.Default.FormatListBulleted, "列表", 1),
        Category(Icons.Default.Image, "插入", 2),
        Category(Icons.Default.Palette, "颜色", 3)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
    ) {
        // Category buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                CategoryButton(
                    icon = category.icon,
                    label = category.label,
                    isActive = activeCategory == category.index && showToolbar,
                    onClick = {
                        if (activeCategory == category.index && showToolbar) {
                            onCategoryChange(-1)
                            onToggleToolbar()
                        } else {
                            onCategoryChange(category.index)
                            if (!showToolbar) onToggleToolbar()
                        }
                    },
                    textColor = textColor,
                    activeColor = activeColor
                )
            }
        }

        // Expandable sub-function panel
        AnimatedVisibility(
            visible = showToolbar && activeCategory >= 0,
            enter = expandVertically(tween(200)) + fadeIn(),
            exit = shrinkVertically(tween(150)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor.copy(alpha = 0.95f))
                    .animateContentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(borderColor)
                )

                when (activeCategory) {
                    0 -> FormatSubPanel(onFormat = onFormat, onHeading = onHeading, textColor = textColor, activeColor = activeColor)
                    1 -> ListSubPanel(onFormat = onFormat, textColor = textColor, activeColor = activeColor)
                    2 -> InsertSubPanel(onFormat = onFormat, onInsert = onInsert, onImageInsert = onImageInsert, textColor = textColor, activeColor = activeColor)
                    3 -> ColorSubPanel(onFormat = onFormat, textColor = textColor, activeColor = activeColor)
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    activeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else textColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) activeColor else textColor
        )
    }
}

@Composable
private fun FormatSubPanel(
    onFormat: (String) -> Unit,
    onHeading: (Int) -> Unit,
    textColor: Color,
    activeColor: Color
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: H1 | H2 | H3
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "H1", icon = null, description = "一级标题", onClick = { onHeading(1) }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "H2", icon = null, description = "二级标题", onClick = { onHeading(2) }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "H3", icon = null, description = "三级标题", onClick = { onHeading(3) }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
        }
        // Row 2: Bold | Italic | Underline
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "B", icon = null, description = "加粗", onClick = { onFormat("toggleBold()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f), textStyle = TextStyle(fontWeight = FontWeight.Bold))
            SubFunctionButton(label = "I", icon = null, description = "斜体", onClick = { onFormat("toggleItalic()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f), textStyle = TextStyle(fontStyle = FontStyle.Italic))
            SubFunctionButton(label = "U", icon = null, description = "下划线", onClick = { onFormat("toggleUnderline()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f), textStyle = TextStyle(textDecoration = TextDecoration.Underline))
        }
        // Row 3: Strikethrough | Clear
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "S", icon = null, description = "删除线", onClick = { onFormat("toggleStrike()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f), textStyle = TextStyle(textDecoration = TextDecoration.LineThrough))
            SubFunctionButton(label = "清除", icon = Icons.Default.FormatClear, description = "清除格式", onClick = { onFormat("clearFormatting()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ListSubPanel(
    onFormat: (String) -> Unit,
    textColor: Color,
    activeColor: Color
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Bullet | Ordered | Checkbox
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "无序列表", icon = Icons.Default.FormatListBulleted, onClick = { onFormat("setBulletList()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "有序列表", icon = Icons.Default.FormatListNumbered, onClick = { onFormat("setOrderedList()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "复选框", icon = Icons.Default.CheckBox, onClick = { onFormat("toggleCheckbox()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
        }
        // Row 2: Quote
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "引文", icon = Icons.Default.FormatQuote, onClick = { onFormat("toggleBlockquote()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsertSubPanel(
    onFormat: (String) -> Unit,
    onInsert: (String) -> Unit,
    onImageInsert: () -> Unit,
    textColor: Color,
    activeColor: Color
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Image | Divider | Link
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(label = "图片", icon = Icons.Default.Image, onClick = onImageInsert, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "分割线", icon = Icons.Default.HorizontalRule, onClick = { onInsert("divider") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
            SubFunctionButton(label = "链接", icon = Icons.Default.Link, onClick = { onFormat("insertLink()") }, textColor = textColor, bg = btnBg, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColorSubPanel(
    onFormat: (String) -> Unit,
    textColor: Color,
    activeColor: Color
) {
    val textColors = listOf(
        0xFFE74C3C, 0xFFE67E22, 0xFFF1C40F, 0xFF2ECC71, 0xFF3498DB, 0xFF9B59B6, 0xFF1A1A1A, 0xFFFFFFFF
    )
    val bgColors = listOf(
        0xFFFFF9C4, 0xFFFFE0B2, 0xFFC8E6C9, 0xFFBBDEFB, 0xFFD1C4E9, 0xFFF8BBD0, 0xFFB3E5FC, 0xFFFFF3E0
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Text color section
        Column {
            Text(
                text = "文字颜色",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                textColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(color))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                onFormat("setTextColor('#${Integer.toHexString(color.toInt()).substring(2)}')")
                            }
                    )
                }
            }
        }

        // Background color section
        Column {
            Text(
                text = "背景颜色",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bgColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(color))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                onFormat("setBackgroundColor('#${Integer.toHexString(color.toInt()).substring(2)}')")
                            }
                    )
                }
            }
        }

        // Clear formatting button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onFormat("clearFormatting()") }) {
                Icon(
                    imageVector = Icons.Default.FormatClear,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "清除格式",
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun SubFunctionButton(
    label: String,
    icon: ImageVector? = null,
    description: String = "",
    onClick: () -> Unit,
    textColor: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "subFuncScale"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = textColor,
                style = textStyle
            )
        }
    }
}


@Composable
private fun GridItem(
    label: String,
    description: String,
    onClick: () -> Unit,
    textColor: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "gridItemScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                style = textStyle
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f)
            )
        }
    }
}

private data class ToolbarCategory(val icon: String, val label: String)

private fun getEditorFontSize(prefs: android.content.SharedPreferences): Int {
    return when (prefs.getString("editor_font_size", "medium")) {
        "small" -> 14
        "large" -> 18
        "extra_large" -> 20
        else -> 16
    }
}

private fun escapeForJs(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\u0000", "")
}

private fun countWords(text: String): Int {
    var count = 0
    var inWord = false
    for (ch in text) {
        if (ch.isWhitespace()) {
            inWord = false
        } else if (ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF) {
            count++
            inWord = false
        } else {
            if (!inWord) {
                count++
                inWord = true
            }
        }
    }
    return count
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF6366F1L) }

    val presetColors = listOf(
        0xFF6366F1, 0xFF818CF8, 0xFFA78BFA, 0xFFF472B6,
        0xFFE74C3C, 0xFFF59E0B, 0xFF10B981, 0xFF06B6D4
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_tag)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.select_color), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun TemplateDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (DiaryTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    val templates = remember(selectedCategory) {
        if (selectedCategory == null) TemplateManager.getAllTemplates()
        else TemplateManager.getTemplatesByCategory(selectedCategory!!)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.select_template), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "全部",
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    TemplateManager.getCategories().forEach { category ->
                        CategoryChip(
                            label = templateCategoryLabel(category),
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                // Template list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        TemplateItem(
                            template = template,
                            onClick = { onTemplateSelected(template) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun TemplateItem(
    template: DiaryTemplate,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant.copy(alpha = 0.4f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = iconForTemplate(template.icon),
            contentDescription = template.name,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (template.content.isNotBlank()) {
                Text(
                    text = template.content.take(40) + if (template.content.length > 40) "..." else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            } else {
                Text(
                    text = "空白模板",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun iconForTemplate(iconName: String): ImageVector {
    return when (iconName) {
        "today" -> Icons.Default.Today
        "favorite" -> Icons.Default.Favorite
        "mood" -> Icons.Default.Favorite
        "psychology" -> Icons.Default.Favorite
        "edit" -> Icons.Default.FormatSize
        "menu_book" -> Icons.Default.MenuBook
        "flight" -> Icons.Default.Flight
        "work" -> Icons.Default.Work
        else -> Icons.Default.Today
    }
}

private fun templateCategoryLabel(category: TemplateCategory): String {
    return when (category) {
        TemplateCategory.DAILY -> "日常"
        TemplateCategory.EMOTIONAL -> "情感"
        TemplateCategory.CREATIVE -> "创意"
        TemplateCategory.TRAVEL -> "旅行"
        TemplateCategory.WORK -> "工作"
    }
}
