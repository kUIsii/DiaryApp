# Detail Page Redesign Proposal

Current state: `DiaryDetailScreen.kt` (674 lines) + `viewer.html` (447 lines) + `DiaryDetailViewModel.kt` (198 lines) + `DiaryExporter.kt` (438 lines).

---

## 1. Native Rendering vs WebView

### Current Problems

- WebView inside a `verticalScroll` Column causes nested scroll conflicts -- the WebView has its own internal scroll, the outer Column also scrolls. This produces janky behavior where sometimes the WebView eats the scroll and sometimes the Column does.
- 2MB content limit with plain text fallback is an arbitrary workaround for WebView memory pressure.
- `Base64` encode/decode round-trip on every page load adds latency.
- WebView dark mode requires JS bridge (`setTheme`), creating a split-brain: Compose controls theme state, then pushes it to WebView asynchronously.
- `shouldInterceptRequest` for local file:// media is fragile -- file paths break when storage changes.
- WebView lifecycle is tricky: `DisposableEffect` cleanup, `stopLoading` vs `destroy` ordering, potential leaks.

### Recommendation: Hybrid -- Keep WebView, Fix Integration

Switching to native Compose rendering for Quill Delta is **not worth it** right now. Reasons:

1. Delta JSON has complex inline formatting (bold/italic/code/link/color nested in a single span). Native Compose `AnnotatedString` can do this, but building a full Delta-to-AnnotatedString parser is 300+ lines of delicate code for marginal visual improvement.
2. Code blocks, blockquotes, checklists, nested lists -- each needs a custom Compose layout. WebView handles these with 10 lines of CSS.
3. The existing `viewer.html` is battle-tested and handles malformed/corrupted Delta JSON with multiple fallback strategies (lines 382-436).

Instead, fix the WebView integration:

```kotlin
// Replace nested scroll with a single coordinated scroll host
// Option A: Let WebView own the scroll, put header/footer via JavaScript injection
// Option B: Use AndroidView with NestedScrollConnection to forward flings

// Recommended: Option A -- inject header/footer into WebView
// Move DetailHeader, DetailTags, DetailTimestamps into viewer.html as
// a JS function: setHeader(html) and setFooter(html)
// This eliminates the nested scroll problem entirely.
```

**Effort**: 2-3 days. Refactor `DiaryDetailScreen` to inject header/footer HTML into WebView, remove outer `verticalScroll`, let WebView own all scrolling.

### Future Path to Native

If you ever want native rendering, the cleanest path is:
- Use a Markdown-based storage format instead of Quill Delta.
- Render with a Compose Markdown library (e.g., `compose-markdown` or `richtext-commonmark`).
- This is a storage migration, not just a UI change -- so it's a separate project.

---

## 2. Swipe Between Entries

### Current Problem

From the detail page, the only way to read the previous/next entry is to go back to the list and tap again. For reading through a sequence of entries (e.g., reviewing a week), this is tedious.

### Proposed Solution

Use `HorizontalPager` from `androidx.compose.foundation` to wrap the detail content. Each "page" loads one entry.

```kotlin
// In ViewModel, maintain a list of entry IDs from the current context
private val _entryIds = MutableStateFlow<List<Long>>(emptyList())
private val _currentIndex = MutableStateFlow(0)

fun initWithContext(entryId: Long, contextEntryIds: List<Long>) {
    _entryIds.value = contextEntryIds
    _currentIndex.value = contextEntryIds.indexOf(entryId).coerceAtLeast(0)
    loadEntry(entryId)
}

// In Screen
val pagerState = rememberPagerState(initialPage = currentIndex) { entryIds.size }

HorizontalPager(state = pagerState) { page ->
    // Each page is a DiaryDetailContent(entryIds[page])
    DiaryDetailContent(
        entryId = entryIds[page],
        // ... same props
    )
}

// When page changes, load the new entry
LaunchedEffect(pagerState.currentPage) {
    viewModel.loadEntry(entryIds[pagerState.currentPage])
}
```

Key design decisions:
- The list page passes its current filtered entry IDs (e.g., from search, tag filter, date range) so swipe context matches what the user was browsing.
- Pre-load adjacent entries (just preview, not full content) to reduce perceived latency.
- Swipe animation uses the default `HorizontalPager` spring -- feels native.
- The top bar shows "3 / 42" indicator when swiping, so the user knows their position.

**DAO change needed**: Add a lightweight query to get ordered entry IDs for a given filter context.

```kotlin
@Query("SELECT id FROM diary_entries ORDER BY createdAt DESC")
suspend fun getAllEntryIds(): List<Long>

@Query("SELECT id FROM diary_entries WHERE id IN (SELECT diaryId FROM diary_tag_cross_ref WHERE tagId = :tagId) ORDER BY createdAt DESC")
suspend fun getEntryIdsByTag(tagId: Long): List<Long>
```

