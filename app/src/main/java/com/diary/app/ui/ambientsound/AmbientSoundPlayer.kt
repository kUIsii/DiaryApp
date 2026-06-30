package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.diary.app.data.ambientsound.AudioTrack
import java.io.File

class AmbientSoundPlayer private constructor() {
    private var player: MediaPlayer? = null
    private var track: AudioTrack? = null
    private var paused = false
    private var vol = 0.5f
    private var sleepEndTime = 0L
    private var sleepActive = false
    private var audioManager: AudioManager? = null
    private var audioFocusHeld = false
    private var ducked = false
    private var playCallback: (() -> Unit)? = null
    private var stopCallback: (() -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; applyVolume() }
            AudioManager.AUDIOFOCUS_GAIN -> { ducked = false; applyVolume() }
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
    val hasSession: Boolean get() = track != null
    val currentPosition: Int get() = player?.currentPosition ?: 0
    val duration: Int get() = player?.duration ?: 0
    val currentTrack: AudioTrack? get() = track
    val currentVolume: Float get() = vol

    fun play(context: Context, audioTrack: AudioTrack, audioFile: File) {
        stop()
        track = audioTrack
        paused = false
        ducked = false

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ensureAudioFocus()

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(audioFile.absolutePath)
            isLooping = true
            setVolume(vol, vol)
            prepare()
            start()
            playCallback?.invoke()
        }
    }

    fun resume() {
        player?.let {
            if (!it.isPlaying) {
                it.start()
                paused = false
                playCallback?.invoke()
            }
        }
    }

    fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                paused = true
            }
        }
    }

    fun stop() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
        track = null
        paused = false
        abandonAudioFocus()
        stopCallback?.invoke()
    }

    fun seekTo(position: Int) {
        player?.seekTo(position)
    }

    fun setVolume(volume: Float) {
        vol = volume.coerceIn(0f, 1f)
        applyVolume()
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

    fun checkAndExpireSleepTimer() {
        if (isSleepExpired()) {
            stop()
            cancelSleepTimer()
        }
    }

    private fun applyVolume() {
        val effectiveVol = if (ducked) vol * 0.3f else vol
        player?.setVolume(effectiveVol, effectiveVol)
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
            try {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusChangeListener, Handler(Looper.getMainLooper()))
                    .build()
                am.abandonAudioFocusRequest(request)
            } catch (_: Exception) {}
        } else {
            am.abandonAudioFocus(focusChangeListener)
        }
        audioFocusHeld = false
    }
}
