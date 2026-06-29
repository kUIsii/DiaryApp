# AmbientSound Redesign Implementation Plan

> **For agentic workers:** Use inline execution. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Redesign AmbientSound with single-column cards, MiniBar quick control, master volume, Meander mode, auto-restore.

**Architecture:** Modify AmbientSoundPlayer (master volume + meander), AmbientSoundViewModel (state + auto-restore), AmbientSoundScreen (UI rewrite), DiaryNavHost (MiniBar), AmbientSoundService (notification update).

**Tech Stack:** Kotlin, Jetpack Compose, MediaPlayer, SharedPreferences

---

### Task 1: AmbientSoundPlayer - Add masterVolume and meanderEnabled

**Files:** Modify `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundPlayer.kt`

- [ ] Add masterVolume + meanderEnabled fields, update applyVol(), add setMasterVolume/toggleMeander/meander loop

```kotlin
// New fields in class body:
var masterVolume: Float = 1f
    private set
var meanderEnabled: Boolean = false
    private set
private val meanderHandler = Handler(Looper.getMainLooper())
private var meanderRunnable: Runnable? = null

// Modified applyVol:
private fun applyVol(player: MediaPlayer, type: AmbientSoundType) {
    try {
        val baseVol = (synchronized(currentVolumes) { currentVolumes[type] ?: 0.5f }).let { if (ducked) it * 0.3f else it }
        val vol = baseVol * masterVolume
        player.setVolume(vol.coerceIn(0f, 1f), vol.coerceIn(0f, 1f))
    } catch (_: Exception) {}
}

// New methods:
fun setMasterVolume(v: Float) {
    masterVolume = v.coerceIn(0f, 1f)
    synchronized(players) { players.keys.toList() }.forEach { type ->
        synchronized(players) { players[type] }?.let { applyVol(it, type) }
    }
}

fun toggleMeander() {
    meanderEnabled = !meanderEnabled
    if (meanderEnabled) startMeander() else stopMeander()
}

private fun startMeander() {
    stopMeander()
    meanderRunnable = Runnable {
        if (!meanderEnabled || players.isEmpty()) return@Runnable
        synchronized(players) { players.keys.toList() }.forEach { type ->
            val phase = (System.currentTimeMillis() % 20000) / 20000f * 2f * Math.PI.toFloat()
            val seed = type.ordinal * 137.5f
            val factor = 0.5f + 0.5f * kotlin.math.sin(phase + seed)
            val meanderVol = (synchronized(currentVolumes) { currentVolumes[type] ?: 0.5f }) * factor * masterVolume
            synchronized(players) { players[type] }?.let { p ->
                try { p.setVolume(meanderVol.coerceIn(0f, 1f), meanderVol.coerceIn(0f, 1f)) } catch (_: Exception) {}
            }
        }
        meanderHandler.postDelayed(meanderRunnable!!, 100)
    }
    meanderHandler.post(meanderRunnable!!)
}

private fun stopMeander() {
    meanderRunnable?.let { meanderHandler.removeCallbacks(it) }
    meanderRunnable = null
}
```

- [ ] Verify build: `./gradlew :app:compileExperimentalDebugKotlin`

### Task 2: AmbientSoundViewModel - Add masterVolume, meanderEnabled, auto-restore

**Files:** Modify `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundViewModel.kt`

- [ ] Update AmbientSoundState to include masterVolume and meanderEnabled
- [ ] Add setMasterVolume/toggleMeander methods
- [ ] Add auto-restore in init block
- [ ] Add save in onCleared
- [ ] Remove preset-related UI state (presets from state, saveCurrentPreset, deletePreset, applyPreset — keep PRESET STORAGE code but stop calling it from UI)

