# Ambient Sound Stability Design

## Goal
Stabilize the ambient sound feature so playback, stop, volume, favorites, mini bar, and fullscreen player all stay in sync and can be used repeatedly without UI corruption.

## Root Cause Summary
- Playback state is currently split across `AmbientSoundPlayer`, `AmbientSoundViewModel`, `AmbientSoundScreen`, and `AmbientSoundMiniBar`.
- `AmbientSoundMiniBar` directly manipulates the singleton player, bypassing the screen state flow.
- `AmbientSoundScreen` keeps fullscreen visibility in local Compose state, so stop/replay can leave the UI out of sync with the real playback session.
- `AmbientSoundViewModel` restores `last_track_id` by replaying audio during initialization, which can restart playback when the page is re-created instead of simply syncing to the existing session.

## Requirements
1. Stop must fully end the session and allow the next play action to start normally.
2. Browse screen, fullscreen player, mini bar, and notification controls must reflect one shared playback state.
3. Re-entering the ambient sound screen must sync to an existing session instead of restarting it.
4. Fullscreen visibility must close automatically when playback ends.
5. Volume, favorite, meander, sleep timer, and recent-played state must continue to work after the refactor.

## Recommended Approach

### Shared ViewModel Ownership
- Create one shared `AmbientSoundViewModel` at `DiaryNavHost` scope.
- Pass that shared instance into both `AmbientSoundScreen` and `AmbientSoundMiniBar`.
- Keep `AmbientSoundPlayer` as the execution layer only; UI composables should no longer call it directly.

### Single UI State Source
- Add fullscreen visibility into `AmbientSoundState`.
- Replace `showFullPlayer` local state with `state.isFullscreenPlayerVisible`.
- Add `showFullscreenPlayer()` and `hideFullscreenPlayer()` to the ViewModel.
- Centralize player-to-state synchronization in one helper so play, pause, resume, stop, and service callbacks all update the same state shape.

### Session Restore Behavior
- During ViewModel initialization:
  - Load favorites, recents, saved volume, saved category, and meander preference.
  - If a player session already exists, sync to that session only.
  - Only restore `last_track_id` into playback when there is no active player session.

### Stop and Replay Stability
- `stop()` must:
  - stop the player
  - clear `last_track_id`
  - reset fullscreen visibility
  - cancel sleep timer and progress updates
  - sync state from the ended session
- `togglePlay(track)` should route through one decision path:
  - same track + active session + playing => pause
  - same track + active session + paused => resume
  - different track or no session => play new track

## Testing Strategy
- Add pure logic tests for:
  - restore decision when a session already exists
  - fullscreen auto-close when session ends
  - player snapshot syncing into UI state
- Run targeted ambient-sound unit tests first.
- Run Kotlin compilation for the experimental debug variant after refactor.

## Out of Scope
- Replacing the audio backend
- Redesigning track metadata
- Expanding notification features beyond keeping them in sync with the shared state
