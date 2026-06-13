# Image Viewer, Editor Media Insertion, and Detail Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix image insertion, full-screen image viewing, and detail-page stability without replacing the current editor/viewer architecture.

**Architecture:** Keep the existing `Compose + WebView + Quill + Coil` stack. The editor owns media capture and writes stable local files into `diary_media`, the editor HTML renders those files through a stable WebView asset URL, and the detail page uses the viewer HTML plus a Compose overlay for full-screen image browsing. Detail stability is improved by removing nested scroll behavior where possible and by tightening the JS bridge contract between HTML and Compose.

**Tech Stack:** Kotlin, Jetpack Compose, WebView, Quill.js, Coil, WebViewAssetLoader, JUnit4, Gradle

---

### Task 1: Lock down the shared media URL contract

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/components/WebViewAssetHelper.kt`
- Test: `app/src/test/java/com/diary/app/ui/components/WebViewAssetHelperTest.kt`

- [ ] **Step 1: Write the failing tests for local media URL conversion**

```kotlin
@Test
fun `diary media file path becomes appassets url`() {
    val filePath = "/data/user/0/com.diary.app/files/diary_media/img_123.jpg"
    assertEquals("https://appassets/diary_media/img_123.jpg", WebViewAssetHelper.toWebViewUrl(filePath))
}

@Test
fun `file url for diary media becomes appassets url`() {
    val fileUrl = "file:///data/user/0/com.diary.app/files/diary_media/img_123.jpg"
    assertEquals("https://appassets/diary_media/img_123.jpg", WebViewAssetHelper.toWebViewUrlFromFileUrl(fileUrl))
}

@Test
fun `non media path keeps file url fallback`() {
    val filePath = "/sdcard/Pictures/other.jpg"
    assertEquals("file:///sdcard/Pictures/other.jpg", WebViewAssetHelper.toWebViewUrl(filePath))
}
```

- [ ] **Step 2: Run the helper tests and verify they fail first**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.components.WebViewAssetHelperTest`

Expected:
- Test class is missing or assertions fail because the contract is not fully enforced yet.

- [ ] **Step 3: Implement the minimal helper behavior**

```kotlin
fun toWebViewUrl(filePath: String): String {
    val normalized = filePath.replace("\\", "/")
    val mediaIndex = normalized.indexOf("/diary_media/")
    if (mediaIndex >= 0) {
        val relativePath = normalized.substring(mediaIndex + 1)
        return "https://$AUTHORITY/$relativePath"
    }
    return if (normalized.startsWith("file://")) normalized else "file://$normalized"
}
```

Keep the existing `createAssetLoader(...)` and `interceptRequest(...)` entry points, but make sure the helper always returns one stable URL shape for `diary_media` files.

- [ ] **Step 4: Re-run the helper tests**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.components.WebViewAssetHelperTest`

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit the helper contract change**

```bash
git add app/src/main/java/com/diary/app/ui/components/WebViewAssetHelper.kt app/src/test/java/com/diary/app/ui/components/WebViewAssetHelperTest.kt
git commit -m "test: lock down webview media url contract"
```

### Task 2: Make editor image insertion deterministic

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/assets/editor.html`
- Test: `app/src/test/java/com/diary/app/ui/editor/EditorAssetSourceTest.kt`

- [ ] **Step 1: Write failing tests for the editor asset contract**

```kotlin
@Test
fun `editor html keeps image loading state and media insert hook`() {
    val html = File("app/src/main/assets/editor.html").readText()

    assertTrue(html.contains("function insertMedia(type, url)"))
    assertTrue(html.contains("data-loading"))
    assertTrue(html.contains("finalizeMediaInsert"))
    assertTrue(html.contains("setContentBase64"))
}
```

```kotlin
@Test
fun `editor screen writes diary media files into stable webview urls`() {
    val mediaUrl = WebViewAssetHelper.toWebViewUrl("/data/user/0/com.diary.app/files/diary_media/img_123.jpg")
    assertEquals("https://appassets/diary_media/img_123.jpg", mediaUrl)
}
```

