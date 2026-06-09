# Diary Editor Viewport Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize diary recording so the editor only scrolls when content is actually obscured, uses the full paper viewport, and preserves correct dirty-state and draft behavior.

**Architecture:** Keep the existing `Compose + WebView + Quill` stack, but move all body-scroll ownership into `editor.html`, while Compose provides a stable container height and bottom safety gap. Fix dirty-state and draft recovery by separating programmatic content loading from real user edits and by restoring drafts from full editor snapshots rather than plain text only.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, Quill, JUnit4, Gradle

---

## File structure and responsibilities

- `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
  - Stable editor container sizing
  - Dirty-state gating for programmatic loads
  - Unified autosave triggers for title/body/metadata
  - Toolbar lock reset behavior
- `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
  - Bottom-gap presets
  - Draft restore predicates
  - Pure helper logic for dirty state
- `app/src/main/assets/editor.html`
  - Full-height editable viewport
  - Safe-zone based cursor scrolling
  - Silent programmatic content loading
  - Better scrollbar touch area
- `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
  - Regression tests for helper logic

## Task 1: Add failing regression tests for helper logic

**Files:**
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun `editor bottom gap reserves realistic space for toolbar and panels`() {
    assertEquals(72, resolveEditorBottomGap(showToolbar = false, isKeyboardVisible = false, activeCategory = -1))
    assertEquals(148, resolveEditorBottomGap(showToolbar = true, isKeyboardVisible = true, activeCategory = -1))
    assertEquals(228, resolveEditorBottomGap(showToolbar = true, isKeyboardVisible = true, activeCategory = 2))
}

@Test
fun `should restore draft when metadata exists without plain text`() {
    assertTrue(
        shouldRestoreDraft(
            EditorSnapshot(
                moodLevel = 3,
                defaultTitle = "2026年6月9日"
            )
        )
    )
    assertFalse(shouldRestoreDraft(EditorSnapshot()))
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`
Expected: fails because helper outputs still match old values or the overload does not exist yet.

- [ ] **Step 3: Implement the minimal helper changes**

```kotlin
internal fun resolveEditorBottomGap(
    showToolbar: Boolean,
    isKeyboardVisible: Boolean,
    activeCategory: Int
): Int {
    return when {
        showToolbar && activeCategory >= 0 -> 228
        showToolbar || isKeyboardVisible -> 148
        else -> 72
    }
}

internal fun shouldRestoreDraft(snapshot: EditorSnapshot): Boolean {
    return isMeaningfulDraft(snapshot)
}
```

- [ ] **Step 4: Re-run the focused test**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`
Expected: `EditorUtilsTest` passes if the local Gradle test worker is healthy; otherwise the known worker startup error is the remaining blocker.

## Task 2: Fix editor viewport sizing and scroll logic in WebView

**Files:**
- Modify: `app/src/main/assets/editor.html`

- [ ] **Step 1: Create the failing behavioral target in comments/checklist before code**

Manual target:
- typing on a visible next line does not scroll
- newline only scrolls when the caret exits the safe viewport
- scrollbar spans the full editor height
- programmatic content loads do not mark content dirty

- [ ] **Step 2: Implement full-height viewport and wider scrollbar target**

```css
html, body {
    height: 100%;
    min-height: 100%;
}

body {
    height: 100%;
    overflow: hidden;
}

#editor,
.ql-container.ql-snow,
.ql-editor {
    height: 100%;
}

.ql-editor {
    padding: 12px 20px calc(var(--editor-bottom-gap, 72px) + 28px) 34px;
    scrollbar-width: auto;
}

.ql-editor::-webkit-scrollbar {
    width: 8px;
}
```

- [ ] **Step 3: Add programmatic-load guards**

```javascript
var isProgrammaticChange = false;

function withProgrammaticChange(action) {
    isProgrammaticChange = true;
    try {
        action();
    } finally {
        setTimeout(function() {
            isProgrammaticChange = false;
        }, 0);
    }
}
```

```javascript
function setContentBase64(base64) {
    try {
        var decoded = decodeURIComponent(escape(atob(base64)));
        withProgrammaticChange(function() {
            var parsed = JSON.parse(decoded);
            if (parsed && parsed.ops) {
                quill.setContents(parsed, 'silent');
                resetEditorContext();
            } else {
                quill.setText(decoded, 'silent');
                resetEditorContext();
            }
        });
    } catch (e) {
        console.error('setContentBase64 error:', e);
    }
}
```

- [ ] **Step 4: Replace aggressive cursor scrolling with safe-zone checks**

```javascript
function getViewportState(editor) {
    var bottomGap = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--editor-bottom-gap')) || 72;
    var topInset = 18;
    var bottomInset = Math.max(bottomGap + 16, 96);
    var visibleTop = editor.scrollTop + topInset;
    var visibleBottom = editor.scrollTop + editor.clientHeight - bottomInset;
    return { visibleTop: visibleTop, visibleBottom: visibleBottom };
}

