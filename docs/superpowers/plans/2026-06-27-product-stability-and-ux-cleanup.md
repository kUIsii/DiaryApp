# Product Stability And UX Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize the existing DiaryApp experience without removing major features by fixing verified behavior bugs first, then cleaning up high-friction UI flows and large-screen structure.

**Architecture:** The cleanup is split into four phases so behavior fixes land before screen refactors. Phase 1 addresses correctness and stability regressions that already have failing tests or direct source evidence. Phase 2 improves homepage and todo interaction flows while keeping the current visual language. Phase 3 extracts oversized screens and navigation wiring into smaller units. Phase 4 adds regression coverage and runs final verification across both product flavors.

**Tech Stack:** Kotlin, Jetpack Compose, Android ViewModel, Room, WebView/Quill editor, Gradle, JUnit4

---

## File Structure

**Phase 1 files**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/java/com/diary/app/data/DiaryDatabase.kt`
- Modify: `app/src/main/java/com/diary/app/DiaryApplication.kt`
- Modify: `app/src/main/java/com/diary/app/MainActivity.kt`
- Modify: `app/src/main/java/com/diary/app/ui/map/MapViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/update/ChangelogScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
- Modify: `app/src/test/java/com/diary/app/startup/StartupPerformanceSourceTest.kt`
- Modify: `app/src/test/java/com/diary/app/ui/map/MapViewModelTest.kt`
- Modify: `app/src/test/java/com/diary/app/update/ReleaseVersionUtilsTest.kt`
- Modify: `app/src/test/java/com/diary/app/ui/experimental/ExperimentalFeatureLogicTest.kt`

**Phase 2 files**
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoViewModel.kt`
- Create: `app/src/test/java/com/diary/app/ui/home/HomeScreenLogicTest.kt`
- Create: `app/src/test/java/com/diary/app/ui/todo/TodoScreenStateTest.kt`

**Phase 3 files**
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt`
- Create: `app/src/main/java/com/diary/app/ui/navigation/MainScreenSwipeController.kt`
- Create: `app/src/main/java/com/diary/app/ui/editor/EditorChromeState.kt`
- Create: `app/src/main/java/com/diary/app/ui/home/HomeFeedSections.kt`
- Create: `app/src/main/java/com/diary/app/ui/todo/TodoDialogs.kt`

**Phase 4 files**
- Modify: `app/src/main/java/com/diary/app/data/DiaryDao.kt`
- Create: `app/src/test/java/com/diary/app/data/DiaryDaoProjectionTest.kt`
- Create: `app/src/test/java/com/diary/app/ui/navigation/MainScreenSwipeControllerTest.kt`

### Task 1: Phase 1 Verified Behavior Fixes

**Files:**
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
- Modify: `app/src/test/java/com/diary/app/startup/StartupPerformanceSourceTest.kt`
- Modify: `app/src/test/java/com/diary/app/ui/map/MapViewModelTest.kt`
- Modify: `app/src/test/java/com/diary/app/update/ReleaseVersionUtilsTest.kt`
- Modify: `app/src/test/java/com/diary/app/ui/experimental/ExperimentalFeatureLogicTest.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Modify: `app/src/main/java/com/diary/app/data/DiaryDatabase.kt`
- Modify: `app/src/main/java/com/diary/app/DiaryApplication.kt`
- Modify: `app/src/main/java/com/diary/app/MainActivity.kt`
- Modify: `app/src/main/java/com/diary/app/ui/map/MapViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/update/ChangelogScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`

- [ ] **Step 1: Refresh failing tests so they describe the intended Phase 1 behavior**

Add or update tests so they assert:

```kotlin
@Test
fun `keyboard closing keeps toolbar open only when user explicitly locked it`() {
    assertEquals(
        true,
        shouldAutoHideToolbarOnKeyboardHidden(
            activeCategory = -1,
            keepToolbarOpen = false
        )
    )
    assertEquals(
        false,
        shouldAutoHideToolbarOnKeyboardHidden(
            activeCategory = -1,
            keepToolbarOpen = true
        )
    )
}

