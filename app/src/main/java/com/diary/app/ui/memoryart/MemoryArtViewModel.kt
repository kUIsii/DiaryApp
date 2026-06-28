package com.diary.app.ui.memoryart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

data class MemoryArtConfig(
    val diaryId: Long,
    val seed: Long,
    val colorPalette: List<Long>,
    val shapes: List<ArtShape>
)

data class ArtShape(
    val type: ShapeType,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float,
    val color: Long,
    val alpha: Float
)

enum class ShapeType {
    CIRCLE, SQUARE, TRIANGLE, LINE, ARC
}

class MemoryArtViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _artConfig = MutableStateFlow<MemoryArtConfig?>(null)
    val artConfig: StateFlow<MemoryArtConfig?> = _artConfig

    fun generateArt(diaryId: Long) {
        viewModelScope.launch {
            val entry = dao.getEntryById(diaryId) ?: return@launch
            
            val seed = entry.createdAt + entry.plainText.length.toLong()
            val random = java.util.Random(seed)
            
            // Generate color palette based on mood
            val mood = entry.moodLevel ?: 3
            val colorPalette = when (mood) {
                1 -> listOf(0xFFE53935, 0xFFD32F2F, 0xFFC62828, 0xFFB71C1C, 0xFFEF5350) // Sad - red tones
                2 -> listOf(0xFFFF9800, 0xFFF57C00, 0xFFEF6C00, 0xFFE65100, 0xFFFFB74D) // Low - orange tones
                3 -> listOf(0xFFFFC107, 0xFFFFD54F, 0xFFFFCA28, 0xFFFFB300, 0xFFFFE082) // Neutral - yellow tones
                4 -> listOf(0xFF4CAF50, 0xFF388E3C, 0xFF2E7D32, 0xFF1B5E20, 0xFF81C784) // Good - green tones
                5 -> listOf(0xFF2196F3, 0xFF1976D2, 0xFF1565C0, 0xFF0D47A1, 0xFF64B5F6) // Great - blue tones
                6 -> listOf(0xFF9C27B0, 0xFF7B1FA2, 0xFF6A1B9A, 0xFF4A148C, 0xFFBA68C8) // Excellent - purple tones
                else -> listOf(0xFF9E9E9E, 0xFF757575, 0xFF616161, 0xFF424242, 0xFFBDBDBD) // Default - gray
            }
            
            // Generate shapes based on text characteristics
            val textLength = entry.plainText.length
            val wordCount = entry.plainText.split(Regex("\\s+")).size
            val shapeCount = (wordCount / 10).coerceIn(5, 30)
            
            val shapes = (0 until shapeCount).map { i ->
                val shapeType = ShapeType.values()[random.nextInt(ShapeType.values().size)]
                val x = random.nextFloat()
                val y = random.nextFloat()
                val size = (random.nextFloat() * 0.3f + 0.05f) * (1f + sin(i.toFloat()) * 0.3f)
                val rotation = random.nextFloat() * 360f
                val color = colorPalette[random.nextInt(colorPalette.size)]
                val alpha = random.nextFloat() * 0.5f + 0.3f
                
                ArtShape(shapeType, x, y, size, rotation, color, alpha)
            }
            
            _artConfig.value = MemoryArtConfig(
                diaryId = diaryId,
                seed = seed,
                colorPalette = colorPalette,
                shapes = shapes
            )
        }
    }
}
