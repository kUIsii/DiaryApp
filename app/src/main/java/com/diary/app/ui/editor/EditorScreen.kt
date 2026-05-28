package com.diary.app.ui.editor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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

    var editorContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 撤销 */ }) {
                        Icon(Icons.Default.Undo, contentDescription = "撤销")
                    }
                    IconButton(onClick = { /* 重做 */ }) {
                        Icon(Icons.Default.Redo, contentDescription = "重做")
                    }
                    TextButton(onClick = {
                        // 保存日记
                        onNavigateBack()
                    }) {
                        Text(
                            text = "保存",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 日期标题
            Text(
                text = dateTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 时间和标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = timeText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 心情标签
                TextButton(
                    onClick = { /* 选择心情 */ },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Text("😊 心情", fontSize = 12.sp)
                }
                // 天气标签
                TextButton(
                    onClick = { /* 选择天气 */ },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Text("🌤 天气", fontSize = 12.sp)
                }
                // 位置标签
                TextButton(
                    onClick = { /* 选择位置 */ },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(16.dp)
                    )
                ) {
                    Text("📍 位置", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 添加标签按钮
            TextButton(
                onClick = { /* 添加标签 */ },
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
            ) {
                Text("🏷 添加标签", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 富文本编辑器 (WebView)
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // 加载富文本编辑器HTML
                        loadDataWithBaseURL(
                            null,
                            getEditorHtml(),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            )

            // 底部工具栏
            EditorToolbar()
        }
    }
}

@Composable
fun EditorToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 字体样式按钮
        IconButton(onClick = { /* 字体样式 */ }) {
            Text("Aa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // 调色板按钮
        IconButton(onClick = { /* 颜色选择 */ }) {
            Text("🎨", fontSize = 20.sp)
        }

        // 列表按钮
        IconButton(onClick = { /* 列表 */ }) {
            Text("≡", fontSize = 24.sp)
        }

        // 图片按钮
        IconButton(onClick = { /* 插入图片 */ }) {
            Text("🖼", fontSize = 20.sp)
        }

        // 分割线按钮
        IconButton(onClick = { /* 插入分割线 */ }) {
            Text("—", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 关闭按钮
        IconButton(onClick = { /* 关闭工具栏 */ }) {
            Icon(Icons.Default.Close, contentDescription = "关闭")
        }
    }
}

private fun getEditorHtml(): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                padding: 16px;
                color: #E0E0E0;
                background-color: #1E1E1E;
                line-height: 1.6;
                min-height: 100vh;
            }
            [contenteditable] {
                outline: none;
                min-height: 200px;
            }
            h1 { font-size: 24px; margin: 16px 0 8px 0; }
            h2 { font-size: 20px; margin: 12px 0 6px 0; }
            h3 { font-size: 18px; margin: 10px 0 4px 0; }
            blockquote {
                border-left: 3px solid #90CAF9;
                padding-left: 12px;
                margin: 8px 0;
                color: #BDBDBD;
            }
            ul, ol {
                margin-left: 20px;
                margin-bottom: 8px;
            }
            hr {
                border: none;
                border-top: 1px solid #424242;
                margin: 16px 0;
            }
            img {
                max-width: 100%;
                border-radius: 8px;
                margin: 8px 0;
            }
        </style>
    </head>
    <body>
        <div contenteditable="true" id="editor" placeholder="开始写日记...">
        </div>
        <script>
            // 撤销/重做功能
            function undo() { document.execCommand('undo'); }
            function redo() { document.execCommand('redo'); }

            // 字体样式
            function bold() { document.execCommand('bold'); }
            function italic() { document.execCommand('italic'); }
            function underline() { document.execCommand('underline'); }
            function strikethrough() { document.execCommand('strikethrough'); }

            // 标题
            function heading(level) {
                document.execCommand('formatBlock', false, 'h' + level);
            }

            // 列表
            function unorderedList() { document.execCommand('insertUnorderedList'); }
            function orderedList() { document.execCommand('insertOrderedList'); }

            // 引用
            function blockquote() {
                document.execCommand('formatBlock', false, 'blockquote');
            }

            // 分割线
            function insertHR() {
                document.execCommand('insertHTML', false, '<hr><p><br></p>');
            }

            // 插入图片
            function insertImage(url) {
                document.execCommand('insertHTML', false, '<img src="' + url + '"><p><br></p>');
            }

            // 设置文字颜色
            function setTextColor(color) {
                document.execCommand('foreColor', false, color);
            }

            // 设置背景色
            function setBackgroundColor(color) {
                document.execCommand('hiliteColor', false, color);
            }

            // 获取内容
            function getContent() {
                return document.getElementById('editor').innerHTML;
            }

            // 设置内容
            function setContent(html) {
                document.getElementById('editor').innerHTML = html;
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