function scrollToCursor(index, length, forceRestore) {
    clearTimeout(_scrollTimer);
    _scrollTimer = setTimeout(function() {
        try {
            if (!forceRestore && Date.now() < editorContext.restoreLockUntil) return;
            var bounds = quill.getBounds(index, length);
            var editor = document.querySelector('.ql-editor');
            if (!editor || !bounds) return;
            var cursorTop = bounds.top;
            var cursorBottom = bounds.top + Math.max(bounds.height, 24);
            var viewport = getViewportState(editor);
            if (!forceRestore && cursorTop >= viewport.visibleTop && cursorBottom <= viewport.visibleBottom) {
                return;
            }
            var targetTop = editor.scrollTop;
            if (cursorBottom > viewport.visibleBottom) {
                targetTop += cursorBottom - viewport.visibleBottom;
            } else if (cursorTop < viewport.visibleTop) {
                targetTop -= viewport.visibleTop - cursorTop;
            }
            editor.scrollTo({ top: Math.max(targetTop, 0), behavior: 'auto' });
        } catch (e) {}
    }, forceRestore ? 0 : 16);
}
```

- [ ] **Step 5: Suppress dirty reporting for programmatic changes**

```javascript
quill.on('text-change', function(delta, oldDelta, source) {
    if (!isProgrammaticChange && window.DiaryBridge) {
        DiaryBridge.onContentChange(quill.getText());
    }
    setTimeout(function() {
        var sel = quill.getSelection();
        if (sel) {
            saveEditorContext(sel);
            scrollToCursor(sel.index, sel.length, false);
        }
    }, 16);
});
```

## Task 3: Fix Compose-side dirty state, draft restore, and toolbar behavior

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`

- [ ] **Step 1: Add state to ignore programmatic content loads and track metadata autosave versions**

```kotlin
var isApplyingProgrammaticContent by remember { mutableStateOf(false) }
var metadataVersion by remember { mutableIntStateOf(0) }
```

- [ ] **Step 2: Guard content change collection**

```kotlin
LaunchedEffect(Unit) {
    jsBridge.contentChanges.collect { text ->
        latestPlainText = text
        charCount = text.length
        wordCount = countWords(text)
        if (!isApplyingProgrammaticContent) {
            viewModel.markContentChanged()
            contentVersion++
        }
    }
}
```

- [ ] **Step 3: Wrap draft/existing-entry/template loads in programmatic guards**

```kotlin
isApplyingProgrammaticContent = true
webView?.evaluateJavascript("setContentBase64('$base64Content')") {
    isApplyingProgrammaticContent = false
}
```

- [ ] **Step 4: Trigger autosave for title and metadata changes**

```kotlin
entryTitle = it
viewModel.markContentChanged()
metadataVersion++
```

```kotlin
selectedMood = it
viewModel.markContentChanged()
metadataVersion++
```

- [ ] **Step 5: Add a second debounce for metadata-only changes**

```kotlin
LaunchedEffect(metadataVersion) {
    if (metadataVersion > 0 && hasUnsavedChanges) {
        kotlinx.coroutines.delay(2500)
        webView?.evaluateJavascript("getContent()") { json ->
            val cleanJson = unescapeEvaluateJsResult(json)
            val saveTitle = entryTitle.ifBlank { dateTitle }
            viewModel.updateLatestContent(cleanJson, latestPlainText, saveTitle)
            viewModel.performAutoSave(diaryId, selectedMood, selectedWeather, selectedLocation, locationLat, locationLng)
        }
    }
}
```

- [ ] **Step 6: Restore drafts from full snapshots instead of plain text**

```kotlin
val draft = viewModel.loadDraft(diaryId)
if (draft != null && shouldRestoreDraft(
        EditorSnapshot(
            title = draft.title,
            plainText = draft.plainText,
            moodLevel = draft.moodLevel,
            weather = draft.weather,
            tagIds = draft.tagIds,
            location = draft.location,
            defaultTitle = dateTitle
        )
    )
) {
    pendingDraft = draft
    showDraftDialog = true
}
```

- [ ] **Step 7: Allow toolbar lock to return to auto mode when focus resumes**

```kotlin
webView?.requestFocus()
webView?.evaluateJavascript("focusEditorWithRestore()", null)
isToolbarLocked = false
```

## Task 4: Verification

**Files:**
- Modify: `docs/build-notes.md` only if new local verification caveats are discovered

- [ ] **Step 1: Run the editor helper unit test**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`
Expected: passes or reproduces the known local Gradle worker startup failure from `docs/build-notes.md`.

- [ ] **Step 2: Run Kotlin compilation**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`
Expected: `BUILD SUCCESSFUL`, or a reproducible pre-existing build-environment failure documented with exact output.

- [ ] **Step 3: Re-read the manual behavior checklist**

Manual checklist:
- newline does not trigger needless scroll
- bottom lines remain reachable across the full paper area
- scrollbar can be touched and dragged through the full editor height
- opening an existing entry without editing does not show false unsaved changes
- metadata-only draft restores successfully

- [ ] **Step 4: Capture actual verification status in the final report**

Report:
- what was verified by command
- what was blocked by environment
- what still needs device/manual confirmation
