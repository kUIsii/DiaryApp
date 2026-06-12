package com.diary.app.ui.media

import com.diary.app.data.DiaryImage
import com.diary.app.data.DiaryPreview
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaLibraryMapperTest {

    @Test
    fun `media items are sorted by diary time then image order`() {
        val oldEntry = preview(id = 1, title = "Old", createdAt = 1_000)
        val newEntry = preview(id = 2, title = "New", createdAt = 2_000)

        val items = buildMediaLibraryItems(
            images = listOf(
                image(entryId = 1, mediaName = "old-1.jpg", sortOrder = 0),
                image(entryId = 2, mediaName = "new-2.jpg", sortOrder = 1),
                image(entryId = 2, mediaName = "new-1.jpg", sortOrder = 0)
            ),
            previews = listOf(oldEntry, newEntry),
            resolveDisplayPath = { "display/$it" },
            resolveThumbPath = { "thumb/$it" }
        )

        assertEquals(listOf("new-1.jpg", "new-2.jpg", "old-1.jpg"), items.map { it.mediaName })
        assertEquals(listOf(2L, 2L, 1L), items.map { it.entryId })
        assertEquals("New", items.first().entryTitle)
    }

    @Test
    fun `images without an existing diary are ignored`() {
        val items = buildMediaLibraryItems(
            images = listOf(
                image(entryId = 1, mediaName = "kept.jpg"),
                image(entryId = 404, mediaName = "missing.jpg")
            ),
            previews = listOf(preview(id = 1, title = "Kept", createdAt = 1_000)),
            resolveDisplayPath = { "display/$it" },
            resolveThumbPath = { "thumb/$it" }
        )

        assertEquals(1, items.size)
        assertEquals("kept.jpg", items.single().mediaName)
    }

    private fun preview(id: Long, title: String, createdAt: Long): DiaryPreview {
        return DiaryPreview(
            id = id,
            title = title,
            plainText = "",
            moodLevel = null,
            weather = null,
            location = null,
            latitude = null,
            longitude = null,
            isFavorite = false,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    private fun image(entryId: Long, mediaName: String, sortOrder: Int = 0): DiaryImage {
        return DiaryImage(
            entryId = entryId,
            localPath = "/media/$mediaName",
            thumbPath = "/media/thumbs/$mediaName",
            mediaName = mediaName,
            mediaRef = "diary-media://$mediaName",
            sortOrder = sortOrder
        )
    }
}
