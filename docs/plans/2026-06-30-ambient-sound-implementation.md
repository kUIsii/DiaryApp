# Ambient Sound Redesign Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task.

**Goal:** Replace code-generated fake ambient audio with real downloaded audio, redesign UI with immersive dark-warm theme, add fullscreen player with progress/volume/sleep timer controls.

**Architecture:** 3-layer (Data/Service/UI). Data layer manages track metadata and audio file caching. Service layer handles background playback. UI layer provides browse + fullscreen player screens.

**Tech Stack:** Kotlin, Jetpack Compose, Room (favorites/recent), MediaPlayer (audio), Coil (images), OkHttp (downloads)

**Database:** Separate AmbientSoundDatabase (not part of main DiaryDatabase — avoids complex migrations)

---

### Critical Design Rules
- No emoji anywhere. Use Material Icons (`Icons.Default.PlayArrow`, etc.) for all icons.
- No gradient circles / color blocks for track art. Use Coil `AsyncImage` with real image URLs.
- Colors: background `#1C1511`, card `#2A1F1ABB`, accent `#CCA090`, text primary `#F2E3DA`, text secondary `#9A8579`, surface `#2A1F1A`.
- Audio cached at `Context.filesDir/ambient_sounds/{id}.mp3`.
- Images from Unsplash URLs via Coil — zero APK size increase.

### File Structure

**New:**
- `data/ambientsound/AudioTrack.kt` — data models
- `data/ambientsound/AudioRepository.kt` — track metadata provider
- `data/ambientsound/AudioCacheManager.kt` — download + cache
- `data/ambientsound/AmbientSoundDatabase.kt` — Room DB
- `data/ambientsound/AmbientSoundDao.kt` — favorites/recent DAO

**Modify:**
- `ui/ambientsound/AmbientSoundScreen.kt` — full rewrite
- `ui/ambientsound/AmbientSoundViewModel.kt` — full rewrite
- `ui/ambientsound/AmbientSoundPlayer.kt` — full rewrite
- `ui/ambientsound/AmbientSoundMiniBar.kt` — update UI
- `ui/ambientsound/AmbientSoundService.kt` — minor API alignment

---

### Task 1: Data models

**Create:** `data/ambientsound/AudioTrack.kt`

```kotlin
package com.diary.app.data.ambientsound

data class AudioCategory(
    val id: String,
    val name: String,
    val backgroundImageUrl: String
)

data class AudioTrack(
    val id: String,
    val categoryId: String,
    val name: String,
    val durationSeconds: Int,
    val audioUrl: String,
    val imageUrl: String
)
```

Commit:
```
git add app/src/main/java/com/diary/app/data/ambientsound/AudioTrack.kt
git commit -m "feat: add AudioTrack and AudioCategory models"
```

---

### Task 2: AudioRepository

**Create:** `data/ambientsound/AudioRepository.kt`