@Test
fun `extract city uses final segment for two part locations`() {
    assertEquals("Tokyo", extractCityFromLocation("Shibuya, Tokyo"))
}
```

- [ ] **Step 2: Run focused Phase 1 tests to verify they fail for the right reasons**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest" --tests "com.diary.app.startup.StartupPerformanceSourceTest" --tests "com.diary.app.ui.map.MapViewModelTest" --tests "com.diary.app.update.ReleaseVersionUtilsTest" --tests "com.diary.app.ui.experimental.ExperimentalFeatureLogicTest"
```

Expected: FAIL due to current startup eager-open path, editor toolbar/dirty-state behavior, map parsing, release sorting, and swipe toggle behavior.

- [ ] **Step 3: Implement minimal Phase 1 fixes**

Target behaviors:

```kotlin
internal fun shouldAutoHideToolbarOnKeyboardHidden(
    activeCategory: Int,
    keepToolbarOpen: Boolean
): Boolean {
    return activeCategory < 0 && !keepToolbarOpen
}

internal fun extractCityFromLocation(location: String): String {
    val parts = location.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return when {
        parts.size >= 3 -> parts[parts.size - 2]
        parts.size == 2 -> parts[1]
        parts.size == 1 -> parts[0]
        else -> ""
    }
}
```

Also:
- stop forcing writable DB open during construction
- stop eager DAO access in `DiaryApplication` startup path
- add a startup state gate in `MainActivity`
- remove “current version first” sorting from changelog ordering
- wire main-screen swipe enablement from `ExperimentalFeaturesState` instead of hardcoded `true`

- [ ] **Step 4: Re-run focused Phase 1 tests**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.editor.EditorUtilsTest" --tests "com.diary.app.startup.StartupPerformanceSourceTest" --tests "com.diary.app.ui.map.MapViewModelTest" --tests "com.diary.app.update.ReleaseVersionUtilsTest" --tests "com.diary.app.ui.experimental.ExperimentalFeatureLogicTest"
```

Expected: PASS

- [ ] **Step 5: Commit Phase 1**

```bash
git add app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/main/java/com/diary/app/data/DiaryDatabase.kt app/src/main/java/com/diary/app/DiaryApplication.kt app/src/main/java/com/diary/app/MainActivity.kt app/src/main/java/com/diary/app/ui/map/MapViewModel.kt app/src/main/java/com/diary/app/update/ChangelogScreen.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt app/src/test/java/com/diary/app/startup/StartupPerformanceSourceTest.kt app/src/test/java/com/diary/app/ui/map/MapViewModelTest.kt app/src/test/java/com/diary/app/update/ReleaseVersionUtilsTest.kt app/src/test/java/com/diary/app/ui/experimental/ExperimentalFeatureLogicTest.kt
git commit -m "fix: stabilize startup editor and navigation behavior"
```

### Task 2: Phase 2 Homepage And Todo Flow Cleanup

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoViewModel.kt`
- Create: `app/src/test/java/com/diary/app/ui/home/HomeScreenLogicTest.kt`
- Create: `app/src/test/java/com/diary/app/ui/todo/TodoScreenStateTest.kt`

- [ ] **Step 1: Write failing logic tests for homepage and todo state cleanup**

Cover:
- homepage quick shortcut `"todo"` navigates to todo route
- homepage shows an explicit empty state when search query has zero results
- homepage random review and on-this-day data can refresh from observable state
- todo screen modal state does not keep stale selected habit after dismiss

- [ ] **Step 2: Run targeted tests and confirm failures**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.home.*" --tests "com.diary.app.ui.todo.*"
```

Expected: FAIL for new logic tests.

- [ ] **Step 3: Implement homepage and todo interaction cleanup**

Implement:
- move random review / on-this-day loading into `HomeViewModel` state
- add explicit empty search result rendering
- fix quick shortcut route mapping
- centralize todo detail/dialog dismissal state
- keep reminder side effects out of repeated UI-triggered paths

- [ ] **Step 4: Re-run homepage and todo tests**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.home.*" --tests "com.diary.app.ui.todo.*"
```

Expected: PASS

- [ ] **Step 5: Commit Phase 2**

