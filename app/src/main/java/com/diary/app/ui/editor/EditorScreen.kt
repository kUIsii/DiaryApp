package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DarkTextPrimary
import com.diary.app.ui.theme.DarkTextSecondary
import com.diary.app.ui.theme.DarkTextTertiary
import com.diary.app.ui.theme.isDark
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    var webView by remember { mutableStateOf<WebView?>(null) }
    val jsBridge = remember { DiaryJsBridge() }

    val viewModel: EditorViewModel = viewModel()

    val isDark = themeMode.isDark()

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            viewModel.loadEntry(diaryId)
        }
    }

    // Inject theme when theme changes
    LaunchedEffect(themeMode) {
        val wv = webView ?: return@LaunchedEffect
        val themeStr = if (isDark) "dark" else "light"
        wv.evaluateJavascript("setTheme('$themeStr')", null)
    }

    // Media pickers
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val wv = webView ?: return@let
            wv.evaluateJavascript("insertMedia('image', '$it')", null)
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val wv = webView ?: return@let
            wv.evaluateJavascript("insertMedia('video', '$it')", null)
        }
    }
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val wv = webView ?: return@let
            wv.evaluateJavascript("insertMedia('audio', '$it')", null)
        }
    }

    // Collect JS bridge events
    LaunchedEffect(Unit) {
        jsBridge.events.collect { event ->
            when (event) {
                "image" -> imageLauncher.launch("image/*")
                "video" -> videoLauncher.launch("video/*")
                "audio" -> audioLauncher.launch("audio/*")
            }
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = DarkTextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { webView?.evaluateJavascript("quill.undo()", null) }) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "撤销",
                        tint = DarkTextTertiary
                    )
                }

                IconButton(onClick = { webView?.evaluateJavascript("quill.redo()", null) }) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "重做",
                        tint = DarkTextTertiary
                    )
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
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            brush = Brush.horizontalGradient(
                                colors = listOf(DarkAccentStart, DarkAccentEnd)
                            )
                        )
                    )
                }
            }

            // Date title
            Text(
                text = dateTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Metadata row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = DarkTextTertiary
                )

                MetadataChip(text = "心情")
                MetadataChip(text = "天气")
                MetadataChip(text = "位置")
            }

            // Tags
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "添加标签",
                    fontSize = 12.sp,
                    color = DarkTextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // WebView editor
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

                        // Inject theme after page loads
                        post {
                            val themeStr = if (isDark) "dark" else "light"
                            evaluateJavascript("setTheme('$themeStr')", null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = DarkTextSecondary,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
