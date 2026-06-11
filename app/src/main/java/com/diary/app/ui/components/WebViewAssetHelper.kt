package com.diary.app.ui.components

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File

/**
 * WebView 本地资源加载助手。
 * 把 diary_media 目录映射到 https://appassets/diary_media/，
 * 解决 Android WebView 跨目录 file:// 被拦截的问题。
 */
object WebViewAssetHelper {

    private const val AUTHORITY = "appassets"

    /** 构建 WebViewAssetLoader，映射 assets 和 diary_media 目录 */
    fun createAssetLoader(context: Context): WebViewAssetLoader {
        val mediaDir = File(context.filesDir, "diary_media")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        return WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(context)
            )
            .addPathHandler(
                "/diary_media/",
                WebViewAssetLoader.InternalStoragePathHandler(context, File(context.filesDir, "diary_media"))
            )
            .build()
    }

    /** 把本地文件路径转成 WebView 可加载的 https URL */
    fun toWebViewUrl(filePath: String): String {
        val normalized = filePath.replace("\\", "/")
        val mediaIndex = normalized.indexOf("/diary_media/")
        if (mediaIndex >= 0) {
            val relativePath = normalized.substring(mediaIndex + 1)
            return "https://$AUTHORITY/$relativePath"
        }
        return if (normalized.startsWith("file://")) normalized else "file://$normalized"
    }

    /** 把 file:// URL 转成 WebView 可加载的 https URL */
    fun toWebViewUrlFromFileUrl(fileUrl: String): String {
        val path = fileUrl.removePrefix("file://")
        return toWebViewUrl(path)
    }

    /** 尝试拦截本地资源请求 */
    fun interceptRequest(
        assetLoader: WebViewAssetLoader,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val uri: Uri = request?.url ?: return null
        return try {
            assetLoader.shouldInterceptRequest(uri)
        } catch (_: Exception) {
            null
        }
    }
}