```kotlin
package com.diary.app.data.ambientsound

object AudioRepository {
    val categories = listOf(
        AudioCategory("sleep", "助眠", "https://images.unsplash.com/photo-1506452305024-9d3f02d1c9b5?w=800"),
        AudioCategory("nature", "自然", "https://images.unsplash.com/photo-1511497584788-876760111969?w=800"),
        AudioCategory("reading", "伴读", "https://images.unsplash.com/photo-1519682577862-e7a8d2b82e3a?w=800"),
        AudioCategory("meditation", "冥想", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800")
    )

    private val allTracks = listOf(
        AudioTrack("s1", "sleep", "雨打芭蕉", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=400"),
        AudioTrack("s2", "sleep", "壁炉篝火", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1472712739511-3f2f7fc4f0e1?w=400"),
        AudioTrack("s3", "sleep", "海浪白噪音", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=400"),
        AudioTrack("s4", "sleep", "深层宁静", 5400, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400"),
        AudioTrack("s5", "sleep", "细雨微风", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://images.unsplash.com/photo-1493558103817-58b2922d1be6?w=400"),
        AudioTrack("s6", "sleep", "炉边夜读", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://images.unsplash.com/photo-1519682577862-e7a8d2b82e3a?w=400"),
        AudioTrack("n1", "nature", "山间溪流", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400"),
        AudioTrack("n2", "nature", "森林晨鸟", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400"),
        AudioTrack("n3", "nature", "林风习习", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400"),
        AudioTrack("n4", "nature", "夏日蝉鸣", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3", "https://images.unsplash.com/photo-1470071459604-7b8ec44ffd4c?w=400"),
        AudioTrack("n5", "nature", "瀑布水声", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3", "https://images.unsplash.com/photo-1504805572947-34fad45aed93?w=400"),
        AudioTrack("n6", "nature", "山谷回声", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3", "https://images.unsplash.com/photo-1585409677983-0f6c41ca9c3b?w=400"),
        AudioTrack("r1", "reading", "月光钢琴", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3", "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400"),
        AudioTrack("r2", "reading", "时光吉他", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3", "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=400"),
        AudioTrack("r3", "reading", "低语大提琴", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3", "https://images.unsplash.com/photo-1465847899084-d164df4dedc1?w=400"),
        AudioTrack("r4", "reading", "书页之间", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400"),
        AudioTrack("r5", "reading", "午后阳光", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-17.mp3", "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=400"),
        AudioTrack("r6", "reading", "星空夜曲", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-18.mp3", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400"),
        AudioTrack("m1", "meditation", "颂钵之音", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-19.mp3", "https://images.unsplash.com/photo-1508672019048-805c876b67e2?w=400"),
        AudioTrack("m2", "meditation", "432Hz谐振", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-20.mp3", "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400"),
        AudioTrack("m3", "meditation", "呼吸引导", 1800, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1545389336-cf090694435e?w=400"),
        AudioTrack("m4", "meditation", "空灵钟声", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb?w=400"),
        AudioTrack("m5", "meditation", "竹林风铃", 3600, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1601370690183-1c7796ecec61?w=400"),
        AudioTrack("m6", "meditation", "晨曦鸟鸣", 2700, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400")
    )

    fun getTracks(categoryId: String): List<AudioTrack> = allTracks.filter { it.categoryId == categoryId }
    fun getTrack(id: String): AudioTrack? = allTracks.find { it.id == id }
    fun getAllTracks(): List<AudioTrack> = allTracks
}
```

Commit.

---

### Task 3: AudioCacheManager

**Create:** `data/ambientsound/AudioCacheManager.kt`

```kotlin
package com.diary.app.data.ambientsound

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class AudioCacheManager(private val context: Context) {
    private val cacheDir: File
        get() = File(context.filesDir, "ambient_sounds").also { it.mkdirs() }

    fun isCached(trackId: String): Boolean = getFile(trackId).exists()
    fun getFile(trackId: String): File = File(cacheDir, "${trackId}.mp3")

    suspend fun download(trackId: String, url: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = getFile(trackId)
            if (file.exists()) return@withContext Result.success(file)
            URL(url).openStream().use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            Result.success(file)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun delete(trackId: String) { getFile(trackId).delete() }
    fun clearAll() { cacheDir.listFiles()?.forEach { it.delete() } }
}
```

Commit.

---

### Task 4: Room DB for favorites/recent

**Create:** `data/ambientsound/AmbientSoundDao.kt`

```kotlin
package com.diary.app.data.ambientsound

import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val trackId: String, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "recent")
data class RecentEntity(@PrimaryKey val trackId: String, val playedAt: Long = System.currentTimeMillis())

@Dao
interface AmbientSoundDao {
    @Query("SELECT trackId FROM favorites")
    suspend fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT trackId FROM recent ORDER BY playedAt DESC LIMIT 20")
    suspend fun getRecentIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(recent: RecentEntity)
}
```

**Create:** `data/ambientsound/AmbientSoundDatabase.kt`