**Effort**: 3-4 days. HorizontalPager integration, ViewModel context management, pre-loading, navigation plumbing.

---

## 3. Image Gallery

### Current Problem

Images in the WebView content are rendered inline but have no interaction -- tapping an image does nothing special. There's no way to zoom into a specific image, view it full-screen, or swipe through all images in the entry.

### Proposed Solution

Inject click handlers into `viewer.html` for images, bridge to Compose via `JavascriptInterface`, and show a full-screen image viewer overlay.

```kotlin
// Step 1: Add JS interface in viewer.html
// In deltaToHtml(), wrap each img with onclick:
// <img src="..." onclick="window.Android.onImageTap(this.src)" />

// Step 2: In AndroidView factory, add interface
webView.addJavascriptInterface(object {
    @JavascriptInterface
    fun onImageTap(src: String) {
        // Post to main thread, show overlay
        imageViewerUrl = src
    }
}, "Android")

// Step 3: Full-screen overlay with HorizontalPager + zoom
if (showImageViewer) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = imagePagerState) { page ->
            AsyncImage(
                model = images[page],
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                    }
                    .transformable(state = transformState),  // pinch-to-zoom
                contentScale = ContentScale.Fit
            )
        }
        // Close button, image counter "2 / 5"
    }
}
```

Image source extraction: parse the Delta JSON for `insert.image` ops to build the image list, so the pager knows all images in order.

```kotlin
// In ViewModel
fun extractImageUrls(): List<String> {
    val content = _entry.value?.content ?: return emptyList()
    return try {
        val json = org.json.JSONObject(content)
        val ops = json.getJSONArray("ops")
        (0 until ops.length()).mapNotNull { i ->
            val op = ops.getJSONObject(i)
            val insert = op.optJSONObject("insert")
            insert?.optString("image")?.takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        emptyList()
    }
}
```

**Effort**: 2-3 days. JS bridge, full-screen viewer with zoom (using `transformable` modifier), image list extraction from Delta.

---

## 4. Reading Mode

### Current Problem

- Font size is read from SharedPreferences once at composition time (`fontSizePx`) and pushed to WebView via JS. No way to change it without leaving the page.
- Dark mode content rendering is a JS call that happens after page load -- there's a visible flash of light-mode content before dark kicks in.
- No "reading mode" concept -- the detail page always looks the same regardless of intent (quick glance vs. deep reading).

### Proposed Solution

Add a reading mode toggle (icon in the top bar) that activates:

1. **Immediate dark mode**: Instead of loading viewer.html with transparent background then calling `setTheme`, set the theme in the HTML itself before rendering. Pass theme via URL parameter: `viewer.html?theme=dark`.

```kotlin
// Replace
loadUrl("file:///android_asset/viewer.html")
// With
loadUrl("file:///android_asset/viewer.html?theme=${if (isDark) "dark" else "light"}")

// In viewer.html, read on load:
const params = new URLSearchParams(window.location.search);
if (params.get('theme') === 'dark') setTheme('dark');
```

2. **Font size slider**: A bottom sheet or inline slider that calls `setFontSize()` in real-time. Persist the choice back to SharedPreferences.

```kotlin
// Reading mode overlay
if (readingMode) {
    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { newSize ->
                fontSize = newSize.toInt()
                webView?.evaluateJavascript("setFontSize($fontSize)", null)
            },
            valueRange = 12f..24f,
            steps = 5
        )
    }
}
```

3. **Immersive mode**: Hide the status bar and bottom navigation. Use `WindowCompat.setDecorFitsSystemWindows(window, false)` and let content extend edge-to-edge.

**Effort**: 1-2 days. Mostly UI polish -- the WebView already supports `setFontSize` and `setTheme`, the work is in the Compose wrapper and persistence.

---

## 5. Share Redesign

### Current Problem

- Share button is not in the bottom action bar -- it's missing entirely from `DetailBottomBar`. The share/export functionality exists in `DiaryDetailViewModel.getShareText()` and `exportAsImage()` but there's no obvious UI trigger visible in the detail screen code.
- The share text format is basic: date + mood/weather + plain text + tags + "from DiaryApp".
- No preview of what will be shared.

### Proposed Solution

Add a share button to `DetailBottomBar` that opens a share bottom sheet with three options:

