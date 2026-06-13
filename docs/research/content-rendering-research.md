# 内容渲染引擎调研

> 日期：2026-06-10
> 背景：DiaryApp 目前将 Quill.js Delta JSON 存储在 Room DB 中。查看器（viewer.html）通过自定义 `deltaToHtml()` 函数将 Delta 转换为语义 HTML，并在 WebView 中渲染。本文档评估其他渲染方案。

---

## 当前架构

```
Editor (Quill.js in WebView) --> Delta JSON --> Room DB (content column)
                                                      |
Viewer (viewer.html in WebView) <-- Base64 <-- deltaToHtml()
```

关键事实：
- `DiaryEntry.content` 存储原始 Delta JSON 字符串（schema 中无大小限制）
- `DiaryEntry.plainText` 存储纯文本用于搜索/预览
- 查看器有 2MB 内容限制；超出时回退到 plainText
- `deltaToHtml()` 处理：文本格式、图片、视频、分割线、引用块、列表（有序/无序/已勾选/未勾选）、缩进、对齐、代码块
- 暗色模式通过 CSS 变量 + `setTheme()` JS 桥接实现
- 媒体文件通过 `shouldInterceptRequest` 提供服务（file:// URL 指向 diary_media 路径）
- 已知问题：传给查看器前必须从 Delta 中剥离 Base64 data URL，否则会 OOM

---

## 1. 保留 Quill Delta + WebView（当前方案）

**工作原理：** 存储 Delta JSON，在 viewer.html 中转换为 HTML，在 Android WebView 中渲染。

**优点：**
- 零迁移成本 -- 一切已经正常运行
- 完全的 HTML/CSS 样式控制（暗色模式、自定义字体、图片阴影）
- 已实现的丰富格式：标题、粗体/斜体/下划线/删除线、颜色、列表、代码块、引用块、分割线、对齐、缩进、图片、视频
- Base64 回退处理损坏的 JSON（正则提取文本）
- WebView 原生支持文本选择、复制、缩放

**缺点：**
- WebView 内存开销大（每个实例约 30-50MB）
- WebView 冷启动延迟（加载 viewer.html + 注入内容约 200-500ms）
- `deltaToHtml()` 是自定义 JS -- 任何新格式都必须手动维护
- Delta JSON 可能很大（尤其是嵌入 Base64 图片时）；2MB 保护机制存在但只是权宜之计
- 无原生文本选择/分享集成（WebView 的选择与 Android 系统的分离）
- WebView 滚动可能与 Compose 中的父级 LazyColumn/ScrollState 冲突
- 暗色模式需要 JS 桥接调用（`setTheme`），主题切换不是即时的

**代码路径：**
```kotlin
// DiaryDetailScreen.kt
WebView(ctx).apply {
    loadUrl("file:///android_asset/viewer.html")
    // onPageFinished:
    evaluateJavascript("setContentFromBase64('$encoded')", null)
    evaluateJavascript("setTheme('dark')", null)
    evaluateJavascript("setFontSize($fontSizePx)", null)
}
```

**性能：** 阅读体验良好（静态内容）。冷启动约 300ms。内存：每个 WebView 实例 30-50MB。

**建议：** 作为只读查看器足够。真正的痛点在编辑器（IME 问题），而非查看器。

---

## 2. Markdown 渲染（Markwon）

