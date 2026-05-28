package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.isDark
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    diaryId: Long?,
    onNavigateBack: () -> Unit
) {
    val today = LocalDate.now()
    val currentTime = LocalTime.now()
    val dateTitle = "${today.year}年${today.monthValue}月${today.dayOfMonth}日的日记"
    val timeText = "今天 ${currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"

    val context = LocalContext.current
    val app = context.applicationContext as DiaryApplication
    val themeMode by app.themeMode.collectAsState()
    val isDark = themeMode.isDark()

    var webView by remember { mutableStateOf<WebView?>(null) }
    val jsBridge = remember { DiaryJsBridge() }
    val viewModel: EditorViewModel = viewModel()

    val allTags by viewModel.allTags.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()

    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var selectedWeather by remember { mutableStateOf<String?>(null) }
    var showMetadata by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Toolbar state: 0=format, 1=heading, 2=list, 3=insert, 4=color
    var activeCategory by remember { mutableIntStateOf(-1) }
    var colorTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) viewModel.loadEntry(diaryId)
    }

    LaunchedEffect(viewModel.currentEntry.value) {
        viewModel.currentEntry.value?.let { entry ->
            selectedMood = entry.moodLevel
            selectedWeather = entry.weather
        }
    }

    LaunchedEffect(themeMode) {
        webView?.evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
    }

    // Media pickers
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { webView?.evaluateJavascript("insertMedia('image', '$it')", null) }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { webView?.evaluateJavascript("insertMedia('video', '$it')", null) }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { webView?.evaluateJavascript("insertMedia('audio', '$it')", null) }
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

    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    if (showTagDialog) {
        AddTagDialog(
            onDismiss = { showTagDialog = false },
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showTagDialog = false
            }
        )
    }

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
                IconButton(onClick = { webView?.evaluateJavascript("quill.undo()", null) }) {
                    Icon(Icons.Default.Undo, contentDescription = "撤销", tint = textSecondary)
                }
                IconButton(onClick = { webView?.evaluateJavascript("quill.redo()", null) }) {
                    Icon(Icons.Default.Redo, contentDescription = "重做", tint = textSecondary)
                }
                IconButton(onClick = {
                    webView?.evaluateJavascript("getContent()") { json ->
                        webView?.evaluateJavascript("getPlainText()") { plain ->
                            val cleanJson = json?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: ""
                            val cleanPlain = plain?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: ""
                            viewModel.saveEntry(
                                title = dateTitle,
                                content = cleanJson,
                                plainText = cleanPlain,
                                diaryId = diaryId,
                                moodLevel = selectedMood,
                                weather = selectedWeather
                            )
                        }
                    }
                    onNavigateBack()
                }) {
                    Text(
                        text = "保存",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Date title + metadata toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMetadata = !showMetadata }
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (showMetadata) "收起" else "详情",
                    fontSize = 12.sp,
                    color = accentColor
                )
            }

            // Time
            Text(
                text = timeText,
                fontSize = 12.sp,
                color = textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Expandable metadata section
            AnimatedVisibility(
                visible = showMetadata,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColor.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    // Mood
                    Text(text = "心情", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    MoodSlider(
                        selectedLevel = selectedMood,
                        onLevelChange = { selectedMood = it },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weather
                    Text(text = "天气", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    WeatherSelector(
                        selectedWeather = selectedWeather,
                        onWeatherSelected = { selectedWeather = it },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "标签", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text(
                            text = "+ 新建",
                            fontSize = 12.sp,
                            color = accentColor,
                            modifier = Modifier.clickable { showTagDialog = true }
                        )
                    }
                    TagEditor(
                        allTags = allTags,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = { viewModel.toggleTag(it) },
                        onAddTag = { showTagDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Divider(color = dividerColor, thickness = 0.5.dp)

            // WebView (fills remaining space)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        addJavascriptInterface(jsBridge, "DiaryBridge")
                        loadUrl("file:///android_asset/editor.html")
                        webView = this
                        post {
                            evaluateJavascript("setTheme('${if (isDark) "dark" else "light"}')", null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Bottom toolbar
            EditorToolbar(
                activeCategory = activeCategory,
                onCategoryChange = { cat ->
                    activeCategory = if (activeCategory == cat) -1 else cat
                },
                onFormat = { cmd -> webView?.evaluateJavascript(cmd, null) },
                onHeading = { level -> webView?.evaluateJavascript("setHeading($level)", null) },
                onList = { cmd -> webView?.evaluateJavascript(cmd, null) },
                onInsert = { action ->
                    when (action) {
                        "image" -> imageLauncher.launch("image/*")
                        "video" -> videoLauncher.launch("video/*")
                        "audio" -> audioLauncher.launch("audio/*")
                        "link" -> webView?.evaluateJavascript("insertLink()", null)
                    }
                },
                onColor = { color, type ->
                    if (type == "text") webView?.evaluateJavascript("setTextColor('$color')", null)
                    else webView?.evaluateJavascript("setBackgroundColor('$color')", null)
                },
                onClearFormat = { webView?.evaluateJavascript("clearFormatting()", null) },
                colorTab = colorTab,
                onColorTabChange = { colorTab = it }
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    activeCategory: Int,
    onCategoryChange: (Int) -> Unit,
    onFormat: (String) -> Unit,
    onHeading: (Int) -> Unit,
    onList: (String) -> Unit,
    onInsert: (String) -> Unit,
    onColor: (String, String) -> Unit,
    onClearFormat: () -> Unit,
    colorTab: Int,
    onColorTabChange: (Int) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .border(0.5.dp, borderColor)
    ) {
        // Primary row - 5 icon categories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val categories = listOf(
                ToolbarCategory("Aa", "格式"),
                ToolbarCategory("H", "标题"),
                ToolbarCategory("≡", "列表"),
                ToolbarCategory("▢", "插入"),
                ToolbarCategory("◉", "颜色")
            )
            categories.forEachIndexed { index, cat ->
                val isActive = activeCategory == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) activeColor.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onCategoryChange(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.icon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) activeColor else textColor
                    )
                    Text(
                        text = cat.label,
                        fontSize = 10.sp,
                        color = if (isActive) activeColor else textColor
                    )
                }
            }
        }

        // Secondary row - tools
        AnimatedVisibility(
            visible = activeCategory >= 0,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Divider(color = borderColor, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (activeCategory) {
                        0 -> FormatTools(onFormat, textColor, btnBg)
                        1 -> HeadingTools(onHeading, textColor, btnBg)
                        2 -> ListTools(onList, textColor, btnBg)
                        3 -> InsertTools(onInsert, textColor, btnBg)
                        4 -> ColorTools(onColor, onClearFormat, colorTab, onColorTabChange, textColor, btnBg, activeColor)
                    }
                }
            }
        }
    }
}

private data class ToolbarCategory(val icon: String, val label: String)

@Composable
private fun FormatTools(onFormat: (String) -> Unit, textColor: Color, btnBg: Color) {
    val items = listOf(
        "B" to "toggleBold()",
        "I" to "toggleItalic()",
        "U" to "toggleUnderline()",
        "̶" to "toggleStrike()",
        "❝" to "toggleBlockquote()",
        "—" to "insertDivider()"
    )
    items.forEach { (label, cmd) ->
        ToolChip(label = label, onClick = { onFormat(cmd) }, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun HeadingTools(onHeading: (Int) -> Unit, textColor: Color, btnBg: Color) {
    listOf("H1" to 1, "H2" to 2, "H3" to 3, "正文" to 0).forEach { (label, level) ->
        ToolChip(label = label, onClick = { onHeading(level) }, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun ListTools(onList: (String) -> Unit, textColor: Color, btnBg: Color) {
    ToolChip(label = "1. …", onClick = { onList("setOrderedList()") }, textColor = textColor, bg = btnBg)
    ToolChip(label = "• …", onClick = { onList("setBulletList()") }, textColor = textColor, bg = btnBg)
}

@Composable
private fun InsertTools(onInsert: (String) -> Unit, textColor: Color, btnBg: Color) {
    listOf(
        "▣ 图片" to "image",
        "▷ 视频" to "video",
        "▶ 音频" to "audio",
        "‖ 链接" to "link"
    ).forEach { (label, action) ->
        ToolChip(label = label, onClick = { onInsert(action) }, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun ColorTools(
    onColor: (String, String) -> Unit,
    onClear: () -> Unit,
    tab: Int,
    onTabChange: (Int) -> Unit,
    textColor: Color,
    btnBg: Color,
    accentColor: Color
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("文字" to 0, "背景" to 1).forEach { (label, t) ->
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (tab == t) accentColor else textColor,
                    fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onTabChange(t) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val colors = listOf(
            "#000000", "#FFFFFF", "#667EEA", "#764BA2", "#E74C3C",
            "#E67E22", "#F1C40F", "#2ECC71", "#9B59B6"
        )
        val type = if (tab == 0) "text" else "background"
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .then(
                            if (hex == "#FFFFFF" || hex == "#F1C40F")
                                Modifier.border(1.dp, Color.Gray, CircleShape)
                            else Modifier
                        )
                        .clickable { onColor(hex, type) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        ToolChip(label = "清除格式", onClick = onClear, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun ToolChip(label: String, onClick: () -> Unit, textColor: Color, bg: Color) {
    Text(
        text = label,
        fontSize = 13.sp,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF667EEAL) }

    val presetColors = listOf(
        0xFF667EEA, 0xFF764BA2, 0xFFE74C3C, 0xFFE67E22,
        0xFFF1C40F, 0xFF2ECC71, 0xFF9B59B6, 0xFF1ABC9C
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建标签") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "选择颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
