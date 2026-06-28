package com.diary.app.ui.ambientsound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val key: String,
    val displayName: String,
    val icon: String
) {
    WHITE_NOISE("white_noise", "白噪音", "\uD83C\uDF2C\uFE0F"),
    RAIN("rain", "雨声", "\uD83C\uDF27\uFE0F"),
    CAFE("cafe", "咖啡厅", "\uD83C\uDF76"),
    FOREST("forest", "森林", "\uD83C\uDF33"),
    OCEAN("ocean", "海浪", "\uD83C\uDF0A")
}

class AmbientSoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentType: AmbientSoundType? = null

    fun play(type: AmbientSoundType, volume: Float = 0.5f) {
        stop()

        val file = getOrCreateAudioFile(type)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(file.absolutePath)
            isLooping = true
            setVolume(volume, volume)
            prepare()
            start()
        }
        currentType = type
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentType = null
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    val isPlaying: Boolean get() = mediaPlayer?.isPlaying ?: false

    fun getOrCreateAudioFile(type: AmbientSoundType): File {
        val file = File(context.cacheDir, "${type.key}.wav")
        if (!file.exists()) {
            generateWav(type, file)
        }
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
        return ShortArray(numSamples) { (rng.nextFloat() * 2f - 1f).let { (it * Short.MAX_VALUE * 0.3f).toInt().toShort() } }
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