**工作原理：** 将内容存储为 Markdown 字符串。使用 [Markwon](https://noties.io/Markwon/) 库原生渲染，该库将 Markdown 解析为 Android Spanned 文本。

**迁移路径：** 编写 Delta-to-Markdown 转换器（或将 `deltaToHtml()` 修改为输出 Markdown，然后存储 Markdown）。

**优点：**
- 完全原生渲染 -- 无 WebView 开销
- Markwon 支持：标题、粗体/斜体/删除线、链接、图片、列表、代码块、表格、任务列表、LaTeX（通过插件）、语法高亮（通过插件）
- 库体积约 1MB，冷启动快（<50ms）
- 原生文本选择和分享
- Markdown 可读性强且广泛支持
- 易于导出（纯文本）

**缺点：**
- Delta 到 Markdown 的转换是有损的（自定义颜色、背景色、视频嵌入、缩进级别在 Markdown 中没有等价语法）
- Markwon 渲染到 `TextView` + `Spannable` -- 布局控制不如 HTML/CSS 灵活
- 图片处理需要自定义 `ImageSpan` 实现来支持本地文件
- 视频播放需要单独的原生实现（ExoPlayer）
- 暗色模式需要重新渲染或自定义 span 主题
- Markdown 规范存在歧义（不同解析器处理边界情况的方式不同）

**代码示例：**
```kotlin
val markwon = Markwon.builder(context)
    .usePlugin(StrikethroughPlugin.create())
    .usePlugin(TablePlugin.create(context))
    .usePlugin(TaskListPlugin.create(context))
    .usePlugin(HtmlPlugin.create())
    .usePlugin(GlideImagesPlugin.create(context))
    .build()

// In Compose:
AndroidView(factory = { ctx ->
    TextView(ctx).apply {
        markwon.setMarkdown(this, markdownContent)
    }
})
```

**性能：** 优秀。原生渲染，冷启动 <50ms，内存约 5MB。

**建议：** 如果愿意接受格式损失并重写编辑器，这是不错的选择。不建议作为直接替换 -- Delta-to-Markdown 迁移工作量大且有损。

---

## 3. HTML 渲染（WebView 或 Html.fromHtml）

**工作原理：** 将内容存储为 HTML 字符串。在 WebView 中渲染（完整 CSS）或使用 `android.text.Html.fromHtml()` 渲染（功能有限）。

### 3a. WebView + HTML

**优点：**
- 与当前方案相同的渲染引擎 -- 可复用所有 CSS 样式
- `deltaToHtml()` 已存在；只需存储其输出而非 Delta
- 完全的 CSS 控制（暗色模式、自定义布局、嵌入式媒体）

**缺点：**
- 与当前方案相同的 WebView 开销（约 30-50MB，冷启动约 300ms）
- 存储在 DB 中的 HTML 比 Delta JSON 更大（冗余的标记）
- 编辑需要将 HTML 转回 Delta 或使用不同的编辑器
- 如未正确清理存在 HTML 注入风险

### 3b. Html.fromHtml()（原生）

**优点：**
- 原生 `TextView` 渲染 -- 无 WebView
- 支持基础标签：`<b>`、`<i>`、`<u>`、`<p>`、`<br>`、`<ul>`、`<ol>`、`<li>`、`<a>`、`<img>`、`<h1>`-`<h6>`、`<blockquote>`、`<font>`

**缺点：**
- 标签支持非常有限（无视频、无代码块、无自定义 CSS）
- `Html.fromHtml()` 在不同 Android 版本上存在 bug（间距、列表渲染）
- 不支持暗色模式 CSS 变量 -- 需要自定义 `TagHandler`
- 图片需要自定义 `ImageGetter` 实现
- 无法处理复杂布局（表格、flex）

**代码示例：**
```kotlin
// Native approach
val spanned = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT, imageGetter, tagHandler)
Text(text = AnnotatedString(spanned.toAnnotatedString()))
```

**性能：** 3a 与当前方案相同。3b 速度快但功能有限。

**建议：** 3a 是横向迁移（存储 HTML 而非 Delta，渲染效果相同）。3b 对于富文本日记应用来说过于受限。

---

## 4. Jetpack Compose 原生渲染

**工作原理：** 将 Delta JSON（或 HTML）解析为 Compose 组件树：`Text`、`Image`、`Video`、`Divider` 等。

**优点：**
- 完全原生，与 Compose 生命周期集成
- 无 WebView 开销
- 原生暗色模式，通过 `MaterialTheme.colorScheme`
- 原生文本选择、分享、无障碍支持
- 流畅的动画和过渡
- 直接集成 ExoPlayer 播放视频

**缺点：**
- 必须编写完整的 Delta-to-Compose 解析器（工作量很大）
- `Text` 组件配合 `AnnotatedString` 支持：粗体、斜体、下划线、删除线、颜色、字号、点击事件 -- 但不支持：span 背景色、上标/下标
- 图片加载需要 Coil/Glide 集成
- 视频播放需要 ExoPlayer + `AndroidView`
- 代码块需要自定义等宽字体样式
- 无法实现任意 CSS 效果（图片 box-shadow、复杂引用块边框）
- 每个"块"是一个独立组件 -- 对于长文本效率不如单个 `TextView`

**代码示例：**
```kotlin
@Composable
fun DeltaContent(delta: Delta) {
    Column {
        delta.blocks.forEach { block ->
            when (block) {
                is TextBlock -> {
                    Text(
                        text = buildAnnotatedString {
                            block.spans.forEach { span ->
                                val style = spanStyle(span.attributes)
                                append(span.text, style)
                            }
                        },
                        modifier = blockAlignment(block.attrs)
                    )
                }
                is ImageBlock -> {
                    AsyncImage(model = block.url, contentDescription = null)
                }
                is DividerBlock -> Divider()
                is CodeBlock -> {
                    Text(text = block.code, fontFamily = FontFamily.Monospace,
                         modifier = Modifier.background(codeBg).padding(12.dp))
                }
                is VideoBlock -> { ExoPlayerView(url = block.url) }
                // ... lists, blockquotes, etc.
            }
        }
    }
}
```

**性能：** 中小内容速度快。对于 100+ 块的内容，包含大量组件的 `Column` 可能比单个 `TextView` 更慢。LazyColumn 有帮助但增加复杂度。

**建议：** 理论上最佳的长期方案，但实现成本非常高。`AnnotatedString` 缺乏实现完整 Delta 等价所需的功能（背景色、上标/下标）。高级功能需要维护自定义 `TextLayoutResult`。

---

## 5. 混合方案（Markdown + Compose + WebView）

**工作原理：** 将内容存储为 Markdown。简单格式（粗体、斜体、列表、标题）使用 Compose `Text` 渲染。复杂块（带语法高亮的代码块、LaTeX、嵌入式媒体）才回退到 WebView。

**优点：**
- 两全其美：90% 的内容原生渲染，仅在需要时使用 WebView
- Markdown 紧凑且面向未来
- 简单内容在原生 `Text` 中即时渲染
- 复杂内容在 WebView 中获得完整的 CSS/JS 处理

**缺点：**
- 需要维护两条渲染路径
- 需要 Markdown 解析（CommonMark 或 Markwon 的 AST）
- WebView 回退增加了布局复杂度（在 Compose 滚动中测量 WebView 高度）
- 原生 Text 和 WebView 之间的字体渲染不一致
- 原生和 WebView 块之间的过渡可能出现视觉接缝

**代码示意：**
```kotlin
@Composable
fun HybridMarkdownRenderer(markdown: String) {
    val blocks = parseMarkdown(markdown) // AST parsing
    Column {
        blocks.forEach { block ->
            when {
                block.isSimpleText() -> NativeTextBlock(block)
                block.isCodeBlock() -> WebViewCodeBlock(block) // syntax highlighting
                block.isLatex() -> WebViewLatexBlock(block)    // KaTeX
                block.isMedia() -> NativeMediaBlock(block)     // Coil + ExoPlayer
                else -> NativeTextBlock(block)
            }
        }
    }
}
```

**性能：** 简单内容优秀（原生），复杂内容可接受（仅特定块使用 WebView）。

**建议：** 架构有趣但增加了复杂度。只有在确实有 WebView 擅长处理的特定需求（语法高亮、LaTeX）且希望其他内容原生渲染时才值得。

---

## 6. 代码块语法高亮

### 方案 A：WebView 中使用 Prism.js（扩展当前方案）
- 将 Prism.js 添加到 viewer.html 资源
- 注入内容后调用 `Prism.highlightAll()`
- 优点：支持 200+ 种语言，Web 开发者熟悉
- 缺点：资源增加约 200KB，每个代码块都有 WebView 开销

### 方案 B：原生（highlight.js 移植或自定义）
- **[highlight.js-kotlin](https://github.com/nicolo-ribaudo/highlight.js-kotlin)** 或类似移植 -- 语言支持有限
- **自定义正则高亮器** -- 速度快，但必须为每种语言编写规则
- **Compose `AnnotatedString`** 手动着色 span
- 优点：无 WebView，即时渲染
- 缺点：语言覆盖有限，需手动维护

### 方案 C：TreeSitter（通过 JNI）
- 100+ 种语言的完全保真解析
- 已有 Android 绑定（tree-sitter-android）
- 优点：最高精度，增量解析
- 缺点：二进制体积大（每种语言语法约 2-5MB），JNI 复杂度

**建议：** 如果继续使用 WebView，添加 Prism.js（方案 A）-- 最简单。如果转向原生，先为用户实际使用的 5-10 种语言实现方案 B，仅在精度要求高时考虑 TreeSitter。

---

## 7. 数学公式 / LaTeX 支持

### 方案 A：WebView 中使用 KaTeX
- 将 KaTeX CSS/JS 添加到 viewer.html
- 检测内容中的 `$...$`（行内）和 `$$...$$`（块级）
- 对每个匹配调用 `katex.render()`
- 优点：渲染速度快，完整 LaTeX 数学支持，维护良好
- 缺点：资源增加约 300KB，仅限 WebView

### 方案 B：JLatexMath（原生）
- [JLatexMath](https://github.com/nicolo-ribaudo/JLatexMath-Android) 将 LaTeX 渲染为 Android `Bitmap`
- 以 `Image` 组件或 `TextView` 中的 `ImageSpan` 显示
- 优点：原生渲染，无 WebView
- 缺点：比 KaTeX 慢，库体积更大（约 3MB），符号覆盖有限，无实时预览

### 方案 C：WebView 中使用 MathJax
- 比 KaTeX 更完整但更重（约 1MB）
- 渲染速度更慢
- 除非需要 KaTeX 缺少的功能，否则不推荐

**建议：** 如果保留 WebView 路线，使用方案 A（KaTeX）。仅在完全转向原生渲染且 LaTeX 是必备功能时才考虑 JLatexMath。对于日记应用，LaTeX 优先级可能较低。

---

## 8. 迁移策略

### 当前数据格式
```json
{"ops": [
  {"insert": "Hello "},
  {"insert": "world", "attributes": {"bold": true}},
  {"insert": "\n"},
  {"insert": {"image": "file:///...photo.jpg"}},
  {"insert": "\n", "attributes": {"header": 1}}
]}
```

### 方案 A：保留 Delta，改进查看器（推荐）
- 无需迁移
- 渐进式改进 `deltaToHtml()`
- 考虑在 Room 中缓存 HTML 输出（`renderedHtml` 列），避免每次查看都重新解析

### 方案 B：Delta-to-Markdown 转换器
```kotlin
fun deltaToMarkdown(delta: Delta): String {
    val sb = StringBuilder()
    var currentBlock = StringBuilder()
    var blockAttrs: Map<String, Any>? = null

    for (op in delta.ops) {
        when {
            op.insert is String -> {
                val text = op.insert as String
                val attrs = op.attributes ?: emptyMap()
                val lines = text.split("\n")
                for ((i, line) in lines.withIndex()) {
                    if (i > 0) {
                        sb.append(applyBlockMarkdown(currentBlock.toString(), blockAttrs))
                        currentBlock.clear()
                        blockAttrs = null
                    }
                    if (line.isNotEmpty()) {
                        currentBlock.append(applyInlineMarkdown(line, attrs))
                    }
                    if (i < lines.size - 1) blockAttrs = attrs
                }
            }
            op.insert is Map<*, *> -> {
                val obj = op.insert as Map<*, *>
                when {
                    "image" in obj -> sb.append("![image](${obj["image"]})\n")
                    "video" in obj -> sb.append("[video](${obj["video"]})\n")
                    "divider" in obj -> sb.append("\n---\n\n")
                }
            }
        }
    }
    return sb.toString()
}
```

**有损转换：**
- 自定义文字颜色 --> 丢失（Markdown 无颜色语法）
- 背景色 --> 丢失
- 上标/下标 --> 丢失
- 文本对齐 --> 丢失
- 缩进级别 --> 部分丢失（Markdown 列表支持嵌套）

### 方案 C：Delta-to-HTML 转换器（存储 HTML）
- 复用 `deltaToHtml()` JS 逻辑，移植到 Kotlin
- 在新的 `contentHtml` 列中存储 HTML
- 查看器直接加载 HTML（无需解析 Delta）
- 有损：无（与当前渲染完全一致）

### 迁移执行
```kotlin
// Room migration: add new column
val MIGRATION_XX_YY = object : Migration(XX, YY) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE diary_entries ADD COLUMN contentHtml TEXT DEFAULT ''")
    }
}

// Background job to convert existing entries
suspend fun migrateContentToHtml(diaryDao: DiaryDao) {
    val entries = diaryDao.getAllEntries()
    for (entry in entries) {
        val html = convertDeltaToHtml(entry.content) // Kotlin port of deltaToHtml()
        diaryDao.updateContentHtml(entry.id, html)
    }
}
```

---

## 总结建议

| 方案 | 工作量 | 风险 | 收益 |
|---|---|---|---|
| 保留 Delta + WebView | 无 | 无 | 维持现状，当前可用 |
| 在 Room 中缓存 HTML | 低 | 低 | 冷启动更快（跳过 Delta 解析） |
| Markdown + Markwon | 高 | 中 | 原生渲染，内存更小 |
| Compose 原生 | 很高 | 高 | 完全原生集成 |
| 混合方案 | 高 | 中 | 两全其美，但复杂度高 |

**实际建议：** 保留 Delta 作为存储格式。添加缓存的 `renderedHtml` 列（方案 C 迁移），跳过每次查看时的 `deltaToHtml()` 调用。这是以最低风险获得最快改进的方式。

如果想完全摆脱 WebView，Markdown + Markwon 路线对于日记应用（而非代码编辑器）最为现实。Compose 原生路线是理想方向，但 `AnnotatedString` 尚未准备好实现完整的富文本等价。

编辑器是另一个独立问题 -- 查看器渲染引擎的选择不应受编辑器格式的约束。无论以何种方式渲染，Delta JSON 作为存储格式都是合理的。