```kotlin
// Add to DetailBottomBar
BottomActionButton(
    icon = Icons.Default.Share,
    label = "分享",
    tint = textSecondary.copy(alpha = 0.7f),
    onClick = { showShareSheet = true }
)

// Share bottom sheet
if (showShareSheet) {
    ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
        // Preview card (mini version of the export image)
        SharePreviewCard(entry = currentEntry, tags = tags)

        // Options
        ShareOptionRow(
            icon = Icons.Default.TextFields,
            label = "文字分享",
            subtitle = "纯文本格式",
            onClick = { shareAsText() }
        )
        ShareOptionRow(
            icon = Icons.Default.Image,
            label = "图片分享",
            subtitle = "精美的长图",
            onClick = { shareAsImage() }
        )
        ShareOptionRow(
            icon = Icons.Default.Description,
            label = "Markdown",
            subtitle = ".md 文件",
            onClick = { shareAsMarkdown() }
        )
    }
}
```

Share as text should use the system share sheet directly:

```kotlin
fun shareAsText() {
    val text = viewModel.getShareText() ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享日记"))
}
```

**Effort**: 1-2 days. Add share button, build bottom sheet, wire up existing ViewModel methods.

---

## 6. Export Enhancement

### Current Problems

- Image export uses raw `Canvas` drawing with hardcoded fonts and colors. It doesn't support dark mode, doesn't render rich formatting (bold/italic/headers are lost -- only plain text), and the layout is rigid.
- No PDF export.
- Exported images don't include embedded images from the diary -- only text.

### Proposed Solution

**A. Better image export**: Instead of Canvas drawing, render the WebView to a bitmap. This captures the exact visual appearance including rich formatting, embedded images, and dark mode.

```kotlin
suspend fun exportAsImage(context: Context): String? {
    val wv = webView ?: return null
    return withContext(Dispatchers.Main) {
        // Capture WebView content as bitmap
        val contentHeight = wv.evaluateJavascript(
            "document.body.scrollHeight"
        ) { it.toInt() }  // need callback-based approach

        // Set WebView to full content height, then draw
        wv.layout(0, 0, wv.width, contentHeight)
        val bitmap = Bitmap.createBitmap(wv.width, contentHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        wv.draw(canvas)

        // Add header/footer overlay with Canvas
        // Save to file
        DiaryExporter.saveBitmapToFile(context, bitmap, fileName)
    }
}
```

Note: `WebView.draw()` captures the current visible state. For full-page capture, you need to use `WebView.capturePicture()` (deprecated) or the `PrintDocumentAdapter` approach. A practical alternative is to create a hidden WebView sized to the full content height.

**B. PDF export**: Use Android's `PrintDocumentAdapter` from WebView.

```kotlin
fun exportAsPdf(context: Context) {
    val wv = webView ?: return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = wv.createPrintDocumentAdapter("diary_export")
    printManager.print("Diary Export", printAdapter, PrintAttributes.Builder().build())
}
```

This is surprisingly simple because WebView already has built-in print support. The system handles pagination, margins, and file creation.

**C. Preserve rich formatting in exports**: Currently `exportSingleAsMarkdown` only uses `plainText`. To preserve formatting, add a Delta-to-Markdown converter:

```kotlin
fun deltaToMarkdown(delta: String): String {
    // Parse Delta JSON, convert:
    // - {bold: true} -> **text**
    // - {italic: true} -> *text*
    // - {header: 1} -> # text
    // - {list: "bullet"} -> - text
    // - {insert: {image: "..."}} -> ![](url)
    // ~80 lines of conversion logic
}
```

**Effort**: 2-3 days. WebView-based image capture (1 day), PDF export via PrintAdapter (0.5 day), Delta-to-Markdown converter (1 day).

---

## Priority and Sequencing

| # | Feature | Effort | Impact | Priority |
|---|---------|--------|--------|----------|
| 5 | Share redesign | 1-2d | High -- user-facing, easy win | Do first |
| 4 | Reading mode | 1-2d | Medium -- quality of life | Do second |
| 6 | Export enhancement | 2-3d | Medium -- PDF export is a differentiator | Do third |
| 1 | WebView scroll fix | 2-3d | High -- fixes jank, but risky refactor | Do fourth |
| 3 | Image gallery | 2-3d | Medium -- good for image-heavy entries | Do fifth |
| 2 | Swipe between entries | 3-4d | High -- changes navigation model | Do last (biggest scope) |

Total estimated effort: 11-17 days. Each feature is independently shippable.

---

## Files to Modify

- `app/src/main/java/com/diary/app/ui/detail/DiaryDetailScreen.kt` -- main screen, all 6 features touch this
- `app/src/main/java/com/diary/app/ui/detail/DiaryDetailViewModel.kt` -- context management, image extraction, share
- `app/src/main/java/com/diary/app/data/DiaryExporter.kt` -- export improvements
- `app/src/main/java/com/diary/app/data/DiaryDao.kt` -- new lightweight ID queries for swipe
- `app/src/main/assets/viewer.html` -- theme param, image click handlers, header/footer injection
