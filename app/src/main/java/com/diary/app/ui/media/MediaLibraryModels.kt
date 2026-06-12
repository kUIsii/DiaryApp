package com.diary.app.ui.media

import com.diary.app.data.DiaryImage
import com.diary.app.data.DiaryPreview

data class MediaLibraryItem(
    val id: Long,
    val entryId: Long,
    val entryTitle: String,
    val entryText: String,
    val entryCreatedAt: Long,
    val mediaName: String,
    val displayPath: String,
    val thumbnailPath: String,
    val sortOrder: Int
)

fun buildMediaLibraryItems(
    images: List<DiaryImage>,
    previews: List<DiaryPreview>,
    resolveDisplayPath: (DiaryImage) -> String,
    resolveThumbPath: (DiaryImage) -> String
): List<MediaLibraryItem> {
    val previewById = previews.associateBy { it.id }
    return images
        .mapNotNull { image ->
            val preview = previewById[image.entryId] ?: return@mapNotNull null
            MediaLibraryItem(
                id = image.id,
                entryId = image.entryId,
                entryTitle = preview.title.ifBlank { "无标题日记" },
                entryText = preview.plainText,
                entryCreatedAt = preview.createdAt,
                mediaName = image.mediaName,
                displayPath = resolveDisplayPath(image),
                thumbnailPath = resolveThumbPath(image),
                sortOrder = image.sortOrder
            )
        }
        .sortedWith(
            compareByDescending<MediaLibraryItem> { it.entryCreatedAt }
                .thenBy { it.sortOrder }
                .thenBy { it.mediaName }
        )
}