- [ ] **Step 2: Run the editor tests and confirm they fail**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorAssetSourceTest`

Expected:
- The asset test fails because the current HTML contract is only partially enforced, or the test file is still missing.

- [ ] **Step 3: Update the editor insertion flow to use only local saved media**

```kotlin
val webViewUrl = WebViewAssetHelper.toWebViewUrl(outputFile.absolutePath)
webView?.evaluateJavascript("insertMedia('image', '${escapeForJs(webViewUrl)}')", null)
```

Keep the current image picker, but make the save step explicit:
- decode the chosen image into a bitmap
- scale down oversized images before writing
- save into `filesDir/diary_media`
- insert the saved file URL, not an inline Base64 image

Preserve the current `data-loading` behavior in `editor.html`:
- set `data-loading="true"` on the inserted image
- clear it on `load` or `error`
- move the cursor after the inserted image only after the load path finishes

- [ ] **Step 4: Tighten the HTML insertion and restore logic**

```javascript
function insertMedia(type, url) {
    try {
        var r = quill.getSelection(true);
        if (type === 'image') {
            quill.insertEmbed(r.index, 'image', url, 'user');
            var insertedLeaf = quill.getLeaf(r.index);
            var insertedImage = insertedLeaf && insertedLeaf[0] && insertedLeaf[0].domNode
                ? insertedLeaf[0].domNode
                : null;
            quill.insertText(r.index + 1, '\n', 'user');
            var nextIndex = r.index + 2;
            if (insertedImage) {
                insertedImage.setAttribute('data-loading', 'true');
                var finish = function() {
                    insertedImage.removeAttribute('data-loading');
                    insertedImage.onload = null;
                    insertedImage.onerror = null;
                    finalizeMediaInsert(nextIndex);
                };
                insertedImage.onload = finish;
                insertedImage.onerror = finish;
                if (insertedImage.complete) finish();
            } else {
                finalizeMediaInsert(nextIndex);
            }
        }
    } catch (e) {
        console.error('insertMedia error:', e);
    }
}
```

Keep the existing `setContentBase64(...)` path for saved draft/entry reloads, because that is the current persistence path and it already restores the editor content shape.

- [ ] **Step 5: Re-run the editor asset test and compile check**

Run:
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorAssetSourceTest`
`.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- HTML contract test passes
- Kotlin compilation passes

- [ ] **Step 6: Commit the editor image insertion fix**

```bash
git add app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/main/assets/editor.html app/src/test/java/com/diary/app/ui/editor/EditorAssetSourceTest.kt
git commit -m "feat: stabilize editor image insertion"
```

### Task 3: Make the detail page open images in a real full-screen viewer

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/detail/DiaryDetailScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/detail/ImageViewerScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/detail/DetailJsBridge.kt`
- Modify: `app/src/main/assets/viewer.html`
- Test: `app/src/test/java/com/diary/app/ui/detail/DetailJsBridgeTest.kt`
- Test: `app/src/test/java/com/diary/app/ui/viewer/ViewerAssetSourceTest.kt`

- [ ] **Step 1: Write failing tests for the image click bridge**

```kotlin
@Test
fun `image click payload keeps clicked url and all urls`() {
    val bridge = DetailJsBridge()
    val events = mutableListOf<DetailJsBridge.ImageClickEvent>()

    // collect the emitted event in a test coroutine or blocking helper
    bridge.onImageClick(
        "https://appassets/diary_media/img_2.jpg",
        """["https://appassets/diary_media/img_1.jpg","https://appassets/diary_media/img_2.jpg"]"""
    )

    // assert the emitted event equals the parsed list
}
```

```kotlin
@Test
fun `viewer html keeps image click handler contract`() {
    val html = File("app/src/main/assets/viewer.html").readText()

    assertTrue(html.contains("DiaryBridge.onImageClick"))
    assertTrue(html.contains("setContentFromBase64"))
    assertTrue(html.contains("#content img"))
}
```

- [ ] **Step 2: Run the new bridge and viewer tests and verify failure**

Run:
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.detail.DetailJsBridgeTest`
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.viewer.ViewerAssetSourceTest`

Expected:
- Bridge test fails until payload parsing is covered explicitly
- Viewer asset test fails if the click contract is not present or the HTML changes

- [ ] **Step 3: Make the viewer overlay consume image click events without closing the detail page**

```kotlin
LaunchedEffect(Unit) {
    detailJsBridge.imageClicks.collect { event ->
        imageViewerUrls = event.allUrls
        imageViewerIndex = event.allUrls.indexOf(event.clickedUrl).coerceAtLeast(0)
    }
}
```

Keep the full-screen overlay model in Compose:
- clicking an image from the detail HTML opens `ImageViewerScreen`
- closing the viewer resets only the overlay state
- the detail page stays mounted, so the web content does not reload

- [ ] **Step 4: Make the viewer resolve local media URLs through Coil, not ad hoc bitmap decoding**

```kotlin
val localPath = remember(url) {
    when {
        url.startsWith("https://appassets/") -> {
            val relativePath = url.removePrefix("https://appassets/")
            File(context.filesDir, relativePath).absolutePath
        }
        url.startsWith("file://") -> url.removePrefix("file://")
        else -> url
    }
}

AsyncImage(
    model = ImageRequest.Builder(context)
        .data(localPath)
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

Do not add a manual bitmap loader in the viewer. Keep Coil as the single loading path so caching, recycling, and decoding stay consistent.

- [ ] **Step 5: Re-run the bridge and viewer tests, then compile**

Run:
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.detail.DetailJsBridgeTest`
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.viewer.ViewerAssetSourceTest`
`.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- Bridge test passes
- Viewer asset test passes
- Kotlin compilation passes

- [ ] **Step 6: Commit the full-screen viewer wiring**

