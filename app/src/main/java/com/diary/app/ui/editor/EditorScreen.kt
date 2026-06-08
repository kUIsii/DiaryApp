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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.diary.app.ui.todo.TodoViewModel
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
    val todoViewModel: TodoViewModel = viewModel()
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

    // Metadata collapse state
    var isMetadataExpanded by remember { mutableStateOf(false) }

    // Toolbar state - initially hidden, shown when keyboard appears
    var showToolbar by remember { mutableStateOf(true) }
    var activeCategory by remember { mutableIntStateOf(-1) }
    var activeFormats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isFullEditorVisible by remember { mutableStateOf(false) }
    var isToolbarManuallyHidden by remember { mutableStateOf(false) }

    // Detect keyboard visibility and show/hide toolbar
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && !isToolbarManuallyHidden) {
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

    // Title state
    var entryTitle by remember { mutableStateOf("") }

    // Auto-save and unsaved changes
    val autoSaveVisible by viewModel.autoSaveVisible.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    // Dialogs
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }
    var pendingDraft by remember { mutableStateOf<DraftData?>(null) }

    // Writing duration
    val writingDuration by viewModel.writingDuration.collectAsState()

    val applyDraftToEditor: (DraftData, String?) -> Unit = { draft, sourceDraftId ->
        val base64Content = Base64.encodeToString(
            draft.content.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        if (isWebViewReady) {
            webView?.evaluateJavascript("setContentBase64('$base64Content')", null)
        }
        entryTitle = draft.title.takeUnless { it == dateTitle } ?: ""
        selectedMood = draft.moodLevel
        selectedWeather = draft.weather
        selectedLocation = draft.location
        locationLat = draft.latitude
        locationLng = draft.longitude
        viewModel.setSelectedTagIds(draft.tagIds)
        currentDraftId = sourceDraftId
        pendingDraft = null
        showDraftDialog = false
    }

    LaunchedEffect(diaryId) {
        if (diaryId != null) viewModel.loadEntry(diaryId)
        viewModel.startWritingTimer()
    }

    // Load draft when opened with draftId
    LaunchedEffect(draftId, diaryId, isWebViewReady) {
        if (draftId != null && diaryId == null && isWebViewReady) {
            val draft = viewModel.loadDraftById(draftId)
            if (draft != null) {
                applyDraftToEditor(draft, draftId)
            }
        }
    }

    LaunchedEffect(isKeyboardVisible, isFullEditorVisible) {
        if (isKeyboardVisible && !isFullEditorVisible) {
            activePanel = null
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
            // Load existing title
            if (diaryId != null) {
                entryTitle = entry.title
            }
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

    LaunchedEffect(showToolbar, activeCategory, isKeyboardVisible, isFullEditorVisible, isWebViewReady) {
        if (isWebViewReady) {
            val bottomGap = resolveEditorBottomGap(
                showToolbar = showToolbar,
                isKeyboardVisible = isKeyboardVisible,
                isFullEditorVisible = isFullEditorVisible,
                activeCategory = activeCategory
            )
            webView?.evaluateJavascript("setEditorBottomGap($bottomGap)", null)
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
                val saveTitle = entryTitle.ifBlank { dateTitle }
                viewModel.updateLatestContent(cleanJson, latestPlainText, saveTitle)
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
                if (!hasUnsavedChanges) return@LifecycleEventObserver
                webView?.evaluateJavascript("getContent()") { json ->
                    webView?.evaluateJavascript("getPlainText()") { plain ->
                        val cleanJson = unescapeEvaluateJsResult(json)
                        val cleanPlain = unescapeEvaluateJsResult(plain)
                        if (cleanPlain.isNotBlank()) {
                            val saveTitle = entryTitle.ifBlank { dateTitle }
                            viewModel.updateLatestContent(cleanJson, cleanPlain, saveTitle)
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

    // Check for auto draft on new entries and unsaved edits to existing entries.
    LaunchedEffect(diaryId, draftId) {
        if (draftId == null) {
            val draft = viewModel.loadDraft(diaryId)
            if (draft != null && shouldRestoreDraft(diaryId = diaryId, plainText = draft.plainText)) {
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
                "focusEditorWithRestore()",
                null
            )
        }
    }

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val paperColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    }
    val editorBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.22f else 0.46f)
    val metaSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.16f else 0.48f)
    val metaBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.42f)
    val titleTextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = textColor,
        lineHeight = 31.sp,
        fontSize = 24.sp
    )

    fun saveCurrentEntry() {
        webView?.evaluateJavascript("getContent()") { json ->
            webView?.evaluateJavascript("getPlainText()") plainCallback@{ plain ->
                val cleanJson = unescapeEvaluateJsResult(json)
                val cleanPlain = unescapeEvaluateJsResult(plain)
                val saveTitle = entryTitle.ifBlank { dateTitle }
                if (cleanPlain.isBlank() && saveTitle.isBlank()) {
                    onNavigateBack()
                    return@plainCallback
                }
                scope.launch {
                    val savedEntryId = viewModel.saveEntry(
                        title = saveTitle,
                        content = cleanJson,
                        plainText = cleanPlain,
                        diaryId = diaryId,
                        moodLevel = selectedMood,
                        weather = selectedWeather,
                        location = selectedLocation,
                        latitude = locationLat,
                        longitude = locationLng
                    )
                    todoViewModel.autoCompleteHabitsForDiary(
                        diaryTagIds = selectedTagIds.toList(),
                        diaryEntryId = savedEntryId
                    )
                    currentDraftId?.let(viewModel::deleteDraft)
                    currentDraftId = null
                    pendingDraft = null
                    viewModel.onManualSaveCompleted(diaryId)
                    haptic.success()
                    snackbarHostState.showSnackbar(
                        message = "日记已保存",
                        duration = SnackbarDuration.Short
                    )
                    onNavigateBack()
                }
            }
        }
    }

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
                                        currentDraftId = viewModel.saveDraftToList(
                                            cleanJson,
                                            cleanPlain,
                                            entryTitle.ifBlank { dateTitle },
                                            selectedMood,
                                            selectedWeather,
                                            selectedLocation,
                                            locationLat,
                                            locationLng,
                                            currentDraftId
                                        )
                                        viewModel.clearDraft(diaryId)
                                        pendingDraft = null
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
                                currentDraftId?.let(viewModel::deleteDraft)
                                currentDraftId = null
                                pendingDraft = null
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
    val currentPendingDraft = pendingDraft
    if (showDraftDialog && currentPendingDraft != null) {
        AlertDialog(
            onDismissRequest = {
                pendingDraft = null
                showDraftDialog = false
            },
            title = { Text(stringResource(R.string.draft_found)) },
            text = { Text(stringResource(R.string.draft_found_message)) },
            confirmButton = {
                TextButton(onClick = {
                    applyDraftToEditor(currentPendingDraft, null)
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearDraft(diaryId)
                    pendingDraft = null
                    currentDraftId = null
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
                                        applyDraftToEditor(draft, draft.id)
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
                                        color = textSecondary.copy(alpha = 0.6f)
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
                                        tint = textSecondary.copy(alpha = 0.6f),
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
                IconButton(
                    onClick = {
                        if (hasUnsavedChanges) showUnsavedDialog = true
                        else onNavigateBack()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = textSecondary.copy(alpha = 0.82f),
                        modifier = Modifier.size(21.dp)
                    )
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
                EditorTopIconButton(
                    icon = Icons.Default.Undo,
                    contentDescription = stringResource(R.string.undo),
                    onClick = { webView?.evaluateJavascript("quill.history.undo()", null) }
                )
                EditorTopIconButton(
                    icon = Icons.Default.Redo,
                    contentDescription = stringResource(R.string.redo),
                    onClick = { webView?.evaluateJavascript("quill.history.redo()", null) }
                )
                EditorTopIconButton(
                    icon = Icons.Default.Description,
                    contentDescription = "草稿箱",
                    onClick = { showDraftsDialog = true }
                )
                EditorTopIconButton(
                    icon = if (isToolbarManuallyHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isToolbarManuallyHidden) "显示编辑器" else "隐藏编辑器",
                    onClick = {
                        isToolbarManuallyHidden = !isToolbarManuallyHidden
                        showToolbar = !isToolbarManuallyHidden
                    }
                )
                EditorSaveButton(onClick = { saveCurrentEntry() })
            }

            // Date + time (compact single line)
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary.copy(alpha = 0.94f)
                )
                Text(
                    text = timeText,
                    fontSize = 14.sp,
                    color = textSecondary.copy(alpha = 0.82f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = entryTitle,
                    onValueChange = {
                        entryTitle = it
                        viewModel.markContentChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = titleTextStyle,
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (entryTitle.isBlank()) {
                            Text(
                                text = "标题（可选）",
                                style = titleTextStyle.copy(color = textSecondary.copy(alpha = 0.42f))
                            )
                        }
                        innerTextField()
                    }
                )
            }

            AnimatedVisibility(visible = !isKeyboardVisible || isFullEditorVisible || !isMetadataExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isMetadataExpanded) {
                        // Row 1: mood + weather + category
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mood chip
                            val moodColor = selectedMood?.let { moodIconForLevel(it).tint }
                            val currentSelectedMood = selectedMood
                            MetadataChip(
                                label = currentSelectedMood?.let(::moodLabelForLevel) ?: "心情",
                                icon = moodIconForLevel(currentSelectedMood ?: 3).icon,
                                isSelected = currentSelectedMood != null,
                                isActive = activePanel == "mood",
                                onClick = { activePanel = if (activePanel == "mood") null else "mood" },
                                accentColor = moodColor,
                                modifier = Modifier.weight(1f)
                            )
                            // Weather chip
                            val weatherColor = selectedWeather?.let { weatherIconFor(it).tint }
                            MetadataChip(
                                label = selectedWeather ?: "天气",
                                icon = weatherIconFor(selectedWeather).icon,
                                isSelected = selectedWeather != null,
                                isActive = activePanel == "weather",
                                onClick = { activePanel = if (activePanel == "weather") null else "weather" },
                                accentColor = weatherColor,
                                modifier = Modifier.weight(1f)
                            )
                            val rowTagLabel = summarizeSelectedNames(
                                names = allTags.filter { it.id in selectedTagIds }.map { it.name },
                                emptyLabel = "标签"
                            )
                            MetadataChip(
                                label = rowTagLabel,
                                icon = Icons.Default.Sell,
                                isSelected = selectedTagIds.isNotEmpty(),
                                isActive = activePanel == "tags",
                                onClick = { activePanel = if (activePanel == "tags") null else "tags" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Row 2: location (left-aligned)
                        MetadataChip(
                            label = resolveCenteredLocationLabel(selectedLocation),
                            icon = Icons.Default.LocationOn,
                            isSelected = selectedLocation != null,
                            isActive = activePanel == "location",
                            onClick = { activePanel = if (activePanel == "location") null else "location" },
                            centerContent = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            EditorMetaPill(
                                label = "收起",
                                isSelected = false,
                                onClick = {
                                    isMetadataExpanded = false
                                    activePanel = null
                                }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(metaSurfaceColor)
                                .border(0.5.dp, metaBorderColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentSelectedMood = selectedMood
                            EditorMetaPill(
                                label = currentSelectedMood?.let { "心情 ${moodLabelForLevel(it)}" } ?: "心情",
                                isSelected = currentSelectedMood != null,
                                onClick = {
                                    isMetadataExpanded = true
                                    activePanel = "mood"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            EditorMetaPill(
                                label = selectedWeather ?: "天气",
                                isSelected = selectedWeather != null,
                                onClick = {
                                    isMetadataExpanded = true
                                    activePanel = "weather"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            EditorMetaPill(
                                label = if (selectedTagIds.isEmpty()) "标签" else "标签 ${selectedTagIds.size}",
                                isSelected = selectedTagIds.isNotEmpty(),
                                onClick = {
                                    isMetadataExpanded = true
                                    activePanel = "tags"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            EditorMetaPill(
                                label = selectedLocation?.take(4) ?: "位置",
                                isSelected = selectedLocation != null,
                                onClick = {
                                    isMetadataExpanded = true
                                    activePanel = "location"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "展开",
                                tint = textSecondary.copy(alpha = 0.64f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
                                    .clickable { isMetadataExpanded = true }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Expandable panels - simple background
            AnimatedVisibility(
                visible = activePanel != null && (!isKeyboardVisible || isFullEditorVisible),
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(surfaceVariant.copy(alpha = 0.34f))
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .heightIn(min = 46.dp)
                    ) {
                        when (activePanel) {
                            "mood" -> Column {
                                MoodSlider(
                                    selectedLevel = selectedMood,
                                    onLevelChange = {
                                        selectedMood = it
                                        viewModel.markContentChanged()
                                    }
                                )
                                if (selectedMood != null) {
                                    TextButton(
                                        onClick = {
                                            selectedMood = null
                                            viewModel.markContentChanged()
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("清除心情", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            "weather" -> Column {
                                WeatherSelector(
                                    selectedWeather = selectedWeather,
                                    onWeatherSelected = {
                                        selectedWeather = it
                                        viewModel.markContentChanged()
                                    }
                                )
                                if (selectedWeather != null) {
                                    TextButton(
                                        onClick = {
                                            selectedWeather = null
                                            viewModel.markContentChanged()
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("清除天气", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            "tags" -> TagEditor(
                                allTags = allTags,
                                selectedTagIds = selectedTagIds,
                                onTagToggle = {
                                    viewModel.toggleTag(it)
                                    viewModel.markContentChanged()
                                },
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
                                    viewModel.markContentChanged()
                                },
                                recentLocations = recentLocations
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(paperColor)
                    .border(0.5.dp, editorBorderColor, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
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
                    modifier = Modifier.fillMaxSize()
                )
            }

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

            AnimatedVisibility(
                visible = showToolbar,
                enter = slideInVertically(tween(200)) { it } + fadeIn(tween(150)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
            ) {
                Box(modifier = Modifier.imePadding()) {
                    EditorToolbar(
                        showToolbar = showToolbar,
                        activeCategory = activeCategory,
                        isFocusWritingMode = !isFullEditorVisible,
                        onWritingModeChange = { isFocusWritingMode ->
                            isFullEditorVisible = !isFocusWritingMode
                            if (isFocusWritingMode) {
                                activePanel = null
                                activeCategory = -1
                                webView?.requestFocus()
                                webView?.evaluateJavascript("focusEditorWithRestore()", null)
                            }
                        },
                        onCategoryChange = { cat ->
                            if (activeCategory == cat) {
                                activeCategory = -1
                                webView?.requestFocus()
                                webView?.evaluateJavascript("focusEditorWithRestore()", null)
                            } else {
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
                            webView?.evaluateJavascript("focusEditorWithRestore()", null)
                        },
                        onHideToolbar = {
                            isToolbarManuallyHidden = true
                            showToolbar = false
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
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}

@Composable
private fun EditorTopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun EditorMetaPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val contentColor = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val backgroundColor = if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent

    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditorSaveButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "保存",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
