package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
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

    // Toolbar state: 0=format, 1=heading, 2=list, 3=insert, 4=color
    var activeCategory by remember { mutableIntStateOf(-1) }
    // Color sub-tab: 0=text, 1=background
    var colorTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) viewModel.loadEntry(diaryId)
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

    // Theme-aware colors
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

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
                TextButton(onClick = {
                    webView?.evaluateJavascript("getContent()") { json ->
                        webView?.evaluateJavascript("getPlainText()") { plain ->
                            val cleanJson = json?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: ""
                            val cleanPlain = plain?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: ""
                            viewModel.saveEntry(dateTitle, cleanJson, cleanPlain, diaryId)
                        }
                    }
                    onNavigateBack()
                }) {
                    Text(
                        text = "保存",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            brush = Brush.horizontalGradient(listOf(DarkAccentStart, DarkAccentEnd))
                        )
                    )
                }
            }

            // Date title
            Text(
                text = dateTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeText, fontSize = 12.sp, color = textSecondary)
                MetadataChip(text = "心情", textColor = textSecondary)
                MetadataChip(text = "天气", textColor = textSecondary)
                MetadataChip(text = "位置", textColor = textSecondary)
            }

            // Tags
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = "添加标签",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColor.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

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

            // Bottom toolbar (above keyboard)
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
                onColorTabChange = { colorTab = it },
                isDark = isDark
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
    onColorTabChange: (Int) -> Unit,
    isDark: Boolean
) {
    val surfaceColor = if (isDark) Color(0xFF1E1E2E) else Color(0xFFF5F5F5)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val textColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
    val activeColor = MaterialTheme.colorScheme.primary
    val btnBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .border(1.dp, borderColor)
    ) {
        // Primary row - category buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val categories = listOf("格式", "标题", "列表", "插入", "颜色")
            categories.forEachIndexed { index, label ->
                val isActive = activeCategory == index
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) activeColor else textColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) activeColor.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onCategoryChange(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Secondary row - tools
        if (activeCategory >= 0) {
            Divider(color = borderColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (activeCategory) {
                    0 -> FormatTools(onFormat, textColor, btnBg)
                    1 -> HeadingTools(onHeading, textColor, btnBg)
                    2 -> ListTools(onList, textColor, btnBg)
                    3 -> InsertTools(onInsert, textColor, btnBg)
                    4 -> ColorTools(onColor, onClearFormat, colorTab, onColorTabChange, textColor, btnBg, activeColor, isDark)
                }
            }
        }
    }
}

@Composable
private fun FormatTools(onFormat: (String) -> Unit, textColor: Color, btnBg: Color) {
    val items = listOf(
        "B" to "toggleBold()",
        "I" to "toggleItalic()",
        "U" to "toggleUnderline()",
        "S" to "toggleStrike()",
        "引用" to "toggleBlockquote()",
        "---" to "insertDivider()"
    )
    items.forEach { (label, cmd) ->
        ToolButton(label = label, onClick = { onFormat(cmd) }, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun HeadingTools(onHeading: (Int) -> Unit, textColor: Color, btnBg: Color) {
    listOf("标题1" to 1, "标题2" to 2, "标题3" to 3, "正文" to 0).forEach { (label, level) ->
        ToolButton(label = label, onClick = { onHeading(level) }, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun ListTools(onList: (String) -> Unit, textColor: Color, btnBg: Color) {
    ToolButton(label = "有序列表", onClick = { onList("setOrderedList()") }, textColor = textColor, bg = btnBg)
    ToolButton(label = "无序列表", onClick = { onList("setBulletList()") }, textColor = textColor, bg = btnBg)
}

@Composable
private fun InsertTools(onInsert: (String) -> Unit, textColor: Color, btnBg: Color) {
    listOf("图片" to "image", "视频" to "video", "音频" to "audio", "链接" to "link").forEach { (label, action) ->
        ToolButton(label = label, onClick = { onInsert(action) }, textColor = textColor, bg = btnBg)
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
    accentColor: Color,
    isDark: Boolean
) {
    Column {
        // Tab row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("文字颜色" to 0, "背景颜色" to 1).forEach { (label, t) ->
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
        // Color palette
        val colors = listOf(
            "#000000" to "黑色", "#FFFFFF" to "白色", "#667EEA" to "蓝色",
            "#764BA2" to "紫色", "#E74C3C" to "红色", "#E67E22" to "橙色",
            "#F1C40F" to "黄色", "#2ECC71" to "绿色", "#9B59B6" to "紫红"
        )
        val type = if (tab == 0) "text" else "background"
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEach { (hex, _) ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
        Spacer(modifier = Modifier.height(4.dp))
        ToolButton(label = "清除格式", onClick = onClear, textColor = textColor, bg = btnBg)
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit, textColor: Color, bg: Color) {
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
private fun MetadataChip(text: String, textColor: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = textColor,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