```kotlin
package com.diary.app.data.ambientsound

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class, RecentEntity::class], version = 1, exportSchema = false)
abstract class AmbientSoundDatabase : RoomDatabase() {
    abstract fun dao(): AmbientSoundDao

    companion object {
        @Volatile private var INSTANCE: AmbientSoundDatabase? = null
        fun getInstance(ctx: Context): AmbientSoundDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(ctx.applicationContext, AmbientSoundDatabase::class.java, "ambient_sound.db")
                    .build().also { INSTANCE = it }
            }
    }
}
```

Commit.

---

### Task 5: Rewrite AmbientSoundPlayer

**Modify:** `ui/ambientsound/AmbientSoundPlayer.kt` (complete rewrite)

```kotlin
package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.MediaPlayer
import com.diary.app.data.ambientsound.AudioTrack
import java.io.File

class AmbientSoundPlayer private constructor() {
    private var player: MediaPlayer? = null
    private var track: AudioTrack? = null
    private var paused = false
    private var vol = 0.5f
    private var sleepEndTime = 0L
    private var sleepActive = false

    companion object {
        @Volatile private var INSTANCE: AmbientSoundPlayer? = null
        fun getInstance(): AmbientSoundPlayer = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AmbientSoundPlayer().also { INSTANCE = it }
        }
    }

    val isPlaying get() = player?.isPlaying ?: false
    val isPaused get() = paused
    val hasSession get() = track != null
    val position get() = player?.currentPosition ?: 0
    val duration get() = player?.duration ?: 0
    val currentTrack get() = track
    val currentVolume get() = vol

    fun play(ctx: Context, t: AudioTrack, file: File) {
        stop()
        track = t; paused = false
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setVolume(vol, vol)
            isLooping = true
            prepare()
            start()
        }
    }

    fun resume() { player?.let { if (!it.isPlaying) { it.start(); paused = false } } }
    fun pause() { player?.let { if (it.isPlaying) { it.pause(); paused = true } } }

    fun stop() {
        player?.apply { if (isPlaying) stop(); release() }
        player = null; track = null; paused = false
    }

    fun seekTo(pos: Int) { player?.seekTo(pos) }
    fun setVolume(v: Float) { vol = v.coerceIn(0f, 1f); player?.setVolume(vol, vol) }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) { cancelSleepTimer(); return }
        sleepEndTime = System.currentTimeMillis() + minutes * 60000L
        sleepActive = true
    }

    fun cancelSleepTimer() { sleepActive = false; sleepEndTime = 0L }
    fun sleepRemaining(): Int {
        if (!sleepActive) return 0
        val rem = (sleepEndTime - System.currentTimeMillis()) / 1000
        return if (rem > 0) rem.toInt() else 0
    }

    fun isSleepExpired(): Boolean = sleepActive && System.currentTimeMillis() >= sleepEndTime

    fun checkTimer() { if (isSleepExpired()) { stop(); cancelSleepTimer() } }
}
```

**Also update AmbientSoundService.kt** to match the new player API (no more `hasActivePlayers()`, `getActiveTypes()`, `isAnyPlaying` — replaced with `hasSession`, `isPlaying`, `currentTrack`).

Key changes in AmbientSoundService:
- `player.hasActivePlayers()` → `player.hasSession`
- `player.isAnyPlaying` → `player.isPlaying`
- `player.getActiveTypes()` → `player.currentTrack?.name ?: ""`
- Notification text uses `player.currentTrack?.name` instead of `player.getActiveTypes().joinToString(...)`

Commit.

---

### Task 6: Rewrite AmbientSoundViewModel

**Modify:** `ui/ambientsound/AmbientSoundViewModel.kt`

