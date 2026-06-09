# Editor Drafts And Nav Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore a usable diary editor cursor, make discard actions fully clear all draft traces, and remove the visual seam above the home bottom navigation bar.

**Architecture:** Keep the existing `WebView + Quill` editor architecture and fix the three regressions with small, targeted changes. Put draft-key rules in editor utilities so the ViewModel and tests share one source of truth, tighten editor focus restoration in the HTML/Compose bridge, and simplify bottom-bar inset handling so the surface does not render an extra strip.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, Quill editor, JUnit4

---

### Task 1: Lock Down Draft Cleanup Behavior

**Files:**
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt`

- [ ] **Step 1: Write the failing test**

Add utility coverage for clearing both autosave keys and list-backed draft ids when discarding or saving:

```kotlin
@Test
fun `draft list key is resolved from id`() {
    assertEquals("draft_item_abc", draftListItemKey("abc"))
}

@Test
fun `draft keys to clear include autosave and current entry`() {
    assertEquals(setOf("draft_new", "draft_42"), draftKeysToClear(42L))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest"`
Expected: FAIL because `draftListItemKey` is not defined yet.

- [ ] **Step 3: Write minimal implementation**

Add shared key helpers in `EditorUtils.kt`, then update `EditorViewModel` to use them for save/load/delete/clear flows so discard and save clear both autosave and draft-box state.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt
git commit -m "fix: unify editor draft cleanup"
```

### Task 2: Restore Editor Focus And Cursor

**Files:**
- Modify: `app/src/main/assets/editor.html`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Test: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`

- [ ] **Step 1: Write the failing test**

Add an editor asset regression check that expects the focus helper to normalize empty selections before restoring:

```kotlin
@Test
fun `editor asset restores a usable cursor on focus`() {
    val html = File("src/main/assets/editor.html").readText()

    assertTrue(html.contains("ensureSelection()"))
    assertTrue(html.contains("focusEditorAtEnd()"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest"`
Expected: FAIL because the new helpers are not present in `editor.html`.

- [ ] **Step 3: Write minimal implementation**

Add explicit selection/focus helpers in `editor.html` and call them from `EditorScreen.kt` after page load and after metadata overlays dismiss so Quill always has a valid cursor before typing.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/editor.html app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt
git commit -m "fix: restore editor cursor focus"
```

### Task 3: Remove The Bottom Navigation Seam

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`

- [ ] **Step 1: Write the smallest regression assertion**

Use the existing UI code path and keep the change scoped to the bottom bar container: replace inset padding that expands the surface with a fixed bar body plus a matching inset spacer.

- [ ] **Step 2: Implement the minimal layout fix**

Render the navigation row at `76.dp`, then draw a separate bottom spacer using the same surface color so the system navigation area does not create a second visible band above the bar.

- [ ] **Step 3: Run targeted verification**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt
git commit -m "fix: clean up home bottom navigation spacing"
```

### Task 4: Final Verification

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/assets/editor.html`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`

- [ ] **Step 1: Run focused unit coverage**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest"`
Expected: PASS

- [ ] **Step 2: Run compilation verification**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Review the diff**

Run: `git diff -- app/src/main/assets/editor.html app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
Expected: Only the intended editor, draft, and navigation changes appear.