```bash
git add app/src/main/java/com/diary/app/ui/home/HomeScreen.kt app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt app/src/main/java/com/diary/app/ui/todo/TodoViewModel.kt app/src/test/java/com/diary/app/ui/home/HomeScreenLogicTest.kt app/src/test/java/com/diary/app/ui/todo/TodoScreenStateTest.kt
git commit -m "refactor: simplify homepage and todo interaction flows"
```

### Task 3: Phase 3 Screen And Navigation Decomposition

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt`
- Create: `app/src/main/java/com/diary/app/ui/navigation/MainScreenSwipeController.kt`
- Create: `app/src/main/java/com/diary/app/ui/editor/EditorChromeState.kt`
- Create: `app/src/main/java/com/diary/app/ui/home/HomeFeedSections.kt`
- Create: `app/src/main/java/com/diary/app/ui/todo/TodoDialogs.kt`

- [ ] **Step 1: Write failing tests for extracted navigation and state helpers**

Create tests for:
- swipe controller route selection
- editor chrome state transitions
- home feed section gating
- todo dialog reducer behavior

- [ ] **Step 2: Run extracted-helper tests**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.navigation.*" --tests "com.diary.app.ui.editor.*" --tests "com.diary.app.ui.home.*" --tests "com.diary.app.ui.todo.*"
```

Expected: FAIL for new helpers before extraction is implemented.

- [ ] **Step 3: Extract screen helpers with no intentional feature removal**

Implement:
- move swipe-target logic into `MainScreenSwipeController`
- move editor toolbar/unsaved/startup-facing state transitions into `EditorChromeState`
- move home feed rendering sections into `HomeFeedSections`
- move todo dialog rendering and state helpers into `TodoDialogs`

- [ ] **Step 4: Re-run helper and screen tests**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.ui.navigation.*" --tests "com.diary.app.ui.editor.*" --tests "com.diary.app.ui.home.*" --tests "com.diary.app.ui.todo.*"
```

Expected: PASS

- [ ] **Step 5: Commit Phase 3**

```bash
git add app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/main/java/com/diary/app/ui/navigation/MainScreenSwipeController.kt app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/main/java/com/diary/app/ui/editor/EditorChromeState.kt app/src/main/java/com/diary/app/ui/home/HomeScreen.kt app/src/main/java/com/diary/app/ui/home/HomeFeedSections.kt app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt app/src/main/java/com/diary/app/ui/todo/TodoDialogs.kt
git commit -m "refactor: break large screens into focused components"
```

### Task 4: Phase 4 Data Integrity And Final Verification

**Files:**
- Modify: `app/src/main/java/com/diary/app/data/DiaryDao.kt`
- Create: `app/src/test/java/com/diary/app/data/DiaryDaoProjectionTest.kt`
- Create: `app/src/test/java/com/diary/app/ui/navigation/MainScreenSwipeControllerTest.kt`

- [ ] **Step 1: Write failing tests for DAO projections and regression helpers**

Cover:
- all `DiaryEntry` projections used in export/search include the fields they must preserve
- preview projections do not fetch unused large content when not needed

- [ ] **Step 2: Run DAO-focused tests to confirm failure**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest --tests "com.diary.app.data.*" --tests "com.diary.app.ui.navigation.MainScreenSwipeControllerTest"
```

Expected: FAIL for new projection checks before DAO cleanup.

- [ ] **Step 3: Implement DAO projection cleanup and regression coverage**

Implement:
- align `DiaryEntry` safe/export projections with actual entity fields that must round-trip
- align preview queries with preview-only columns
- keep large-content avoidance where required

- [ ] **Step 4: Run full flavor verification**

Run:

```bash
./gradlew.bat :app:testStableDebugUnitTest :app:testExperimentalDebugUnitTest
```

Expected: PASS with zero failing tests

- [ ] **Step 5: Commit Phase 4**

```bash
git add app/src/main/java/com/diary/app/data/DiaryDao.kt app/src/test/java/com/diary/app/data/DiaryDaoProjectionTest.kt app/src/test/java/com/diary/app/ui/navigation/MainScreenSwipeControllerTest.kt
git commit -m "test: lock in projection integrity and final cleanup"
```