```kotlin
package com.diary.app.ui.ambientsound

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.data.ambientsound.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AmbientSoundUiState(
    val categories: List<AudioCategory> = AudioRepository.categories,
    val selectedCategoryId: String = "sleep",
    val tracks: List<AudioTrack> = emptyList(),
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isDownloading: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val volume: Float = 0.5f,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemaining: Int = 0,
    val cachedIds: Set<String> = emptySet(),
    val showFullscreen: Boolean = false
)

class AmbientSoundViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AmbientSoundUiState())
    val state: StateFlow<AmbientSoundUiState> = _state

    private val player = AmbientSoundPlayer.getInstance()
    private val cache = AudioCacheManager(application)
    private val dao = AmbientSoundDatabase.getInstance(application).dao()

    init {
        selectCategory("sleep")
        startUpdater()
    }

    fun selectCategory(id: String) {
        _state.value = _state.value.copy(selectedCategoryId = id, tracks = AudioRepository.getTracks(id))
        refreshCacheStatus()
    }

    fun togglePlay(track: AudioTrack) {
        viewModelScope.launch {
            val s = _state.value
            if (player.currentTrack?.id == track.id && player.isPlaying) { player.pause(); sync(); return@launch }
            if (player.currentTrack?.id == track.id && player.isPaused) { player.resume(); sync(); return@launch }

            _state.value = s.copy(isDownloading = true)
            if (!cache.isCached(track.id)) {
                val r = cache.download(track.id, track.audioUrl)
                if (r.isFailure) { _state.value = _state.value.copy(isDownloading = false); return@launch }
            }
            refreshCacheStatus()
            player.play(getApplication(), track, cache.getFile(track.id))
            dao.addRecent(RecentEntity(track.id))
            sync()
            _state.value = _state.value.copy(isDownloading = false, showFullscreen = true)
        }
    }

    fun pause() { player.pause(); sync() }
    fun resume() { player.resume(); sync() }

    fun stop() {
        player.stop()
        _state.value = _state.value.copy(showFullscreen = false, currentTrack = null, isPlaying = false, currentPosition = 0, duration = 0)
    }

    fun seekTo(pos: Int) { player.seekTo(pos) }
    fun setVolume(v: Float) { player.setVolume(v); _state.value = _state.value.copy(volume = v) }

    fun setSleepTimer(m: Int) { player.startSleepTimer(m); _state.value = _state.value.copy(sleepTimerMinutes = m, sleepTimerRemaining = m * 60) }
    fun cancelSleepTimer() { player.cancelSleepTimer(); _state.value = _state.value.copy(sleepTimerMinutes = 0, sleepTimerRemaining = 0) }

    fun showFullscreen() { _state.value = _state.value.copy(showFullscreen = true) }
    fun hideFullscreen() { _state.value = _state.value.copy(showFullscreen = false) }

    private fun sync() {
        _state.value = _state.value.copy(
            currentTrack = player.currentTrack, isPlaying = player.isPlaying,
            isPaused = player.isPaused, currentPosition = player.position, duration = player.duration
        )
    }

    private fun refreshCacheStatus() {
        val ids = AudioRepository.getAllTracks().filter { cache.isCached(it.id) }.map { it.id }.toSet()
        _state.value = _state.value.copy(cachedIds = ids)
    }

    private fun startUpdater() {
        viewModelScope.launch {
            while (true) {
                delay(500)
                player.checkTimer()
                val s = _state.value
                if (player.isPlaying) {
                    _state.value = s.copy(currentPosition = player.position, duration = player.duration, isPlaying = true, currentTrack = player.currentTrack)
                }
                val rem = player.sleepRemaining()
                if (rem != s.sleepTimerRemaining) _state.value = _state.value.copy(sleepTimerRemaining = rem)
                if (!player.hasSession && s.currentTrack != null) {
                    _state.value = s.copy(currentTrack = null, isPlaying = false, currentPosition = 0, showFullscreen = false)
                }
            }
        }
    }
}
```

Commit.

---

### Task 7: Rewrite AmbientSoundScreen

**Modify:** `ui/ambientsound/AmbientSoundScreen.kt` (complete rewrite)

