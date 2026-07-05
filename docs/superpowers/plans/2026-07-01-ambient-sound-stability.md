# Ambient Sound Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize ambient sound playback so stop/replay, fullscreen, mini bar, and background controls all stay in sync.

**Architecture:** Hoist one shared `AmbientSoundViewModel` to `DiaryNavHost`, move fullscreen visibility into `AmbientSoundState`, and make every UI surface consume the same state instead of touching `AmbientSoundPlayer` directly. Keep `AmbientSoundPlayer` as the playback engine and centralize player-to-state syncing inside the ViewModel.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, AndroidViewModel, MediaPlayer, JUnit4

---

### File Structure

- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundUiLogic.kt`
  - Add pure state-sync helpers and restore-decision logic for unit testing.
- Modify: `app/src/test/java/com/diary/app/ui/ambientsound/AmbientSoundUiLogicTest.kt`
  - Add regression coverage for restore behavior and fullscreen/session sync.
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundViewModel.kt`
  - Add shared fullscreen state, centralized player sync, safer restore logic, and stable stop/replay behavior.
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundScreen.kt`
  - Remove local fullscreen state and consume ViewModel-owned state only.
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundMiniBar.kt`
  - Make mini bar state-driven instead of singleton-player-driven.
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
  - Create one shared `AmbientSoundViewModel` and pass it to both ambient sound UI surfaces.

### Task 1: Lock the shared-state rules in tests

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundUiLogic.kt`
- Test: `app/src/test/java/com/diary/app/ui/ambientsound/AmbientSoundUiLogicTest.kt`

- [ ] **Step 1: Write the failing tests**

Add tests for:
- `shouldRestoreAmbientTrack(savedTrackId = "s1", hasActiveSession = true)` returning `false`
- syncing a no-session snapshot forcing `isFullscreenPlayerVisible` to `false`
- syncing an active-session snapshot carrying track, volume, play state, duration, and progress into `AmbientSoundState`

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests "com.diary.app.ui.ambientsound.AmbientSoundUiLogicTest" --no-daemon --max-workers=1`

Expected: ambient sound UI logic test fails because the new helper functions and expectations do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add pure helper types/functions in `AmbientSoundUiLogic.kt`:
- `AmbientPlayerSnapshot`
- `shouldRestoreAmbientTrack(...)`
- `syncAmbientStateWithPlayer(...)`

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testExperimentalDebugUnitTest --tests "com.diary.app.ui.ambientsound.AmbientSoundUiLogicTest" --no-daemon --max-workers=1`

Expected: ambient sound UI logic tests pass.

### Task 2: Refactor the ViewModel into the single state owner

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundViewModel.kt`

- [ ] **Step 1: Update state shape**

Add `isFullscreenPlayerVisible: Boolean = false` to `AmbientSoundState`.

- [ ] **Step 2: Centralize player sync**

Implement a helper inside the ViewModel that reads the current player snapshot and writes one synchronized `AmbientSoundState`.

- [ ] **Step 3: Fix restore behavior**

During `init`, only replay `last_track_id` when there is no active player session. If a session already exists, sync to it without replaying.

- [ ] **Step 4: Fix stop and replay behavior**

Ensure `stop()` clears fullscreen state, cancels timers/jobs, removes `last_track_id`, and then synchronizes the now-ended session state.

- [ ] **Step 5: Re-run the ambient tests**

Run: `./gradlew testExperimentalDebugUnitTest --tests "com.diary.app.ui.ambientsound.*" --no-daemon --max-workers=1`

Expected: all ambient sound unit tests pass.

### Task 3: Make screen and mini bar consume the shared ViewModel state

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundMiniBar.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`

- [ ] **Step 1: Share the ViewModel**

Create a single `AmbientSoundViewModel` in `DiaryNavHost` and pass it to both `AmbientSoundScreen` and `AmbientSoundMiniBar`.

- [ ] **Step 2: Remove local fullscreen state**

Replace `showFullPlayer` in `AmbientSoundScreen` with `state.isFullscreenPlayerVisible` and wire all open/close actions through the ViewModel.

- [ ] **Step 3: Make the mini bar state-driven**

Pass `AmbientSoundState` and callbacks into `AmbientSoundMiniBar` so play/pause/stop/volume changes all go through the ViewModel.

- [ ] **Step 4: Restore missing stop affordance in fullscreen**

Render an actual stop action in the fullscreen player and wire it through `viewModel.stop()`.

- [ ] **Step 5: Compile the app**

Run: `./gradlew compileExperimentalDebugKotlin --no-daemon --max-workers=1`

Expected: Kotlin compilation succeeds for the experimental debug variant.

### Task 4: Final verification

**Files:**
- No code changes expected

- [ ] **Step 1: Run targeted ambient tests**

Run: `./gradlew testExperimentalDebugUnitTest --tests "com.diary.app.ui.ambientsound.*" --no-daemon --max-workers=1`

Expected: ambient sound tests pass.

- [ ] **Step 2: Run debug Kotlin compilation**

Run: `./gradlew compileExperimentalDebugKotlin --no-daemon --max-workers=1`

Expected: build succeeds with exit code 0.

- [ ] **Step 3: Review the diff**

Run: `git diff -- app/src/main/java/com/diary/app/ui/ambientsound app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/test/java/com/diary/app/ui/ambientsound docs/superpowers/specs docs/superpowers/plans`

Expected: diff only contains the shared-state ambient sound stabilization work and matching docs.
