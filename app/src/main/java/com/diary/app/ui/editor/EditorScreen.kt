package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
    val dateTitle = "${today.year}年${today.monthValue}月${today.dayOfMonth}日"
    val timeText = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))

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
    var showTagDialog by remember { mutableStateOf(false) }

    // Which metadata panel is open: null = none, "mood", "weather", "tags"
    var activePanel by remember { mutableStateOf<String?>(null) }

    // Toolbar state
    var showToolbar by remember { mutableStateOf(true) }
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

    val moodLabels = arrayOf("", "沮丧", "低落", "平静", "开心", "愉快", "兴奋")

    fun getMoodIcon(level: Int?): ImageVector {
        return when (level) {
            1 -> Icons.Default.MoodBad
            2 -> Icons.Default.SentimentDissatisfied
            3 -> Icons.Default.SentimentNeutral
            4 -> Icons.Default.Mood
            5 -> Icons.Default.SentimentSatisfied
            6 -> Icons.Default.SentimentVerySatisfied
            else -> Icons.Default.Mood
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

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
                    Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }

            // Date + time
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(text = dateTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(text = timeText, fontSize = 12.sp, color = textSecondary)
            }

            // Metadata buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mood button
                MetadataButton(
                    label = if (selectedMood != null) moodLabels[selectedMood!!] else "心情",
                    icon = getMoodIcon(selectedMood),
                    isSelected = selectedMood != null,
                    isActive = activePanel == "mood",
                    accentColor = accentColor,
                    surfaceVariant = surfaceVariant,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onClick = { activePanel = if (activePanel == "mood") null else "mood" }
                )
                // Weather button
                MetadataButton(
                    label = selectedWeather ?: "天气",
                    icon = getWeatherIcon(selectedWeather) ?: Icons.Default.Cloud,
                    isSelected = selectedWeather != null,
                    isActive = activePanel == "weather",
                    accentColor = accentColor,
                    surfaceVariant = surfaceVariant,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onClick = { activePanel = if (activePanel == "weather") null else "weather" }
                )
                // Tags button
                MetadataButton(
                    label = if (selectedTagIds.isNotEmpty()) "${selectedTagIds.size} 个标签" else "标签",
                    icon = Icons.Default.Sell,
                    isSelected = selectedTagIds.isNotEmpty(),
                    isActive = activePanel == "tags",
                    accentColor = accentColor,
                    surfaceVariant = surfaceVariant,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onClick = { activePanel = if (activePanel == "tags") null else "tags" }
                )
            }

            // Expandable panels
            AnimatedVisibility(
                visible = activePanel != null,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColor)
                        .animateContentSize()
                        .padding(12.dp)
                ) {
                    when (activePanel) {
                        "mood" -> MoodSlider(
                            selectedLevel = selectedMood,
                            onLevelChange = { selectedMood = it }
                        )
                        "weather" -> WeatherSelector(
                            selectedWeather = selectedWeather,
                            onWeatherSelected = { selectedWeather = it }
                        )
                        "tags" -> TagEditor(
                            allTags = allTags,
                            selectedTagIds = selectedTagIds,
                            onTagToggle = { viewModel.toggleTag(it) },
                            onAddTag = { showTagDialog = true }
                        )
                    }
                }
            }

            // WebView (fills remaining space)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setBackgroundColor(0)
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
                showToolbar = showToolbar,
                onToggleToolbar = { showToolbar = !showToolbar; activeCategory = -1 },
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
private fun MetadataButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isActive: Boolean,
    accentColor: Color,
    surfaceVariant: Color,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val bgColor = if (isActive) accentColor.copy(alpha = 0.12f) else surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected || isActive) accentColor else textSecondary
    val borderColor = if (isActive) accentColor.copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (isActive) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isActive) FontWeight.SemiBold else FontWeight.Normal,
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
    ) {
        // Category row - always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) activeColor.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            if (showToolbar) {
                                onCategoryChange(index)
                            } else {
                                onToggleToolbar()
                                onCategoryChange(index)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat.icon, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isActive) activeColor else textColor)
                    Text(cat.label, fontSize = 11.sp, color = if (isActive) activeColor else textColor)
                }
            }

            // Collapse/expand button
            IconButton(
                onClick = onToggleToolbar,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (showToolbar) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (showToolbar) "收起" else "展开",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Tools panel - expandable
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
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(borderColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatTools(onFormat: (String) -> Unit, textColor: Color, btnBg: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("B", "加粗", "toggleBold()"),
            Triple("I", "斜体", "toggleItalic()"),
            Triple("U", "下划线", "toggleUnderline()"),
            Triple("S", "删除线", "toggleStrike()"),
            Triple("❝", "引用", "toggleBlockquote()"),
            Triple("—", "分割线", "insertDivider()")
        ).forEach { (label, desc, cmd) ->
            ToolChip(label = label, description = desc, onClick = { onFormat(cmd) }, textColor = textColor, bg = btnBg)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeadingTools(onHeading: (Int) -> Unit, textColor: Color, btnBg: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("H1", "大标题", 1),
            Triple("H2", "中标题", 2),
            Triple("H3", "小标题", 3),
            Triple("正文", "默认", 0)
        ).forEach { (label, desc, level) ->
            ToolChip(label = label, description = desc, onClick = { onHeading(level) }, textColor = textColor, bg = btnBg)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListTools(onList: (String) -> Unit, textColor: Color, btnBg: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolChip(label = "1.", description = "有序列表", onClick = { onList("setOrderedList()") }, textColor = textColor, bg = btnBg)
        ToolChip(label = "•", description = "无序列表", onClick = { onList("setBulletList()") }, textColor = textColor, bg = btnBg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsertTools(onInsert: (String) -> Unit, textColor: Color, btnBg: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("图片", "插入图片", "image"),
            Triple("视频", "插入视频", "video"),
            Triple("音频", "插入音频", "audio"),
            Triple("链接", "插入链接", "link")
        ).forEach { (label, desc, action) ->
            ToolChip(label = label, description = desc, onClick = { onInsert(action) }, textColor = textColor, bg = btnBg)
        }
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
private fun ToolChip(label: String, description: String = "", onClick: () -> Unit, textColor: Color, bg: Color) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (description.isNotEmpty()) {
                Text(text = description, fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
            }
        }
    }
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
                Text("选择颜色", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
