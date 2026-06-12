package com.diary.app.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryMediaManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File

data class MediaLibraryState(
    val items: List<MediaLibraryItem> = emptyList(),
    val isLoading: Boolean = true
)

class MediaLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val dao = (application as DiaryApplication).database.diaryDao()

    val state: StateFlow<MediaLibraryState> = combine(
        dao.getAllImagesFlow(),
        dao.getAllPreviews()
    ) { images, previews ->
        MediaLibraryState(
            items = buildMediaLibraryItems(
                images = images,
                previews = previews,
                resolveDisplayPath = { image ->
                    image.localPath.ifBlank {
                        File(DiaryMediaManager.mediaDir(appContext), image.mediaName).absolutePath
                    }
                },
                resolveThumbPath = { image ->
                    image.thumbPath?.takeIf { it.isNotBlank() }
                        ?: File(DiaryMediaManager.thumbDir(appContext), image.mediaName).absolutePath
                }
            ),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLibraryState())
}