```bash
git add app/src/main/java/com/diary/app/ui/detail/DiaryDetailScreen.kt app/src/main/java/com/diary/app/ui/detail/ImageViewerScreen.kt app/src/main/java/com/diary/app/ui/detail/DetailJsBridge.kt app/src/main/assets/viewer.html app/src/test/java/com/diary/app/ui/detail/DetailJsBridgeTest.kt app/src/test/java/com/diary/app/ui/viewer/ViewerAssetSourceTest.kt
git commit -m "feat: add full screen image viewer"
```

### Task 4: Stabilize the detail page scroll and render flow

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/detail/DiaryDetailScreen.kt`
- Modify: `app/src/main/assets/viewer.html`
- Modify: `app/src/main/java/com/diary/app/ui/components/WebViewAssetHelper.kt`

- [ ] **Step 1: Write the failing regression checks for the detail page HTML and layout assumptions**

```kotlin
@Test
fun `viewer html keeps paragraph spacing compact for long entries`() {
    val html = File("app/src/main/assets/viewer.html").readText()

    assertTrue(html.contains("#content p { margin: 0; }"))
    assertTrue(html.contains("#content p:empty::before"))
}
```

```kotlin
@Test
fun `detail page keeps media assets on the appassets path`() {
    val path = "/data/user/0/com.diary.app/files/diary_media/img_777.jpg"
    assertEquals("https://appassets/diary_media/img_777.jpg", WebViewAssetHelper.toWebViewUrl(path))
}
```

- [ ] **Step 2: Run the regression tests and watch them fail first**

Run:
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.viewer.ViewerAssetSourceTest`
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.components.WebViewAssetHelperTest`

Expected:
- Either a failing assertion or a missing updated contract

- [ ] **Step 3: Reduce nested scroll in the detail page**

```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    // fixed header / meta bands
    // WebView content area
    // bottom actions
}
```

Keep the article body inside the WebView as the primary scroll surface. Avoid wrapping the whole detail page in an outer `verticalScroll(...)` container around the WebView content area, because that is the source of the jumpy double-scroll behavior.

If the header or metadata blocks need to be visible while reading, keep them outside the WebView as fixed bands instead of forcing a second scroll layer.

- [ ] **Step 4: Keep the viewer HTML render path stable across existing content**

```javascript
function setContentFromBase64(base64) {
    try {
        var decoded = decodeURIComponent(escape(atob(base64)));
        withProgrammaticChange(function() {
            try {
                var parsed = JSON.parse(decoded);
                if (parsed && parsed.ops) {
                    quill.setContents(parsed, 'silent');
                    resetEditorContext();
                    return;
                }
            } catch(e1) {}
            quill.setText(decoded, 'silent');
            resetEditorContext();
        });
    } catch(e) {
        console.error('setContentFromBase64 error:', e);
    }
}
```

Do not replace the current `setContentFromBase64` entry point in this phase. It is already the stable reload path for existing entries and drafts.

- [ ] **Step 5: Run the detail-page compile and test pass**

Run:
`.\gradlew.bat :app:compileExperimentalDebugKotlin`
`.\gradlew.bat :app:testExperimentalDebugUnitTest`

Expected:
- Kotlin compile succeeds
- Existing unit tests still pass
- No new regression in the viewer asset contract

- [ ] **Step 6: Commit the detail page stability work**

```bash
git add app/src/main/java/com/diary/app/ui/detail/DiaryDetailScreen.kt app/src/main/assets/viewer.html app/src/main/java/com/diary/app/ui/components/WebViewAssetHelper.kt
git commit -m "fix: stabilize diary detail rendering"
```

### Task 5: Verify the whole media path end to end

**Files:**
- No code changes expected unless verification exposes a real regression

- [ ] **Step 1: Run the focused test suite for the image path**

Run:
`.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.components.WebViewAssetHelperTest --tests com.diary.app.ui.editor.EditorAssetSourceTest --tests com.diary.app.ui.detail.DetailJsBridgeTest --tests com.diary.app.ui.viewer.ViewerAssetSourceTest`

Expected:
- All targeted tests pass

- [ ] **Step 2: Run the Kotlin compile again**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 3: Run the full debug unit test suite if the focused tests are clean**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest`

Expected:
- No failures related to image insertion, full-screen image viewing, or detail rendering

- [ ] **Step 4: Record the final verification status**

Capture the exact commands used and the passing output before claiming completion.
Do not mark the work done until the fresh verification run has passed.

## Self-Review

- Scope coverage: image insertion, detail image viewer, and detail stability are all assigned a task.
- Placeholder scan: no TBD/TODO placeholders were left in the plan.
- Type consistency: the plan uses the existing `WebViewAssetHelper`, `DetailJsBridge`, `ImageViewerScreen`, `editor.html`, and `viewer.html` entry points consistently.
- Risk check: the only structural assumption is that the detail page should keep the current WebView-based rendering model and only tighten the scroll contract, not replace the storage format.
