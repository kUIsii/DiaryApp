package com.diary.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.diary.app.ui.components.WebViewAssetHelper
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.max

private const val MEDIA_SCHEME = "diary-media://"

data class ImportedDiaryMedia(
    val mediaName: String,
    val displayFile: File,
    val thumbFile: File,
    val displayRef: String,
    val displayWebUrl: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSize: Long
)

object DiaryMediaManager {
    const val MEDIA_DIR_NAME = "diary_media"
    const val THUMB_DIR_NAME = "thumbs"

    fun mediaDir(context: Context): File = File(context.filesDir, MEDIA_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    fun thumbDir(context: Context): File = File(mediaDir(context), THUMB_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    fun toMediaRef(mediaName: String): String = "$MEDIA_SCHEME$mediaName"

    fun isMediaRef(value: String): Boolean = value.startsWith(MEDIA_SCHEME)

    fun mediaNameFromRef(ref: String): String = ref.removePrefix(MEDIA_SCHEME)

    fun toWebViewUrl(context: Context, refOrUrl: String): String {
        return when {
            refOrUrl.startsWith(MEDIA_SCHEME) -> {
                val mediaName = mediaNameFromRef(refOrUrl)
                WebViewAssetHelper.toWebViewUrl(File(mediaDir(context), mediaName).absolutePath)
            }
            refOrUrl.startsWith("file://") -> WebViewAssetHelper.toWebViewUrlFromFileUrl(refOrUrl)
            else -> refOrUrl
        }
    }

    fun contentToWebViewUrls(context: Context, content: String): String {
        if (content.isBlank()) return content
        return content
            .replace(Regex("\"diary-media://([^\"]+)\"")) { match ->
                "\"${toWebViewUrl(context, match.value.trim('"'))}\""
            }
            .replace(Regex("\"file://([^\"]*diary_media[^\"]*?)\"")) { match ->
                "\"${WebViewAssetHelper.toWebViewUrlFromFileUrl("file://${match.groupValues[1]}")}\""
            }
    }

    fun contentToStableMediaRefs(content: String): String {
        if (content.isBlank()) return content
        return content
            .replace(Regex("\"https://appassets/diary_media/((?!thumbs/)[^\"]+)\"")) { match ->
                "\"${toMediaRef(match.groupValues[1])}\""
            }
            .replace(Regex("\"file://([^\"]*diary_media[\\\\/](?!thumbs[\\\\/])([^\"\\\\/]+))\"")) { match ->
                "\"${toMediaRef(match.groupValues[2])}\""
            }
    }

    fun extractMediaNames(content: String): List<String> {
        if (content.isBlank()) return emptyList()
        val names = linkedSetOf<String>()
        Regex("diary-media://([^\"'\\s}]+)").findAll(content).forEach {
            names.add(it.groupValues[1])
        }
        Regex("https://appassets/diary_media/((?!thumbs/)[^\"'\\s}]+)").findAll(content).forEach {
            names.add(it.groupValues[1])
        }
        Regex("file://([^\"']*diary_media[\\\\/]((?!thumbs[\\\\/])[^\"'\\\\/]+))").findAll(content).forEach {
            names.add(it.groupValues[2])
        }
        return names.toList()
    }

    fun importImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        thumbDimension: Int = 360,
        jpegQuality: Int = 78
    ): ImportedDiaryMedia? {
        return importImage(
            openInputStream = { context.contentResolver.openInputStream(uri) },
            mediaDir = mediaDir(context),
            thumbDir = thumbDir(context),
            maxDimension = maxDimension,
            thumbDimension = thumbDimension,
            jpegQuality = jpegQuality
        )
    }

    internal fun importImage(
        openInputStream: () -> InputStream?,
        mediaDir: File,
        thumbDir: File,
        maxDimension: Int = 1600,
        thumbDimension: Int = 360,
        jpegQuality: Int = 78
    ): ImportedDiaryMedia? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateImageSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = openInputStream()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val displayBitmap = scaleBitmapIfNeeded(decoded, maxDimension)
        val thumbBitmap = scaleBitmapIfNeeded(displayBitmap, thumbDimension)

        mediaDir.mkdirs()
        thumbDir.mkdirs()
        val mediaName = "img_${System.currentTimeMillis()}_${contentHashSeed(bounds.outWidth, bounds.outHeight)}.jpg"
        val displayFile = File(mediaDir, mediaName)
        val thumbFile = File(thumbDir, mediaName)

        return try {
            displayFile.outputStream().use { displayBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it) }
            thumbFile.outputStream().use { thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 72, it) }
            ImportedDiaryMedia(
                mediaName = mediaName,
                displayFile = displayFile,
                thumbFile = thumbFile,
                displayRef = toMediaRef(mediaName),
                displayWebUrl = WebViewAssetHelper.toWebViewUrl(displayFile.absolutePath),
                mimeType = "image/jpeg",
                width = displayBitmap.width,
                height = displayBitmap.height,
                fileSize = displayFile.length()
            )
        } finally {
            if (thumbBitmap !== displayBitmap) thumbBitmap.recycle()
            if (displayBitmap !== decoded) displayBitmap.recycle()
            decoded.recycle()
        }
    }

    internal fun calculateImageSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (max(sampledWidth, sampledHeight) > maxDimension * 2) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sampleSize
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun contentHashSeed(width: Int, height: Int): String {
        val raw = "$width:$height:${System.nanoTime()}".toByteArray()
        val digest = MessageDigest.getInstance("SHA-1").digest(raw)
        return digest.take(5).joinToString("") { "%02x".format(it) }
    }
}
