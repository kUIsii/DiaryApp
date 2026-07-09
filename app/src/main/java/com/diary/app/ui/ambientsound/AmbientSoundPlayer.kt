package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.diary.app.data.ambientsound.AudioTrack
import java.io.File
import kotlin.math.sin
import kotlin.math.PI

class AmbientSoundPlayer private constructor() {
    private val playbackSessionGate = AmbientPlaybackSessionGate()
    private var player: MediaPlayer? = null
    private var track by mutableStateOf<AudioTrack?>(null)
    private var paused by mutableStateOf(false)
    private var vol = 0.5f
    private var sleepEndTime = 0L
    private var sleepActive = false
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusHeld = false
    private var ducked = false
    private var pausedForFocusLoss = false
    private var playCallback: (() -> Unit)? = null
    private var stopCallback: (() -> Unit)? = null

    private var meanderEnabled = false
    private var meanderFactor = 1f
    private var meanderHandler: Handler? = null
    private var meanderRunnable: Runnable? = null
    private var meanderPeriodMs = 10000L
    private var meanderPhase = 0.0
    private val meanderDepth = 0.12f

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去焦点（被其他应用长期占用）：不自动续播
                pausedForFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 瞬时失去（来电/其他 App 短暂出声）：暂停并标记，待恢复时自动续播
                pausedForFocusLoss = true
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; applyVolume() }
            AudioManager.AUDIOFOCUS_GAIN -> {
                ducked = false
                if (pausedForFocusLoss) {
                    // 因焦点瞬失而暂停的，恢复焦点后自动续播
                    pausedForFocusLoss = false
                    resume()
                } else {
                    applyVolume()
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AmbientSoundPlayer? = null

        fun getInstance(): AmbientSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: AmbientSoundPlayer().also { instance = it }
            }
        }
    }

    fun setOnPlayCallback(cb: () -> Unit) { playCallback = cb }
    fun setOnStopCallback(cb: () -> Unit) { stopCallback = cb }

    val isPlaying: Boolean get() = player?.isPlaying ?: false
    val isPausedState: Boolean get() = paused
    val hasSession: Boolean get() = track != null && player != null
    val currentPosition: Int get() = try { player?.currentPosition ?: 0 } catch (_: Exception) { 0 }
    val duration: Int get() = try { player?.duration ?: 0 } catch (_: Exception) { 0 }
    val currentTrack: AudioTrack? get() = track
    val currentVolume: Float get() = vol
    val isMeanderEnabled: Boolean get() = meanderEnabled

    fun play(context: Context, audioTrack: AudioTrack, audioFile: File): Result<Unit> {
        playbackSessionGate.beginSessionReplacement()
        stop()
        track = audioTrack
        paused = false
        ducked = false

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ensureAudioFocus()

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(audioFile.absolutePath)
            isLooping = true
            setVolume(vol, vol)
        }

        try {
            mp.prepare()
        } catch (e: Exception) {
            mp.release()
            track = null
            playCallback?.invoke()
            return Result.failure(e)
        }

        try {
            mp.start()
        } catch (e: Exception) {
            mp.release()
            track = null
            return Result.failure(e)
        }
        player = mp
        if (meanderEnabled) startMeander()
        playCallback?.invoke()
        return Result.success(Unit)
    }

    fun resume() {
        player?.let {
            if (!it.isPlaying) {
                ensureAudioFocus()
                it.start()
                paused = false
                if (meanderEnabled) startMeander()
                playCallback?.invoke()
            }
        }
    }

    fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                paused = true
                playCallback?.invoke()
            }
        }
    }

    fun stop() {
        stopMeander()
        try {
            player?.apply {
                try { if (isPlaying) stop() } catch (_: Exception) { }
                release()
            }
        } catch (_: Exception) { }
        player = null
        track = null
        paused = false
        sleepActive = false
        sleepEndTime = 0L
        abandonAudioFocus()
        if (playbackSessionGate.shouldDispatchStopCallback()) {
            stopCallback?.invoke()
        }
    }

    fun seekTo(position: Int) {
        try { player?.seekTo(position) } catch (_: Exception) { }
    }

    fun setVolume(volume: Float) {
        vol = volume.coerceIn(0f, 1f)
        applyVolume()
    }

    fun setMeanderEnabled(enabled: Boolean) {
        meanderEnabled = enabled
        if (enabled) {
            startMeander()
        } else {
            stopMeander()
        }
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        sleepEndTime = System.currentTimeMillis() + (minutes * 60_000L)
        sleepActive = true
    }

    fun cancelSleepTimer() {
        sleepActive = false
        sleepEndTime = 0L
    }

    fun sleepRemainingSeconds(): Int {
        if (!sleepActive) return 0
        val remaining = (sleepEndTime - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }

    fun isSleepExpired(): Boolean {
        return sleepActive && System.currentTimeMillis() >= sleepEndTime
    }

    private fun startMeander() {
        stopMeander()
        meanderFactor = 1f
        meanderPeriodMs = 5000L + (Math.random() * 10000).toLong()
        meanderPhase = 0.0
        meanderHandler = Handler(Looper.getMainLooper())
        meanderRunnable = Runnable {
            if (!meanderEnabled || player == null) return@Runnable
            meanderPhase += 2.0 * PI * 200.0 / meanderPeriodMs
            val normalized = (sin(meanderPhase) + 1.0) / 2.0
            // 在用户设定音量(vol)之上做 ±meanderDepth 的轻微呼吸式调制，不覆盖音量条
            meanderFactor = 1f - meanderDepth * (1f - normalized.toFloat())
            applyVolume()
            meanderHandler?.postDelayed(meanderRunnable!!, 200)
        }
        meanderHandler?.post(meanderRunnable!!)
    }

    private fun stopMeander() {
        meanderRunnable?.let { meanderHandler?.removeCallbacks(it) }
        meanderHandler = null
        meanderRunnable = null
        meanderFactor = 1f
        applyVolume()
    }

    private fun applyVolume() {
        val effectiveVol = if (ducked) vol * 0.3f else vol
        player?.setVolume(effectiveVol * meanderFactor, effectiveVol * meanderFactor)
    }

    private fun ensureAudioFocus() {
        if (audioFocusHeld) return
        val am = audioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusChangeListener, Handler(Looper.getMainLooper()))
                .build()
            audioFocusRequest = request
            audioFocusHeld = am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioFocusHeld = am.requestAudioFocus(
                focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            am.abandonAudioFocus(focusChangeListener)
        }
        audioFocusHeld = false
    }
}