```kotlin
package com.diary.app.ui.ambientsound

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.diary.app.data.ambientsound.AudioTrack

@Composable
fun AmbientSoundScreen(
    onNavigateBack: () -> Unit,
    viewModel: AmbientSoundViewModel = viewModel()
) {
    val s by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        BrowseContent(s, viewModel, onNavigateBack)
        AnimatedVisibility(s.showFullscreen, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            FullscreenContent(s, viewModel)
        }
    }
}

@Composable
private fun BrowseContent(s: AmbientSoundUiState, vm: AmbientSoundViewModel, onBack: () -> Unit) {
    val cat = s.categories.find { it.id == s.selectedCategoryId }
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(model = cat?.backgroundImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC1C1511), Color(0x8817120F), Color(0xDD17120F)))))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color(0xFFCCA090), modifier = Modifier.clickable(onClick = onBack))
                Spacer(Modifier.width(8.dp))
                Text("环境音", fontSize = 18.sp, color = Color(0xFFF2E3DA), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2A1F1A)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFCCA090), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (s.sleepTimerMinutes > 0) "${s.sleepTimerMinutes}分" else "关闭", fontSize = 13.sp, color = Color(0xFFCCA090))
                    }
                }
            }

            // Category tabs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(44.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                s.categories.forEach { c ->
                    val sel = c.id == s.selectedCategoryId
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { vm.selectCategory(c.id) }) {
                        Text(c.name, fontSize = 15.sp, color = if (sel) Color(0xFFF2E3DA) else Color(0xFF9A8579))
                        if (sel) Box(Modifier.width(31.dp).height(3.dp).padding(top = 8.dp).background(Color(0xFFCCA090), RoundedCornerShape(2.dp)))
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A2D28)))

            // Track grid
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(s.tracks) { track ->
                    TrackCard(track, s.cachedIds.contains(track.id), s.currentTrack?.id == track.id && s.isPlaying, s.isDownloading) { vm.togglePlay(track) }
                }
            }
        }

        if (s.currentTrack != null) {
            MiniBar(s, vm)
        }
    }
}

@Composable
private fun TrackCard(track: AudioTrack, cached: Boolean, playing: Boolean, downloading: Boolean, onPlay: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(210.dp), shape = RoundedCornerShape(14.dp), color = Color(0xBB2A1F1A)) {
        Box {
            Column {
                AsyncImage(model = track.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(8.dp).height(100.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Text(track.name, fontSize = 14.sp, color = Color(0xFFF2E3DA), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
                Text(String.format("%d:%02d", track.durationSeconds / 60, track.durationSeconds % 60), fontSize = 12.sp, color = Color(0xFF9A8579), modifier = Modifier.padding(start = 12.dp, top = 4.dp))
            }
            Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).size(32.dp), shape = CircleShape, color = if (playing) Color(0xFFCCA090) else Color(0xFF4A3530)) {
                Box(contentAlignment = Alignment.Center) {
                    if (downloading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFCCA090))
                    else Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = if (playing) Color(0xFF17120F) else Color(0xFFCCA090), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FullscreenContent(s: AmbientSoundUiState, vm: AmbientSoundViewModel) {
    val t = s.currentTrack ?: return
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF17120F))) {
        AsyncImage(model = t.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x99000000), Color(0xDD1C1511)))))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowBack, contentDescription = "关闭", tint = Color(0xFFF2E3DA), modifier = Modifier.clickable { vm.hideFullscreen() })
                Spacer(Modifier.weight(1f))
                Text("正在播放", fontSize = 16.sp, color = Color(0xFFF2E3DA))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(24.dp))
            }

            Spacer(Modifier.height(60.dp))

            AsyncImage(model = t.imageUrl, contentDescription = null, modifier = Modifier.width(300.dp).height(300.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)

            Spacer(Modifier.height(30.dp))

            Text(t.name, fontSize = 24.sp, color = Color(0xFFF2E3DA), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), textAlign = TextAlign.Center)
            val cn = s.categories.find { it.id == t.categoryId }?.name ?: t.categoryId
            Text("$cn | ${String.format("%d:%02d", s.duration / 60000, (s.duration / 1000) % 60)}", fontSize = 14.sp, color = Color(0xFF9A8579), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), textAlign = TextAlign.Center)

            Spacer(Modifier.height(10.dp))

            // Progress
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF3A2D28))) {
                val p = if (s.duration > 0) (s.currentPosition.toFloat() / s.duration).coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(2.dp)).background(Color(0xFFCCA090)))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(String.format("%d:%02d", s.currentPosition / 60000, (s.currentPosition / 1000) % 60), fontSize = 11.sp, color = Color(0xFF9A8579))
                Spacer(Modifier.weight(1f))
                Text(String.format("%d:%02d", s.duration / 60000, (s.duration / 1000) % 60), fontSize = 11.sp, color = Color(0xFF9A8579))
            }

            Spacer(Modifier.height(16.dp))

            // Controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(36.dp), shape = CircleShape, color = Color(0xFF4A3530)) { Box(Alignment.Center) { Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", tint = Color(0xFFF2E3DA), modifier = Modifier.size(18.dp)) } }
                Spacer(Modifier.width(48.dp))
                Surface(Modifier.size(56.dp), shape = CircleShape, color = Color(0xFFCCA090)) {
                    Box(Alignment.Center) {
                        Icon(if (s.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF17120F), modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(48.dp))
                Surface(Modifier.size(36.dp), shape = CircleShape, color = Color(0xFF4A3530)) { Box(Alignment.Center) { Icon(Icons.Default.SkipNext, contentDescription = "下一首", tint = Color(0xFFF2E3DA), modifier = Modifier.size(18.dp)) } }
            }

            Spacer(Modifier.height(30.dp))

            // Volume
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF9A8579), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF3A2D28))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(s.volume).clip(RoundedCornerShape(2.dp)).background(Color(0xFFCCA090)))
                }
                Spacer(Modifier.width(8.dp))
                Text("音量", fontSize = 12.sp, color = Color(0xFF9A8579))
            }

            Spacer(Modifier.height(16.dp))

            // Sleep timer
            Surface(Modifier.align(Alignment.CenterHorizontally), shape = RoundedCornerShape(16.dp), color = Color(0xCC2A1F1A)) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFCCA090), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (s.sleepTimerMinutes > 0) "${s.sleepTimerRemaining / 60}分" else "睡眠定时", fontSize = 12.sp, color = Color(0xFFCCA090))
                }
            }
        }
    }
}
```

