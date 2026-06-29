package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val key: String,
    val displayName: String,
    val downloadUrl: String?
) {
    WHITE_NOISE("white_noise", "\u767D\u566A\u97F3", "https://bigsoundbank.com/UPLOAD/mp3/388.mp3"),
    RAIN("rain", "\u96E8\u58F0", "https://bigsoundbank.com/UPLOAD/mp3/0740.mp3"),
    FOREST("forest", "\u68EE\u6797", "https://bigsoundbank.com/UPLOAD/mp3/0100.mp3"),
    OCEAN("ocean", "\u6D77\u6D6A", "https://bigsoundbank.com/UPLOAD/mp3/2566.mp3"),
    CAFE("cafe", "\u5496\u5561\u5385", "https://bigsoundbank.com/UPLOAD/mp3/2561.mp3")
}

class AmbientSoundPlayer private constructor() {
    private val players = mutableMapOf<AmbientSoundType, MediaPlayer>()
    private val pendingPrepares = mutableMapOf<AmbientSoundType, MediaPlayer>()
    private val pendingDownloads = mutableSetOf<AmbientSoundType>()
    private var contextRef: Context? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { wasPausedByFocusLoss = true; pauseAll() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { wasPausedByFocusLoss = true; pauseAll() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ducked = true; for (t in getActiveTypes()) players[t]?.let { applyVol(it, t) }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (ducked) { ducked = false; for (t in getActiveTypes()) players[t]?.let { applyVol(it, t) } }
                wasPausedByFocusLoss = false
            }
        }
    }
    private var currentVolumes = mutableMapOf<AmbientSoundType, Float>()
    private var ducked = false
    var wasPausedByFocusLoss = false
    private var audioFocusHeld = false
    private var playCallback: (() -> Unit)? = null
    private var stopCallback: (() -> Unit)? = null

    fun setOnPlayCallback(cb: () -> Unit) { playCallback = cb }
    fun setOnStopCallback(cb: () -> Unit) { stopCallback = cb }

    companion object {
        @Volatile private var instance: AmbientSoundPlayer? = null
        fun getInstance(): AmbientSoundPlayer = instance ?: synchronized(this) { AmbientSoundPlayer().also { instance = it } }
    }

    fun init(context: Context) {
        contextRef = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play(type: AmbientSoundType, volume: Float = 0.5f) {
        val ctx = contextRef ?: return
        synchronized(players) {
            if (players.containsKey(type)) return
            if (pendingPrepares.containsKey(type)) return
        }

        val mp3 = File(ctx.cacheDir, "${type.key}.mp3")
        if (mp3.exists() && mp3.length() > 1000) {
            playFile(type, volume, mp3)
            return
        }

        val wav = File(ctx.cacheDir, "${type.key}.wav")
        if (wav.exists() && wav.length() > 1000) {
            playFile(type, volume, wav)
            return
        }

        synchronized(pendingDownloads) { pendingDownloads.add(type) }

        Thread {
            try {
                val url = type.downloadUrl
                if (url != null) {
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.setRequestProperty("Referer", "https://bigsoundbank.com/")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 30000
                    conn.instanceFollowRedirects = true
                    conn.connect()
                    if (conn.responseCode == 200) {
                        conn.inputStream.use { input -> FileOutputStream(mp3).use { output -> input.copyTo(output) } }
                    }
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w("AmbientSoundPlayer", "Download failed for ${type.key}", e)
                try { mp3.delete() } catch (_: Exception) {}
            }
            synchronized(pendingDownloads) { pendingDownloads.remove(type) }
            Handler(Looper.getMainLooper()).post {
                synchronized(players) {
                    if (players.containsKey(type)) return@post
                    if (pendingPrepares.containsKey(type)) return@post
                }
                if (mp3.exists() && mp3.length() > 1000) playFile(type, volume, mp3)
                else {
                    if (!wav.exists()) generateFallbackWav(ctx, type)
                    playFile(type, volume, wav)
                }
            }
        }.start()
    }

    private fun playFile(type: AmbientSoundType, volume: Float, file: File) {
        synchronized(players) {
            if (players.containsKey(type)) return
            if (pendingPrepares.containsKey(type)) return
        }
        ensureAudioFocus()
        val player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                setOnPreparedListener {
                    synchronized(players) {
                        pendingPrepares.remove(type)
                        players[type] = this@apply
                    }
                    currentVolumes[type] = volume
                    fadeTo(type, volume, 200)
                    start()
                    playCallback?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    synchronized(players) { pendingPrepares.remove(type) }
                    try { mp.release() } catch (_: Exception) {}
                    true
                }
                prepareAsync()
                setVolume(0f, 0f)
            }
        } catch (e: Exception) {
            Log.e("AmbientSoundPlayer", "Failed to create player for ${type.key}", e)
            return
        }
        synchronized(players) { pendingPrepares[type] = player }
    }

    fun stop(type: AmbientSoundType) {
        val ps = mutableListOf<MediaPlayer>()
        synchronized(players) {
            players.remove(type)?.let { ps.add(it) }
            pendingPrepares.remove(type)?.let { ps.add(it) }
        }
        for (p in ps) {
            try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
            p.release()
        }
        synchronized(currentVolumes) { currentVolumes.remove(type) }
        synchronized(players) { if (players.isEmpty() && pendingPrepares.isEmpty()) abandonAudioFocus() }
        stopCallback?.invoke()
    }

    fun stopAll() {
        val active: List<AmbientSoundType>
        synchronized(players) { active = players.keys.toList() }
        active.forEach { stop(it) }
    }

    fun pauseAll() {
        val active: List<MediaPlayer>
        synchronized(players) { active = players.values.toList() }
        for (p in active) {
            try { if (p.isPlaying) p.pause() } catch (_: Exception) {}
        }
    }

    fun resumeAll() {
        val active: List<MediaPlayer>
        synchronized(players) { active = players.values.toList() }
        for (p in active) {
            try { if (!p.isPlaying) p.start() } catch (_: Exception) {}
        }
    }

    val isPaused: Boolean get() {
        val active: List<MediaPlayer>
        synchronized(players) { active = players.values.toList() }
        return active.any {
            try { !it.isPlaying } catch (_: Exception) { false }
        }
    }

    val isAnyPlaying: Boolean get() {
        val active: List<MediaPlayer>
        synchronized(players) { active = players.values.toList() }
        return active.any {
            try { it.isPlaying } catch (_: Exception) { false }
        }
    }

    fun getActiveTypes(): Set<AmbientSoundType> = synchronized(players) { players.keys.toSet() }
    fun getVolume(type: AmbientSoundType): Float = synchronized(currentVolumes) { currentVolumes[type] ?: 0.5f }
    fun hasActivePlayers(): Boolean = synchronized(players) { players.isNotEmpty() || pendingPrepares.isNotEmpty() }

    fun setVolume(type: AmbientSoundType, volume: Float) {
        synchronized(currentVolumes) { currentVolumes[type] = volume }
        val p: MediaPlayer?
        synchronized(players) { p = players[type] }
        if (p != null) applyVol(p, type)
    }

    fun setVolumeAll(volume: Float) { for (t in getActiveTypes()) setVolume(t, volume) }

    private fun applyVol(player: MediaPlayer, type: AmbientSoundType) {
        try {
            val vol = (synchronized(currentVolumes) { currentVolumes[type] ?: 0.5f }).let { if (ducked) it * 0.3f else it }
            player.setVolume(vol, vol)
        } catch (_: Exception) {}
    }

    private var fadeHandler: Handler? = null
    private var fadeSteps = 0
    private var fadeIndex = 0

    private fun fadeTo(type: AmbientSoundType, target: Float, durationMs: Int) {
        fadeHandler?.removeCallbacksAndMessages(null)
        fadeHandler = Handler(Looper.getMainLooper())
        val p = synchronized(players) { players[type] } ?: return
        val start = synchronized(currentVolumes) { currentVolumes[type] ?: target }
        val steps = (durationMs / 16).coerceAtLeast(1)
        val delta = (target - start) / steps
        fadeSteps = steps
        fadeIndex = 0
        fun step() {
            fadeIndex++
            if (fadeIndex > fadeSteps) { applyVol(p, type); return }
            val v = (start + delta * fadeIndex).coerceIn(0f, 1f)
            try { p.setVolume(v, v) } catch (_: Exception) {}
            fadeHandler?.postDelayed({ step() }, 16)
        }
        step()
    }

    private fun ensureAudioFocus() {
        if (audioFocusHeld) return
        val am = audioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setOnAudioFocusChangeListener(focusChangeListener, Handler(Looper.getMainLooper()))
                .build()
            audioFocusRequest = request
            audioFocusHeld = am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            audioFocusHeld = am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= 26) audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        else am.abandonAudioFocus(focusChangeListener)
        audioFocusHeld = false; audioFocusRequest = null
    }

    private fun generateFallbackWav(ctx: Context, type: AmbientSoundType) {
        val file = File(ctx.cacheDir, "${type.key}.wav")
        if (file.exists()) return
        val sr = 44100; val durSec = 30; val n = sr * durSec
        val samples = when (type) {
            AmbientSoundType.WHITE_NOISE -> genWhiteNoise(n)
            AmbientSoundType.RAIN -> genRain(n, sr)
            AmbientSoundType.CAFE -> genCafe(n, sr)
            AmbientSoundType.FOREST -> genForest(n, sr)
            AmbientSoundType.OCEAN -> genOcean(n, sr)
        }
        writeWav(file, samples, sr)
    }

    private fun genWhiteNoise(n: Int): ShortArray {
        val rng = Random(42)
        return ShortArray(n) { ((rng.nextFloat() * 2f - 1f) * Short.MAX_VALUE * 0.3f).toInt().toShort() }
    }

    private fun genRain(n: Int, sr: Int): ShortArray {
        val rng = Random(99); val s = ShortArray(n); var st = 0f
        val pi = Math.PI.toFloat()
        for (i in s.indices) {
            st = st * 0.99f + rng.nextFloat() * 0.02f - 0.01f
            val e = if (rng.nextFloat() < 0.01f) 1f else 0.3f
            val v = (st * 2f + sin(i.toFloat() / sr * 0.5f * 2f * pi) * 0.05f) * 32767f * 0.25f * e
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun genCafe(n: Int, sr: Int): ShortArray {
        val rng = Random(77); val s = ShortArray(n); val pi = Math.PI.toFloat()
        for (i in s.indices) {
            val chatter = sin(i.toFloat() / sr * 30f * 2f * pi) * 0.15f
            val clatter = if (rng.nextFloat() < 0.01f) rng.nextFloat() * 0.3f else 0f
            val v = (chatter + clatter + rng.nextFloat() * 0.08f - 0.04f) * 32767f * 0.15f
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun genForest(n: Int, sr: Int): ShortArray {
        val rng = Random(55); val s = ShortArray(n); val pi = Math.PI.toFloat()
        for (i in s.indices) {
            val wind = sin(i.toFloat() / sr * 0.5f * 2f * pi) * 0.2f
            val bird = if (rng.nextFloat() < 0.001f) sin(i.toFloat() / sr * 2000f * 2f * pi) * 0.1f else 0f
            val v = (wind + rng.nextFloat() * 0.12f + bird) * 32767f * 0.2f
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun genOcean(n: Int, sr: Int): ShortArray {
        val rng = Random(33); val s = ShortArray(n); var p = 0f; val pi = Math.PI.toFloat()
        for (i in s.indices) {
            p += 0.99f + rng.nextFloat() * 0.02f
            val wave = sin(i.toFloat() / sr * 0.1f * 2f * pi) * 0.3f + sin(i.toFloat() / sr * 0.3f * 2f * pi) * 0.2f + sin(p) * 0.1f
            val crash = if (rng.nextFloat() < 0.0005f) rng.nextFloat() * 0.5f else 0f
            val v = (wave + crash) * 32767f * 0.15f
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1; val bits = 16; val byteRate = sampleRate * numChannels * bits / 8
        val blockAlign = numChannels * bits / 8; val dataSize = samples.size * 2; val fileSize = 36 + dataSize
        FileOutputStream(file).use { fos ->
            fun w(s: String) { fos.write(s.toByteArray(Charsets.US_ASCII)) }
            fun i16(v: Int) { fos.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())) }
            fun i32(v: Int) { fos.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())) }
            w("RIFF"); i32(fileSize); w("WAVE"); w("fmt "); i32(16); i16(1); i16(numChannels); i32(sampleRate); i32(byteRate); i16(blockAlign); i16(bits); w("data"); i32(dataSize)
            val buf = ByteArray(dataSize); for (i in samples.indices) { val v = samples[i].toInt(); buf[i * 2] = (v and 0xFF).toByte(); buf[i * 2 + 1] = ((v shr 8) and 0xFF).toByte() }
            fos.write(buf)
        }
    }
}