```kotlin
// In AmbientSoundState:
data class AmbientSoundState(
    val activeSounds: Set<AmbientSoundType> = emptySet(),
    val volumes: Map<AmbientSoundType, Float> = emptyMap(),
    val timerOption: TimerOption = TimerOption.OFF,
    val remainingSeconds: Int = 0,
    val isSleepFading: Boolean = false,
    val masterVolume: Float = 1f,
    val meanderEnabled: Boolean = false
)

// In ViewModel:
private val prefs = application.getSharedPreferences("ambient_sound", Context.MODE_PRIVATE)

init {
    // Auto-restore
    val savedKeys = prefs.getStringSet("active_keys", emptySet()) ?: emptySet()
    if (savedKeys.isNotEmpty()) {
        val savedVolumes = mutableMapOf<AmbientSoundType, Float>()
        val savedMaster = prefs.getFloat("master_volume", 1f)
        val savedMeander = prefs.getBoolean("meander_enabled", false)
        for (key in savedKeys) {
            val type = AmbientSoundType.entries.find { it.key == key } ?: continue
            val vol = prefs.getFloat("vol_$key", 0.5f)
            savedVolumes[type] = vol
        }
        player.masterVolume = savedMaster
        if (savedMeander) player.toggleMeander()
        for ((type, vol) in savedVolumes) {
            player.play(type, vol)
        }
        _state.value = _state.value.copy(
            activeSounds = savedVolumes.keys,
            volumes = savedVolumes,
            masterVolume = savedMaster,
            meanderEnabled = savedMeander
        )
    }
}

fun setMasterVolume(v: Float) {
    player.setMasterVolume(v)
    _state.value = _state.value.copy(masterVolume = v)
}

fun toggleMeander() {
    player.toggleMeander()
    _state.value = _state.value.copy(meanderEnabled = !_state.value.meanderEnabled)
}

override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
    val s = _state.value
    prefs.edit().apply {
        putStringSet("active_keys", s.activeSounds.map { it.key }.toSet())
        for ((type, vol) in s.volumes) putFloat("vol_${type.key}", vol)
        putFloat("master_volume", s.masterVolume)
        putBoolean("meander_enabled", s.meanderEnabled)
        apply()
    }
}
```

- [ ] Remove from ViewModel: `presets` from state, `applyPreset()`, `saveCurrentPreset()`, `deletePreset()`, `PresetStorage.load/save/delete` calls
- [ ] Verify build

### Task 3: AmbientSoundScreen - Full UI rewrite

**Files:** Rewrite `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundScreen.kt`

- [ ] Full rewrite with LazyColumn, single-column cards, no presets row, master volume + meander button
- [ ] Pulse animation for playing cards

Key changes from current:
- LazyVerticalGrid(2) → LazyColumn
- SoundGridCard → SoundRowCard (horizontal layout: icon + name + play/stop button + volume slider)
- Remove presets row (LazyRow + FilterChip + Add button + save dialog)
- Add master volume slider + meander toggle in bottom area
- Pulse animation on icon when playing

- [ ] Verify build

### Task 4: DiaryNavHost - Add AmbientSoundMiniBar

**Files:** Modify `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`

- [ ] Add AmbientSoundMiniBar composable in Scaffold, between bottomBar and content
- [ ] Show only when player has active sounds
- [ ] Animated visibility with slide from bottom

```kotlin
// Import:
import com.diary.app.ui.ambientsound.AmbientSoundMiniBar

// In Scaffold, after bottomBar:
if (showBottomBar) {
    DiaryBottomNavigationBar(...)
}
AmbientSoundMiniBar(...)
```

The composable needs:
- Current active sounds text
- Master volume slider
- Pause/resume button
- Stop all button
- Navigate to full screen button

### Task 5: AmbientSoundService - Notification update

**Files:** Modify `app/src/main/java/com/diary/app/ui/ambientsound/AmbientSoundService.kt`

- [ ] Update notification to show master volume (if paused/playing)
- [ ] Add notification action for meander toggle (optional, maybe skip)

### Task 6: Build and Release

- [ ] Bump version in build.gradle.kts
- [ ] Build APK
- [ ] Commit, push, create release

### Verification
- [ ] Build: `./gradlew assembleExperimentalDebug` — 0 errors
- [ ] APK exists at `app/build/outputs/apk/experimental/debug/app-experimental-debug.apk`
- [ ] Release created with tag `vX.X.X-experimental`
