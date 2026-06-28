package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val key: String,
    val displayName: String,
    val downloadUrl: String?
) {
    WHITE_NOISE("white_noise", "白噪音", "https://bigsoundbank.com/UPLOAD/mp3/388.mp3"),
    RAIN("rain", "雨声", "https://bigsoundbank.com/UPLOAD/mp3/0740.mp3"),
    FOREST("forest", "森林", "https://bigsoundbank.com/UPLOAD/mp3/0100.mp3"),
    OCEAN("ocean", "海浪", "https://bigsoundbank.com/UPLOAD/mp3/2566.mp3"),
    CAFE("cafe", "咖啡厅", "https://bigsoundbank.com/UPLOAD/mp3/2561.mp3")
}

class AmbientSoundPlayer private constructor() {
    private val players = mutableMapOf<AmbientSoundType, MediaPlayer>()
    private var contextRef: Context? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { wasPausedByFocusLoss = true; pauseAll() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { wasPausedByFocusLoss = true; pauseAll() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ducked = true; for (t in players.keys) players[t]?.let { applyVolume(it, t) }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (ducked) { ducked = false; for (t in players.keys) players[t]?.let { applyVolume(it, t) } }
                wasPausedByFocusLoss = false
            }
        }
    }
    private var currentVolumes = mutableMapOf<AmbientSoundType, Float>()
    private var ducked = false
    var wasPausedByFocusLoss = false
    private var audioFocusHeld = false
    private var downloadInProgress = mutableSetOf<AmbientSoundType>()

    companion object {
        @Volatile private var instance: AmbientSoundPlayer? = null
        fun getInstance(): AmbientSoundPlayer = instance ?: synchronized(this) { instance ?: AmbientSoundPlayer().also { instance = it } }
    }

    fun init(context: Context) {
        contextRef = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play(type: AmbientSoundType, volume: Float = 0.5f) {
        val ctx = contextRef ?: return
        if (players.containsKey(type) || type in downloadInProgress) return

        val audioFile = getAudioFile(ctx, type)
        if (audioFile == null) {
            downloadInProgress.add(type)
            Thread {
                val file = downloadAudio(ctx, type)
                Handler(Looper.getMainLooper()).post {
                    downloadInProgress.remove(type)
                    if (file != null) playFile(type, volume, file) else createFallbackAndPlay(ctx, type, volume)
                }
            }.start()
            return
        }
        playFile(type, volume, audioFile)
    }

    private fun playFile(type: AmbientSoundType, volume: Float, file: File) {
        ensureAudioFocus()
        val player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) { return }
        players[type] = player
        currentVolumes[type] = volume
        player.setVolume(0f, 0f)
        fadeTo(type, volume, 200)
    }

    private fun createFallbackAndPlay(ctx: Context, type: AmbientSoundType, volume: Float) {
        val file = File(ctx.cacheDir, "${type.key}.wav")
        if (!file.exists()) generateWav(type, file)
        playFile(type, volume, file)
    }

    fun stop(type: AmbientSoundType) {
        fadeTo(type, 0f, 200)
        Handler(Looper.getMainLooper()).postDelayed({
            players.remove(type)?.apply { if (isPlaying) stop(); release() }
            currentVolumes.remove(type)
            if (players.isEmpty()) abandonAudioFocus()
        }, 200)
    }

    fun stopAll() { players.keys.toList().forEach { stop(it) } }

    fun pauseAll() { for ((_, p) in players) if (p.isPlaying) p.pause() }
    fun resumeAll() { for ((_, p) in players) if (!p.isPlaying) p.start() }

    val isPaused: Boolean get() = players.isNotEmpty() && players.values.any { !it.isPlaying }
    val isAnyPlaying: Boolean get() = players.values.any { it.isPlaying }

    fun setVolume(type: AmbientSoundType, volume: Float) {
        currentVolumes[type] = volume
        players[type]?.let { applyVolume(it, type) }
    }

    fun setVolumeAll(volume: Float) { for (t in players.keys) setVolume(t, volume) }

    fun getActiveTypes(): Set<AmbientSoundType> = players.keys.toSet()
    fun getVolume(type: AmbientSoundType): Float = currentVolumes[type] ?: 0.5f

    private fun applyVolume(player: MediaPlayer, type: AmbientSoundType) {
        val vol = (currentVolumes[type] ?: 0.5f).let { if (ducked) it * 0.3f else it }
        player.setVolume(vol, vol)
    }

    private fun fadeTo(type: AmbientSoundType, target: Float, durationMs: Int) {
        val player = players[type] ?: return
        val start = currentVolumes[type] ?: target
        val steps = (durationMs / 16).coerceAtLeast(1)
        val delta = (target - start) / steps
        val handler = Handler(Looper.getMainLooper())
        var i = 0
        fun step() {
            i++
            if (i > steps) { applyVolume(player, type); return }
            val v = (start + delta * i).coerceIn(0f, 1f)
            player.setVolume(v, v)
            handler.postDelayed({ step() }, 16)
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

    private fun getAudioFile(ctx: Context, type: AmbientSoundType): File? {
        val f = File(ctx.cacheDir, "${type.key}.mp3")
        if (f.exists() && f.length() > 1000) return f
        return null
    }

    private fun downloadAudio(ctx: Context, type: AmbientSoundType): File? {
        val url = type.downloadUrl ?: return null
        val dest = File(ctx.cacheDir, "${type.key}.mp3")
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.setRequestProperty("Referer", "https://bigsoundbank.com/")
            conn.connectTimeout = 10000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode == 200) {
                conn.inputStream.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
                if (dest.exists() && dest.length() > 1000) return dest
            }
            conn.disconnect()
        } catch (_: Exception) {}
        return null
    }

    private fun generateWav(type: AmbientSoundType, file: File) {
        val sampleRate = 44100; val durationSec = 300; val numSamples = sampleRate * durationSec
        val samples = when (type) {
            AmbientSoundType.WHITE_NOISE -> generateWhiteNoise(numSamples)
            AmbientSoundType.RAIN -> generateRain(numSamples, sampleRate)
            AmbientSoundType.CAFE -> generateCafe(numSamples, sampleRate)
            AmbientSoundType.FOREST -> generateForest(numSamples, sampleRate)
            AmbientSoundType.OCEAN -> generateOcean(numSamples, sampleRate)
        }
        writeWav(file, samples, sampleRate)
    }

    private fun generateWhiteNoise(n: Int): ShortArray {
        val rng = Random(42); return ShortArray(n) { ((rng.nextFloat() * 2f - 1f) * Short.MAX_VALUE * 0.3f).toInt().toShort() }
    }

    private fun generateRain(n: Int, sr: Int): ShortArray {
        val rng = Random(99); val s = ShortArray(n); var st = 0f
        for (i in s.indices) {
            st = st * 0.999f + rng.nextFloat() * 0.001f - 0.0005f
            val e = if (rng.nextFloat() < 0.001f) 1f else 0.3f
            val v = (st * 2f + rng.nextFloat() * 0.1f - 0.05f) * 32767f * 0.25f * e
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun generateCafe(n: Int, sr: Int): ShortArray {
        val rng = Random(77); val s = ShortArray(n); val pi = Math.PI.toFloat()
        for (i in s.indices) {
            val v = ((sin(i.toFloat() / sr * 30f * 2f * pi) * 0.15f) + (if (rng.nextFloat() < 0.001f) rng.nextFloat() * 0.3f else 0f) + (rng.nextFloat() * 0.08f - 0.04f)) * 32767f * 0.15f
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun generateForest(n: Int, sr: Int): ShortArray {
        val rng = Random(55); val s = ShortArray(n); val pi = Math.PI.toFloat()
        for (i in s.indices) {
            val v = (sin(i.toFloat() / sr * 0.5f * 2f * pi) * 0.2f + rng.nextFloat() * 0.12f + (if (rng.nextFloat() < 0.00005f) sin(i.toFloat() / sr * 2000f * 2f * pi) * 0.1f else 0f)) * 32767f * 0.2f
            s[i] = v.toInt().coerceIn(-32767, 32767).toShort()
        }
        return s
    }

    private fun generateOcean(n: Int, sr: Int): ShortArray {
        val rng = Random(33); val s = ShortArray(n); var p = 0f; val pi = Math.PI.toFloat()
        for (i in s.indices) {
            p += 0.999f + rng.nextFloat() * 0.002f
            val v = (sin(i.toFloat() / sr * 0.1f * 2f * pi) * 0.3f + sin(i.toFloat() / sr * 0.3f * 2f * pi) * 0.2f + sin(p) * 0.1f + (if (rng.nextFloat() < 0.00003f) rng.nextFloat() * 0.5f else 0f)) * 32767f * 0.15f
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
