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
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
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

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    diaryId: Long?,
    draftId: String? = null,
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
    val recentLocations by viewModel.recentLocations.collectAsState()

    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var selectedWeather by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showDraftsDialog by remember { mutableStateOf(false) }
    var currentDraftId by remember { mutableStateOf(draftId) }

    // Which metadata panel is open: null = none, "mood", "weather", "tags"
    var activePanel by remember { mutableStateOf<String?>(null) }

    // Toolbar state - initially hidden, shown when keyboard appears
    var showToolbar by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableIntStateOf(-1) }
    var activeFormats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    // Detect keyboard visibility and show/hide toolbar
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            showToolbar = true
            // When keyboard reappears, close any open sub-panel
            if (activeCategory >= 0) {
                activeCategory = -1
            }
        } else if (activeCategory < 0) {
            // Delay hiding to avoid flicker when transitioning from sub-panel to keyboard
            kotlinx.coroutines.delay(200)
            if (activeCategory < 0) {
                showToolbar = false
            }
        }
    }

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

    // Load draft when opened with draftId
    LaunchedEffect(draftId) {
        if (draftId != null && diaryId == null) {
            val draft = viewModel.loadDraftById(draftId)
            if (draft != null) {
                val base64Content = android.util.Base64.encodeToString(
                    draft.content.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
                webView?.evaluateJavascript("setContentBase64('$base64Content')", null)
                selectedMood = draft.moodLevel
                selectedWeather = draft.weather
                selectedLocation = draft.location
                locationLat = draft.latitude
                locationLng = draft.longitude
                currentDraftId = draftId
            }
        }
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

    // Media pickers - save images to local files for reliable display
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    // Scale down if larger than 800px on longest side
                    val maxDim = 800
                    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap
                    // Save to local file instead of Base64 inline
                    val mediaDir = java.io.File(context.filesDir, "diary_media")
                    if (!mediaDir.exists()) mediaDir.mkdirs()
                    val fileName = "img_${System.currentTimeMillis()}.jpg"
                    val outputFile = java.io.File(mediaDir, fileName)
                    java.io.FileOutputStream(outputFile).use { fos ->
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, fos)
                    }
                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()

                    webView?.evaluateJavascript("insertMedia('image', '${escapeForJs("file://${outputFile.absolutePath}")}')", null)
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

    // Collect format state changes from Quill editor with debounce
    LaunchedEffect(Unit) {
        jsBridge.formatChanges.collect { json ->
            try {
                kotlinx.coroutines.delay(150) // Debounce to reduce lag
                val parsed = org.json.JSONObject(json)
                val map = mutableMapOf<String, Any>()
                parsed.keys().forEach { key -> map[key] = parsed.get(key) }
                activeFormats = map
            } catch (_: Exception) {}
        }
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

    // Unsaved changes dialog - redesigned with proper button layout
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes)) },
            text = {
                Column {
                    Text(stringResource(R.string.unsaved_changes_message))
                    Spacer(modifier = Modifier.height(20.dp))
                    // Three buttons stacked vertically with consistent styling
                    // 1. Continue editing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showUnsavedDialog = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.continue_editing), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 2. Save as draft
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                webView?.evaluateJavascript("getContent()") { json ->
                                    webView?.evaluateJavascript("getPlainText()") { plain ->
                                        val cleanJson = unescapeEvaluateJsResult(json)
                                        val cleanPlain = unescapeEvaluateJsResult(plain)
                                        viewModel.saveDraftToList(cleanJson, cleanPlain, dateTitle, selectedMood, selectedWeather, selectedLocation, locationLat, locationLng, currentDraftId)
                                        showUnsavedDialog = false
                                        onNavigateBack()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.save_as_draft), fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 3. Exit without saving
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                viewModel.clearDraft(diaryId)
                                showUnsavedDialog = false
                                onNavigateBack()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.exit_without_saving), fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
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

    // Drafts dialog
    if (showDraftsDialog) {
        val drafts = remember(showDraftsDialog) { viewModel.getAllDrafts() }
        AlertDialog(
            onDismissRequest = { showDraftsDialog = false },
            title = { Text("草稿箱", fontWeight = FontWeight.Bold) },
            text = {
                if (drafts.isEmpty()) {
                    Text("暂无草稿", color = textSecondary, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drafts.forEach { draft ->
                            val previewText = draft.plainText.take(50).ifBlank { draft.title.take(20) }
                            val timeAgo = getTimeAgo(draft.timestamp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        showDraftsDialog = false
                                        // Load the draft directly into current editor
                                        val base64Content = android.util.Base64.encodeToString(
                                            draft.content.toByteArray(Charsets.UTF_8),
                                            android.util.Base64.NO_WRAP
                                        )
                                        webView?.evaluateJavascript("setContentBase64('$base64Content')", null)
                                        selectedMood = draft.moodLevel
                                        selectedWeather = draft.weather
                                        selectedLocation = draft.location
                                        locationLat = draft.latitude
                                        locationLng = draft.longitude
                                        currentDraftId = draft.id
                                        viewModel.setSelectedTagIds(draft.tagIds)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draft.title.ifBlank { "无标题" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    if (previewText.isNotBlank()) {
                                        Text(
                                            text = previewText,
                                            fontSize = 12.sp,
                                            color = textSecondary.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = timeAgo,
                                        fontSize = 11.sp,
                                        color = textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteDraft(draft.id)
                                        showDraftsDialog = false
                                        showDraftsDialog = true // Refresh
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = textSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDraftsDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    BackHandler {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                IconButton(onClick = { showDraftsDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Description, contentDescription = "草稿箱", tint = textSecondary, modifier = Modifier.size(20.dp))
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
                Text(text = dateTitle, style = MaterialTheme.typography.headlineSmall, color = textColor)
                Text(text = timeText, fontSize = 11.sp, color = textSecondary)
            }

            // Writing prompt (only for new entries, hide after 50 chars)
            if (diaryId == null && writingPrompt.isNotBlank() && charCount < 50) {
                var promptVisible by remember { mutableStateOf(false) }
                LaunchedEffect(writingPrompt) {
                    promptVisible = false
                    kotlinx.coroutines.delay(100)
                    promptVisible = true
                }
                AnimatedVisibility(
                    visible = promptVisible,
                    enter = fadeIn(tween(400))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.refreshPrompt() }
                            .background(textSecondary.copy(alpha = 0.04f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                        Text(
                            text = writingPrompt,
                            fontSize = 12.sp,
                            color = textSecondary.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Metadata buttons - two rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: mood + weather + tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mood chip
                    val moodColor = selectedMood?.let { moodIconForLevel(it).tint }
                    MetadataChip(
                        label = if (selectedMood != null) moodLabelForLevel(selectedMood!!) else "心情",
                        icon = moodIconForLevel(selectedMood ?: 3).icon,
                        isSelected = selectedMood != null,
                        isActive = activePanel == "mood",
                        onClick = { activePanel = if (activePanel == "mood") null else "mood" },
                        accentColor = moodColor
                    )
                    // Weather chip
                    val weatherColor = selectedWeather?.let { weatherIconFor(it).tint }
                    MetadataChip(
                        label = selectedWeather ?: "天气",
                        icon = weatherIconFor(selectedWeather).icon,
                        isSelected = selectedWeather != null,
                        isActive = activePanel == "weather",
                        onClick = { activePanel = if (activePanel == "weather") null else "weather" },
                        accentColor = weatherColor
                    )
                    // Tags chip - show full tag names
                    val tagLabel = if (selectedTagIds.isNotEmpty()) {
                        val names = allTags.filter { it.id in selectedTagIds }.map { it.name }
                        names.joinToString(" ")
                    } else "标签"
                    MetadataChip(
                        label = tagLabel,
                        icon = Icons.Default.Sell,
                        isSelected = selectedTagIds.isNotEmpty(),
                        isActive = activePanel == "tags",
                        onClick = { activePanel = if (activePanel == "tags") null else "tags" }
                    )
                }
                // Row 2: location (auto-wrap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataChip(
                        label = selectedLocation ?: "位置",
                        icon = Icons.Default.LocationOn,
                        isSelected = selectedLocation != null,
                        isActive = activePanel == "location",
                        onClick = { activePanel = if (activePanel == "location") null else "location" }
                    )
                }
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
                                },
                                recentLocations = recentLocations
                            )
                        }
                    }
                }
            }

            // WebView (fills remaining space)
            val mediaDir = java.io.File(context.filesDir, "diary_media")
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebViewReady = true
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
                                            return android.webkit.WebResourceResponse(
                                                mime, null, file.inputStream()
                                            )
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
                        setBackgroundColor(0)
                        addJavascriptInterface(jsBridge, "DiaryBridge")
                        loadUrl("file:///android_asset/editor.html")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Word count and writing duration - integrated into space above toolbar
            if (charCount > 0 || writingDuration > 30) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
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
                            val milestone = when {
                                charCount >= 1000 -> "长篇佳作"
                                charCount >= 500 -> "文思泉涌"
                                charCount >= 200 -> "渐入佳境"
                                charCount >= 100 -> "继续加油"
                                else -> null
                            }
                            if (milestone != null) {
                                Text(
                                    text = milestone,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    if (writingDuration > 30) {
                        Text(
                            text = "已写${viewModel.getFormattedDuration()}",
                            fontSize = 11.sp,
                            color = textSecondary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
        // Floating toolbar overlay - hidden by default, shown when keyboard appears
        AnimatedVisibility(
            visible = showToolbar,
            enter = slideInVertically(tween(200)) { it } + fadeIn(tween(150)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.imePadding()) {
                EditorToolbar(
                    showToolbar = showToolbar,
                    activeCategory = activeCategory,
                    onCategoryChange = { cat ->
                        if (activeCategory == cat) {
                            // Same category clicked - close it and show keyboard
                            activeCategory = -1
                            webView?.requestFocus()
                            webView?.evaluateJavascript("focusEditor()", null)
                        } else {
                            // New category - open it and hide keyboard
                            activeCategory = cat
                            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.hideSoftInputFromWindow((context as android.app.Activity).currentFocus?.windowToken, 0)
                        }
                    },
                activeFormats = activeFormats,
                onFormat = { cmd -> webView?.evaluateJavascript(cmd, null) },
                onHeading = { level -> webView?.evaluateJavascript("setHeading($level)", null) },
                onInsert = { action ->
                    when (action) {
                        "divider" -> webView?.evaluateJavascript("insertDivider()", null)
                    }
                },
                onImageInsert = { imageLauncher.launch("image/*") },
                onHideKeyboard = {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow((context as android.app.Activity).currentFocus?.windowToken, 0)
                },
                onShowKeyboard = {
                    webView?.requestFocus()
                    webView?.evaluateJavascript("focusEditor()", null)
                },
                onClose = {
                    if (hasUnsavedChanges) showUnsavedDialog = true
                    else onNavigateBack()
                },
                fontSize = editorFontSize,
                onFontSizeChange = { newSize ->
                    editorFontSize = newSize
                    prefs.edit().putString("editor_font_size", when(newSize) {
                        10 -> "tiny"
                        14 -> "small"
                        16 -> "medium"
                        18 -> "large"
                        20 -> "extra_large"
                        else -> "small"
                    }).apply()
                    webView?.evaluateJavascript("setFontSize($newSize)", null)
                }
            )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}
