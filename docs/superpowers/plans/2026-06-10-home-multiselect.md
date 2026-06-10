# Home Multi-Select Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add long-press multi-select on the home screen's selected-date diary list so users can batch favorite or delete entries without entering each detail page.

**Architecture:** Keep the existing home layout intact and activate the already-seeded multi-select state in `HomeScreen`. Add a small pure logic helper for selection state transitions, expand `HomeViewModel` with batch favorite/delete methods that reuse existing trash behavior, and surface a compact top action bar plus delete confirmation dialog.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android ViewModel, Room DAO, JUnit4

---

### Task 1: Add selection state helper and test it first

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/home/HomeMultiSelectState.kt`
- Create: `app/src/test/java/com/diary/app/ui/home/HomeMultiSelectStateTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMultiSelectStateTest {

    @Test
    fun `startSelection enables multiselect and selects first id`() {
        val state = HomeMultiSelectState.startSelection(42L)

        assertTrue(state.isEnabled)
        assertEquals(setOf(42L), state.selectedIds)
    }

    @Test
    fun `toggleSelection adds and removes ids while staying enabled`() {
        val state = HomeMultiSelectState.startSelection(1L)

        val afterAdd = state.toggleSelection(2L)
        val afterRemove = afterAdd.toggleSelection(1L)

        assertTrue(afterAdd.selectedIds.containsAll(setOf(1L, 2L)))
        assertEquals(setOf(2L), afterRemove.selectedIds)
        assertTrue(afterRemove.isEnabled)
    }

    @Test
    fun `clearSelection disables multiselect and removes all ids`() {
        val state = HomeMultiSelectState.startSelection(5L).toggleSelection(6L)

        val cleared = state.clearSelection()

        assertFalse(cleared.isEnabled)
        assertTrue(cleared.selectedIds.isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.home.HomeMultiSelectStateTest" --rerun-tasks`
Expected: FAIL with unresolved reference errors for `HomeMultiSelectState`

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.diary.app.ui.home

data class HomeMultiSelectState(
    val isEnabled: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    fun toggleSelection(id: Long): HomeMultiSelectState {
        val nextIds = selectedIds.toMutableSet().apply {
            if (!add(id)) {
                remove(id)
            }
        }
        return copy(selectedIds = nextIds)
    }

    fun clearSelection(): HomeMultiSelectState = HomeMultiSelectState()

    companion object {
        fun startSelection(id: Long): HomeMultiSelectState {
            return HomeMultiSelectState(
                isEnabled = true,
                selectedIds = setOf(id)
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.home.HomeMultiSelectStateTest" --rerun-tasks`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/home/HomeMultiSelectState.kt app/src/test/java/com/diary/app/ui/home/HomeMultiSelectStateTest.kt
git commit -m "test: add home multiselect state coverage"
```

### Task 2: Add batch operations to the home view model

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Write the failing test**

Because `HomeViewModel` is tightly coupled to `DiaryApplication` and Room, first add a small internal helper in the same file that converts a list of fetched entries into trash copies and iterates deletes. Cover it with a pure JVM test before wiring the ViewModel method.

```kotlin
package com.diary.app.ui.home

import com.diary.app.data.DiaryEntry
import com.diary.app.data.TrashEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDeleteMapperTest {

    @Test
    fun `toTrashEntries preserves ids and favorite state`() {
        val entries = listOf(
            DiaryEntry(
                id = 10L,
                title = "A",
                content = "c",
                plainText = "p",
                moodLevel = 3,
                weather = "sunny",
                location = null,
                latitude = null,
                longitude = null,
                isFavorite = true,
                createdAt = 1L,
                updatedAt = 2L
            )
        )

        val trash = entries.map(::toTrashEntry)

        assertEquals(10L, trash.first().originalId)
        assertEquals(true, trash.first().isFavorite)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.home.HomeDeleteMapperTest" --rerun-tasks`
Expected: FAIL with unresolved reference for `toTrashEntry`

- [ ] **Step 3: Write minimal implementation**

Add an internal top-level helper in `HomeViewModel.kt`:

```kotlin
internal fun toTrashEntry(entry: DiaryEntry): TrashEntry {
    return TrashEntry(
        originalId = entry.id,
        title = entry.title,
        content = entry.content,
        plainText = entry.plainText,
        moodLevel = entry.moodLevel,
        weather = entry.weather,
        location = entry.location,
        latitude = entry.latitude,
        longitude = entry.longitude,
        isFavorite = entry.isFavorite,
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt
    )
}
```

Then update `deleteEntry`, `deleteEntryById`, and add:

```kotlin
fun favoriteEntries(ids: Set<Long>) {
    if (ids.isEmpty()) return
    viewModelScope.launch {
        ids.forEach { id ->
            dao.toggleFavorite(id, true)
        }
    }
}

fun deleteEntries(ids: Set<Long>) {
    if (ids.isEmpty()) return
    viewModelScope.launch {
        ids.forEach { id ->
            val entry = dao.getEntryById(id) ?: return@forEach
            dao.insertTrashEntry(toTrashEntry(entry))
            dao.deleteEntryWithTags(entry)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.home.HomeDeleteMapperTest" --rerun-tasks`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt app/src/test/java/com/diary/app/ui/home/HomeDeleteMapperTest.kt
git commit -m "feat: add home batch diary actions"
```

### Task 3: Wire the home screen multi-select UI

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Write the failing UI test or logic seam**

If Compose UI tests are not already set up for this screen, add a small pure helper to `HomeMultiSelectState.kt` first and extend its test with the deselect edge case used by the UI:

```kotlin
@Test
fun `toggleSelection can leave empty selection while staying enabled`() {
    val state = HomeMultiSelectState.startSelection(7L)

    val result = state.toggleSelection(7L)

    assertTrue(result.isEnabled)
    assertTrue(result.selectedIds.isEmpty())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests "com.diary.app.ui.home.HomeMultiSelectStateTest" --rerun-tasks`
Expected: FAIL if helper currently disables mode on empty selection

- [ ] **Step 3: Write minimal implementation**

Update `HomeScreen.kt` to:

```kotlin
var multiSelectState by remember { mutableStateOf(HomeMultiSelectState()) }
var showDeleteConfirm by remember { mutableStateOf(false) }

LaunchedEffect(selectedDate) {
    multiSelectState = HomeMultiSelectState()
}
```

Replace normal header with a conditional action bar:

```kotlin
SelectedDateHeader(
    date = currentSelectedDate,
    entryCount = selectedEntries.size,
    multiSelectState = multiSelectState,
    onFavoriteSelected = {
        viewModel.favoriteEntries(multiSelectState.selectedIds)
        multiSelectState = HomeMultiSelectState()
    },
    onDeleteSelected = { showDeleteConfirm = true },
    onCancelMultiSelect = { multiSelectState = HomeMultiSelectState() }
)
```

Update card click handlers:

```kotlin
onClick = {
    haptic.click()
    if (multiSelectState.isEnabled) {
        multiSelectState = multiSelectState.toggleSelection(entry.id)
    } else {
        onNavigateToDetail(entry.id)
    }
},
onLongClick = {
    haptic.click()
    multiSelectState = if (multiSelectState.isEnabled) {
        multiSelectState.toggleSelection(entry.id)
    } else {
        HomeMultiSelectState.startSelection(entry.id)
    }
}
```

Add a selected visual treatment inside `EntryCard` via an `isSelected` flag, and render a compact check badge plus highlighted border/background tint when selected.

Add delete confirmation dialog:

```kotlin
if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.deleteEntries(multiSelectState.selectedIds)
                    multiSelectState = HomeMultiSelectState()
                    showDeleteConfirm = false
                }
            ) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
        },
        title = { Text("删除选中的日记？") },
        text = { Text("这些日记会被移入回收站。") }
    )
}
```

- [ ] **Step 4: Run focused verification**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin --rerun-tasks`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/home/HomeScreen.kt app/src/main/java/com/diary/app/ui/home/HomeMultiSelectState.kt app/src/test/java/com/diary/app/ui/home/HomeMultiSelectStateTest.kt
git commit -m "feat: add home diary multiselect ui"
```

### Task 4: Build and release the experimental update

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `docs/build-notes.md`

- [ ] **Step 1: Bump experimental version**

Increment the experimental `versionName` and `versionCode` in `app/build.gradle.kts`, following the current release pattern.

- [ ] **Step 2: Update release notes**

Append a short entry to `docs/build-notes.md` covering:

```markdown
## vX.YY.ZZ-experimental
- 首页支持长按进入多选
- 可对当天多篇日记批量收藏
- 可对当天多篇日记批量删除并移入回收站
```

- [ ] **Step 3: Run release builds**

Run: `.\gradlew.bat :app:clean :app:compileExperimentalDebugKotlin --rerun-tasks`
Expected: BUILD SUCCESSFUL

Run: `.\gradlew.bat :app:assembleExperimentalRelease --rerun-tasks`
Expected: BUILD SUCCESSFUL and APK/AAB generated

- [ ] **Step 4: Commit and push**

```bash
git add app/build.gradle.kts docs/build-notes.md app/src/main/java/com/diary/app/ui/home/HomeScreen.kt app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt app/src/main/java/com/diary/app/ui/home/HomeMultiSelectState.kt app/src/test/java/com/diary/app/ui/home/HomeMultiSelectStateTest.kt app/src/test/java/com/diary/app/ui/home/HomeDeleteMapperTest.kt docs/superpowers/specs/2026-06-10-home-multiselect-design.md docs/superpowers/plans/2026-06-10-home-multiselect.md
git commit -m "feat: add home diary batch actions"
git push
```

- [ ] **Step 5: Create release**

Run the same release flow already used in this repository for experimental builds, ensuring the new version tag points to the latest commit and references the updated release notes.
