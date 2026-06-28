package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.diary.app.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(val key: String, val displayName: String, val rawResId: Int?) {
    WHITE_NOISE("white_noise", "白噪音", R.raw.white_noise),
    RAIN("rain", "雨声", R.raw.rain),
    CAFE("cafe", "咖啡厅", R.raw.cafe),
    FOREST("forest", "森林", R.raw.forest),
    OCEAN("ocean", "海浪", R.raw.ocean)
}

class AmbientSoundPlayer private constructor() {
    private val players = mutableMapOf<AmbientSoundType, MediaPlayer>()
    private var contextRef: Context? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPausedByFocusLoss = true
                pauseAll()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPausedByFocusLoss = true
                pauseAll()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ducked = true
                for (t in players.keys) setVolume(t, currentVolumes[t]?.times(0.3f) ?: 0f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (ducked) {
                    ducked = false
                    for (t in players.keys) setVolume(t, currentVolumes[t] ?: 0.5f)
                }
                if (wasPausedByFocusLoss) {
                    wasPausedByFocusLoss = false
                }
            }
        }
    }
    private var currentVolumes = mutableMapOf<AmbientSoundType, Float>()
    private var ducked = false
    private var wasPausedByFocusLoss = false
    private var audioFocusHeld = false

    companion object {
        @Volatile
        private var instance: AmbientSoundPlayer? = null

        fun getInstance(): AmbientSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: AmbientSoundPlayer().also { instance = it }
            }
        }
    }

    fun init(context: Context) {
        contextRef = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play(type: AmbientSoundType, volume: Float = 0.5f) {
        val ctx = contextRef ?: return
        if (players.containsKey(type)) return
        ensureAudioFocus()
        val player = try {
            if (type.rawResId != null) {
                MediaPlayer.create(ctx, type.rawResId).apply {
                    isLooping = true
                    start()
                }
            } else {
                val file = getOrCreateAudioFile(ctx, type)
                if (!file.exists()) return
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (_: Exception) {
            return
        }
        players[type] = player
        currentVolumes[type] = volume
        if (ducked) {
            player.setVolume(volume * 0.3f, volume * 0.3f)
        } else {
            player.setVolume(0f, 0f)
            fadeTo(type, volume, 200)
        }
    }

    fun stop(type: AmbientSoundType) {
        fadeTo(type, 0f, 200)
        Handler(Looper.getMainLooper()).postDelayed({
            players.remove(type)?.apply {
                if (isPlaying) stop()
                release()
            }
            currentVolumes.remove(type)
            if (players.isEmpty()) abandonAudioFocus()
        }, 200)
    }

    fun stopAll() {
        val types = players.keys.toList()
        types.forEach { stop(it) }
    }

    fun pauseAll() {
        for ((_, player) in players) {
            if (player.isPlaying) player.pause()
        }
    }

    fun resumeAll() {
        for ((_, player) in players) {
            if (!player.isPlaying) player.start()
        }
    }

    val isPaused: Boolean get() = players.values.any { !it.isPlaying } && players.isNotEmpty()

    fun setVolume(type: AmbientSoundType, volume: Float) {
        currentVolumes[type] = volume
        val actual = if (ducked) volume * 0.3f else volume
        players[type]?.setVolume(actual, actual)
    }

    fun setVolumeAll(volume: Float) {
        for (type in players.keys) {
            setVolume(type, volume)
        }
    }

    val isAnyPlaying: Boolean get() = players.values.any { it.isPlaying }
    fun getActiveTypes(): Set<AmbientSoundType> = players.keys.toSet()
    fun getVolume(type: AmbientSoundType): Float = currentVolumes[type] ?: 0.5f

    private fun fadeTo(type: AmbientSoundType, target: Float, durationMs: Int) {
        val player = players[type] ?: return
        val start = currentVolumes[type] ?: target
        val steps = (durationMs / 16).coerceAtLeast(1)
        val delta = (target - start) / steps
        val handler = Handler(Looper.getMainLooper())
        var i = 0
        fun step() {
            i++
            if (i > steps) {
                val actual = if (ducked) target * 0.3f else target
                player.setVolume(actual, actual)
                return
            }
            val v = (start + delta * i).coerceIn(0f, 1f)
            val actual = if (ducked) v * 0.3f else v
            player.setVolume(actual, actual)
            handler.postDelayed({ step() }, 16)
        }
        step()
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
            val result = am.requestAudioFocus(request)
            audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val result = am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        val req = audioFocusRequest
        if (android.os.Build.VERSION.SDK_INT >= 26 && req != null) {
            am.abandonAudioFocusRequest(req)
        } else {
            am.abandonAudioFocus(focusChangeListener)
        }
        audioFocusHeld = false
        audioFocusRequest = null
    }

    private fun getOrCreateAudioFile(context: Context, type: AmbientSoundType): File {
        val file = File(context.cacheDir, "${type.key}.wav")
        if (!file.exists()) generateWav(type, file)
        return file
    }

    private fun generateWav(type: AmbientSoundType, file: File) {
        val sampleRate = 44100
        val durationSec = 300
        val numSamples = sampleRate * durationSec
        val samples = when (type) {
            AmbientSoundType.WHITE_NOISE -> generateWhiteNoise(numSamples)
            AmbientSoundType.RAIN -> generateRain(numSamples, sampleRate)
            AmbientSoundType.CAFE -> generateCafe(numSamples, sampleRate)
            AmbientSoundType.FOREST -> generateForest(numSamples, sampleRate)
            AmbientSoundType.OCEAN -> generateOcean(numSamples, sampleRate)
        }
        writeWav(file, samples, sampleRate)
    }

    private fun generateWhiteNoise(numSamples: Int): ShortArray {
        val rng = Random(42)
        return ShortArray(numSamples) {
            ((rng.nextFloat() * 2f - 1f) * Short.MAX_VALUE * 0.3f).toInt().toShort()
        }
    }

    private fun generateRain(numSamples: Int, sampleRate: Int): ShortArray {
        val rng = Random(99)
        val samples = ShortArray(numSamples)
        var state = 0f
        for (i in samples.indices) {
            state = state * 0.999f + rng.nextFloat() * 0.001f - 0.0005f
            val envelope = if (rng.nextFloat() < 0.001f) 1f else 0.3f
            val filtered = state * 2f + rng.nextFloat() * 0.1f - 0.05f
            val raw = (filtered * 32767f * 0.25f * envelope).toInt()
            samples[i] = raw.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun generateCafe(numSamples: Int, sampleRate: Int): ShortArray {
        val rng = Random(77)
        val samples = ShortArray(numSamples)
        val chatterRate = 30f
        for (i in samples.indices) {
            val chatter = sin(i.toFloat() / sampleRate * chatterRate * 2f * Math.PI.toFloat()) * 0.15f
            val clatter = if (rng.nextFloat() < 0.0005f) rng.nextFloat() * 0.3f else 0f
            val noise = rng.nextFloat() * 0.08f - 0.04f
            val raw = ((chatter + clatter + noise) * 32767f * 0.2f).toInt()
            samples[i] = raw.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun generateForest(numSamples: Int, sampleRate: Int): ShortArray {
        val rng = Random(55)
        val samples = ShortArray(numSamples)
        for (i in samples.indices) {
            val wind = sin(i.toFloat() / sampleRate * 0.5f * 2f * Math.PI.toFloat()) * 0.2f
            val rustle = rng.nextFloat() * 0.15f
            val birdChance = if ((i % sampleRate) in (sampleRate / 4) until (sampleRate / 4 + 200)) 0.0001f else 0f
            val bird = if (rng.nextFloat() < birdChance) sin(i.toFloat() / sampleRate * 2000f * 2f * Math.PI.toFloat()) * 0.1f else 0f
            val raw = ((wind + rustle + bird) * 32767f * 0.25f).toInt()
            samples[i] = raw.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun generateOcean(numSamples: Int, sampleRate: Int): ShortArray {
        val rng = Random(33)
        val samples = ShortArray(numSamples)
        var phase = 0f
        for (i in samples.indices) {
            val slowWave = sin(i.toFloat() / sampleRate * 0.1f * 2f * Math.PI.toFloat()) * 0.3f
            val medWave = sin(i.toFloat() / sampleRate * 0.3f * 2f * Math.PI.toFloat()) * 0.2f
            phase += 0.999f + rng.nextFloat() * 0.002f
            val noise = sin(phase) * 0.1f
            val crash = if (rng.nextFloat() < 0.00005f) rng.nextFloat() * 0.5f else 0f
            val raw = ((slowWave + medWave + noise + crash) * 32767f * 0.2f).toInt()
            samples[i] = raw.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = samples.size * 2
        val fileSize = 36 + dataSize
        FileOutputStream(file).use { fos ->
            fun writeString(s: String) { fos.write(s.toByteArray(Charsets.US_ASCII)) }
            fun writeInt16(v: Int) { fos.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())) }
            fun writeInt32(v: Int) { fos.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())) }
            writeString("RIFF")
            writeInt32(fileSize)
            writeString("WAVE")
            writeString("fmt ")
            writeInt32(16)
            writeInt16(1)
            writeInt16(numChannels)
            writeInt32(sampleRate)
            writeInt32(byteRate)
            writeInt16(blockAlign)
            writeInt16(bitsPerSample)
            writeString("data")
            writeInt32(dataSize)
            val buffer = ByteArray(dataSize)
            for (i in samples.indices) {
                val v = samples[i].toInt()
                buffer[i * 2] = (v and 0xFF).toByte()
                buffer[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            fos.write(buffer)
        }
    }
}