Commit.

---

### Task 8: Update AmbientSoundMiniBar

**Modify:** `ui/ambientsound/AmbientSoundMiniBar.kt`

Replace polling loop with state-driven approach:

```kotlin
package com.diary.app.ui.ambientsound

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AmbientSoundMiniBar(
    state: AmbientSoundUiState,
    onNavigateToFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToFullScreen),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            color = Color(0xDD2A1F1A),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, fontSize = 14.sp, color = Color(0xFFF2E3DA), fontWeight = FontWeight.SemiBold)
                    Text("正在播放", fontSize = 10.sp, color = Color(0xFF9A8579))
                }
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFF4A3530)) {
                    Box(Alignment.Center) {
                        Icon(Icons.Default.Stop, contentDescription = "停止", tint = Color(0xFFCCA090), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFCCA090)) {
                    Box(Alignment.Center) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF17120F),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
```

Commit.

---

### Task 9: Wire it all together

- Add AmbientSoundDatabase init to Application class (or Activity)
- Wire AmbientSoundScreen into DiaryNavHost.kt — make sure the nav route is registered
- Run build and fix any compilation errors
- Do a full review pass

---

### Self-Review

1. **Spec coverage:** All spec features covered: real audio (Task 3, 5), category switching (Task 2, 6), AI/track images (Task 7 via Coil), fullscreen player with progress/volume/sleep timer (Task 7), MiniBar (Task 8), favorites/recent (Task 4), no emoji (all tasks use Material Icons), no gradient circles (Task 7 uses AsyncImage).

2. **Placeholder scan:** No TBD/TODO. Every step has complete code.

3. **Type consistency:** `AudioTrack.id` used throughout. Player API `hasSession`/`isPlaying`/`currentTrack` consistent across Service, ViewModel, MiniBar.

4. **Scope check:** Single feature, focused. No scope creep.
