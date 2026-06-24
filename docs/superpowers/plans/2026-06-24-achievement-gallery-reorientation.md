# Achievement Gallery Reorientation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the nurturing-world feature stack and replace it with a single richer achievement-gallery experience that feels integrated with the diary product.

**Architecture:** Keep the database schema stable for this release, but remove all user-facing nurturing routes and shared UI. Rebuild the achievement experience around dedicated achievement-only presentation helpers and state builders so the page no longer depends on pet/island/title systems.

**Tech Stack:** Kotlin, Jetpack Compose, Room, StateFlow, GitHub Release pipeline, Gradle unit tests

---

## File Structure

### Runtime cleanup slice

- Modify: `app/src/main/java/com/diary/app/DiaryApplication.kt`
- Modify: `app/src/main/java/com/diary/app/data/CrossSystemManager.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`

### Achievement redesign slice

- Modify: `app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/achievement/AchievementViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/data/AchievementModels.kt`
- Modify: `app/src/main/java/com/diary/app/data/UnifiedAchievementSeedData.kt`
- Create: `app/src/main/java/com/diary/app/ui/achievement/AchievementGalleryState.kt`
- Create: `app/src/test/java/com/diary/app/ui/achievement/AchievementGalleryStateTest.kt`

### Version / release slice

- Modify: `app/build.gradle.kts`

---

### Task 1: Remove user-facing nurturing entry points

**Files:**
- Modify: `app/src/main/java/com/diary/app/DiaryApplication.kt`
- Modify: `app/src/main/java/com/diary/app/data/CrossSystemManager.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`

- [ ] Step 1: Remove pet reminder initialization from `DiaryApplication`
- [ ] Step 2: Remove `Pet`, `Island`, `IslandTimeline`, and `TitleWall` routes from `DiaryNavHost`
- [ ] Step 3: Remove nurturing callbacks and section content from `ToolsScreen`
- [ ] Step 4: Shrink `CrossSystemManager` to achievement-only shared state
- [ ] Step 5: Build the app module compile target that covers the modified navigation path

### Task 2: Add failing tests for new achievement gallery state behavior

**Files:**
- Create: `app/src/test/java/com/diary/app/ui/achievement/AchievementGalleryStateTest.kt`
- Create: `app/src/main/java/com/diary/app/ui/achievement/AchievementGalleryState.kt`

- [ ] Step 1: Add a failing test for "recent unlocks are the newest unlocked achievements"
- [ ] Step 2: Add a failing test for "near completion focuses on locked achievements with highest progress"
- [ ] Step 3: Add a failing test for "state filter hides hidden locked names while keeping them discoverable"
- [ ] Step 4: Run the focused achievement gallery test target and verify it fails for missing implementation

### Task 3: Implement achievement-only gallery state helpers

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/achievement/AchievementGalleryState.kt`
- Test: `app/src/test/java/com/diary/app/ui/achievement/AchievementGalleryStateTest.kt`

- [ ] Step 1: Implement pure builders for recent unlocks, near completion, state filtering, and hero summary
- [ ] Step 2: Keep all logic pure and UI-independent
- [ ] Step 3: Re-run the focused tests and verify they pass

### Task 4: Redesign the achievement screen around the new gallery state

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/achievement/AchievementViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/data/AchievementModels.kt`
- Modify: `app/src/main/java/com/diary/app/data/UnifiedAchievementSeedData.kt`

- [ ] Step 1: Remove all nurturing imports, callbacks, and shared-state dependencies from `AchievementScreen`
- [ ] Step 2: Add explicit state filters in the view model for all / unlocked / near completion / hidden
- [ ] Step 3: Refactor the top hero, recent unlock strip, filters, grid, and detail sheet to match the gallery direction
- [ ] Step 4: Replace emoji-forward category semantics with calmer archive-oriented copy in models/seed data where needed
- [ ] Step 5: Re-run focused tests for the gallery state and any affected achievement tests

### Task 5: Verification, version bump, and release

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] Step 1: Run achievement-focused unit tests plus any regression tests touched by this work
- [ ] Step 2: Run `assembleExperimentalRelease`
- [ ] Step 3: Bump experimental version for the new release
- [ ] Step 4: Commit the implementation
- [ ] Step 5: Push branch, tag release, and publish a fresh GitHub experimental release with the APK asset
