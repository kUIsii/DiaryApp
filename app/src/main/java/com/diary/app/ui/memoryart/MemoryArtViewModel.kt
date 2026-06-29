package com.diary.app.ui.memoryart

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiRequest
import com.diary.app.ai.AiServiceManager
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class ArtStyle(val label: String) {
    WATERCOLOR("水彩"),
    OIL("油画"),
    SKETCH("素描"),
    ABSTRACT("抽象"),
    INK("水墨")
}

enum class PromptFormat(val label: String) {
    MIDJOURNEY("Midjourney"),
    STABLE_DIFFUSION("Stable Diffusion"),
    DALLE("DALL-E")
}

enum class ShapeType {
    CIRCLE, SQUARE, TRIANGLE, LINE, ARC, SPLASH, STROKE, DOT
}

data class MemoryArtwork(
    val id: String,
    val entryIds: List<Long>,
    val style: String,
    val palette: List<String>,
    val composition: ArtComposition,
    val aiPrompt: String,
    val promptFormat: String,
    val isFavorite: Boolean,
    val seriesId: String?,
    val createdAt: Long
)

data class ArtComposition(
    val shapes: List<ArtShape>,
    val layout: String,
    val intensity: Float
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

data class MemoryArtConfig(
    val diaryId: Long,
    val seed: Long,
    val colorPalette: List<Long>,
    val shapes: List<ArtShape>
)

data class EvolutionSeries(
    val seriesId: String,
    val theme: String,
    val artworks: List<MemoryArtwork>,
    val emotionTransition: String
)

data class MemoryCollage(
    val id: String,
    val entryIds: List<Long>,
    val period: String,
    val artworks: List<MemoryArtwork>,
    val dominantColors: List<Long>
)

data class MemoryArtUiState(
    val artworks: List<MemoryArtwork> = emptyList(),
    val currentArtwork: MemoryArtwork? = null,
    val artConfig: MemoryArtConfig? = null,
    val evolutionSeries: List<EvolutionSeries> = emptyList(),
    val collage: MemoryCollage? = null,
    val selectedStyle: ArtStyle = ArtStyle.ABSTRACT,
    val selectedPromptFormat: PromptFormat = PromptFormat.MIDJOURNEY,
    val isGenerating: Boolean = false,
    val isAiEnabled: Boolean = false,
    val moodFilter: Int? = null,
    val styleFilter: String? = null,
    val seriesFilter: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false
)

class MemoryArtViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val aiService = AiServiceManager(application)
    private val prefs = application.getSharedPreferences("memory_art", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(MemoryArtUiState())
    val uiState: StateFlow<MemoryArtUiState> = _uiState.asStateFlow()

    init {
        loadArtworks()
        _uiState.value = _uiState.value.copy(isAiEnabled = aiService.isAiEnabled())
    }

    fun selectStyle(style: ArtStyle) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun selectPromptFormat(format: PromptFormat) {
        _uiState.value = _uiState.value.copy(selectedPromptFormat = format)
    }

    fun setMoodFilter(mood: Int?) {
        _uiState.value = _uiState.value.copy(moodFilter = mood)
    }

    fun setStyleFilter(style: String?) {
        _uiState.value = _uiState.value.copy(styleFilter = style)
    }

    fun setSeriesFilter(seriesId: String?) {
        _uiState.value = _uiState.value.copy(seriesFilter = seriesId)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleFavoritesOnly() {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = !_uiState.value.showFavoritesOnly)
    }

    fun generateArt(diaryId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val entry = dao.getEntryById(diaryId) ?: return@launch

            if (aiService.isAiEnabled()) {
                generateArtWithAi(entry)
            } else {
                generateArtLocal(entry)
            }
            _uiState.value = _uiState.value.copy(isGenerating = false)
        }
    }

    private suspend fun generateArtWithAi(entry: DiaryEntry) {
        val style = _uiState.value.selectedStyle.label
        val prompt = """
你是一个艺术生成系统。根据以下日记内容，生成JSON格式的艺术作品描述。

日记情绪: ${entry.moodLevel ?: 3}/6 (1=沮丧, 6=兴奋)
日记内容: ${entry.plainText.take(500)}
艺术风格: $style

返回格式（严格JSON，不要markdown包装）:
{
  "colorPalette": ["#HEX1", "#HEX2", "#HEX3", "#HEX4", "#HEX5"],
  "intensity": 0.0-1.0,
  "layout": "balanced/chaotic/flowing/centered",
  "shapes": [
    {"type": "CIRCLE/SQUARE/TRIANGLE/LINE/ARC/SPLASH/STROKE/DOT", "x": 0.0-1.0, "y": 0.0-1.0, "size": 0.0-0.5, "rotation": 0-360, "description": "why this shape here"}
  ]
}
生成8-20个形状，每个shape的x,y是画布上的相对位置(0-1)，size是相对尺寸(0-0.5)。
注意：不要有任何多余的说明文字，只返回纯JSON。
        """.trimIndent()

        val result = aiService.chat(aiRequest(
            userMessage = prompt,
            systemPrompt = "你是记忆艺术AI，根据日记内容生成视觉艺术描述。只返回JSON。",
            temperature = 0.9f,
            maxTokens = 2048
        ))

        result.onSuccess { response ->
            val artwork = parseAiResponse(response.content, listOf(entry.id), style)
            if (artwork != null) {
                saveArtwork(artwork)
                val config = artworkToConfig(artwork, entry)
                _uiState.value = _uiState.value.copy(
                    currentArtwork = artwork,
                    artConfig = config
                )
            }
        }.onFailure {
            generateArtLocal(entry)
        }
    }

    private suspend fun generateArtLocal(entry: DiaryEntry) {
        val style = _uiState.value.selectedStyle
        val seed = entry.createdAt + entry.plainText.length.toLong()
        val random = Random(seed)
        val mood = entry.moodLevel ?: 3

        val colorPalette = moodColors(mood, style)
        val shapeCount = (entry.plainText.split(Regex("\\s+")).size / 10).coerceIn(5, 30)
        val shapes = (0 until shapeCount).map { i ->
            val shapeType = when (style) {
                ArtStyle.SKETCH -> listOf(ShapeType.LINE, ShapeType.STROKE, ShapeType.DOT)[random.nextInt(3)]
                ArtStyle.INK -> listOf(ShapeType.SPLASH, ShapeType.STROKE, ShapeType.CIRCLE)[random.nextInt(3)]
                ArtStyle.WATERCOLOR -> listOf(ShapeType.CIRCLE, ShapeType.SPLASH, ShapeType.STROKE)[random.nextInt(3)]
                ArtStyle.OIL -> listOf(ShapeType.CIRCLE, ShapeType.SQUARE, ShapeType.TRIANGLE, ShapeType.STROKE)[random.nextInt(4)]
                ArtStyle.ABSTRACT -> ShapeType.values().filter { it != ShapeType.SPLASH && it != ShapeType.DOT }[random.nextInt(6)]
            }
            ArtShape(
                type = shapeType,
                x = random.nextFloat(),
                y = random.nextFloat(),
                size = (random.nextFloat() * 0.3f + 0.05f) * (1f + sin(i.toFloat()) * 0.3f),
                rotation = random.nextFloat() * 360f,
                color = colorPalette[random.nextInt(colorPalette.size)],
                alpha = when (style) {
                    ArtStyle.WATERCOLOR -> random.nextFloat() * 0.3f + 0.15f
                    ArtStyle.INK -> random.nextFloat() * 0.5f + 0.3f
                    ArtStyle.OIL -> random.nextFloat() * 0.2f + 0.7f
                    ArtStyle.SKETCH -> random.nextFloat() * 0.3f + 0.5f
                    ArtStyle.ABSTRACT -> random.nextFloat() * 0.4f + 0.5f
                }
            )
        }

        val hexPalette = colorPalette.map { String.format("#%08X", it) }
        val intensity = when (mood) {
            1, 2 -> 0.3f
            3, 4 -> 0.6f
            5, 6 -> 0.9f
            else -> 0.5f
        }
        val layout = when {
            mood <= 2 -> "chaotic"
            mood <= 4 -> "balanced"
            else -> "flowing"
        }

        val composition = ArtComposition(shapes, layout, intensity)
        val artworkId = "art_${entry.id}_${System.currentTimeMillis()}"
        val artwork = MemoryArtwork(
            id = artworkId,
            entryIds = listOf(entry.id),
            style = style.label,
            palette = hexPalette,
            composition = composition,
            aiPrompt = buildLocalPrompt(composition, hexPalette, style.label),
            promptFormat = "Midjourney",
            isFavorite = false,
            seriesId = null,
            createdAt = System.currentTimeMillis()
        )

        saveArtwork(artwork)
        _uiState.value = _uiState.value.copy(
            currentArtwork = artwork,
            artConfig = MemoryArtConfig(
                diaryId = entry.id,
                seed = seed,
                colorPalette = colorPalette,
                shapes = shapes
            )
        )
    }

    private fun moodColors(mood: Int, style: ArtStyle): List<Long> {
        val base = when (mood) {
            1 -> listOf(0xFFE53935, 0xFFD32F2F, 0xFFC62828, 0xFFB71C1C, 0xFFEF5350)
            2 -> listOf(0xFFFF9800, 0xFFF57C00, 0xFFEF6C00, 0xFFE65100, 0xFFFFB74D)
            3 -> listOf(0xFFFFC107, 0xFFFFD54F, 0xFFFFCA28, 0xFFFFB300, 0xFFFFE082)
            4 -> listOf(0xFF4CAF50, 0xFF388E3C, 0xFF2E7D32, 0xFF1B5E20, 0xFF81C784)
            5 -> listOf(0xFF2196F3, 0xFF1976D2, 0xFF1565C0, 0xFF0D47A1, 0xFF64B5F6)
            6 -> listOf(0xFF9C27B0, 0xFF7B1FA2, 0xFF6A1B9A, 0xFF4A148C, 0xFFBA68C8)
            else -> listOf(0xFF9E9E9E, 0xFF757575, 0xFF616161, 0xFF424242, 0xFFBDBDBD)
        }
        return when (style) {
            ArtStyle.SKETCH -> listOf(0xFF212121, 0xFF424242, 0xFF616161, 0xFF9E9E9E, 0xFFBDBDBD)
            ArtStyle.INK -> listOf(0xFF000000, 0xFF1A1A1A, 0xFF333333, 0xFF4D4D4D, 0xFF666666)
            else -> base
        }
    }

    private fun buildLocalPrompt(composition: ArtComposition, palette: List<String>, style: String): String {
        return "A $style style artwork with composition: ${composition.layout}, " +
            "color palette: ${palette.joinToString(", ")}, " +
            "featuring ${composition.shapes.size} elements at intensity ${composition.intensity}"
    }

    private fun parseAiResponse(content: String, entryIds: List<Long>, style: String): MemoryArtwork? {
        return try {
            val json = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = gson.fromJson(json, Map::class.java)
            val paletteRaw = (parsed["colorPalette"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val shapesRaw = (parsed["shapes"] as? List<*>) ?: emptyList<Map<String, Any>>()
            val intensity = (parsed["intensity"] as? Double)?.toFloat() ?: 0.5f
            val layout = (parsed["layout"] as? String) ?: "balanced"

            val shapes = shapesRaw.mapNotNull { shapeMap ->
                val sm = shapeMap as? Map<*, *> ?: return@mapNotNull null
                val typeName = (sm["type"] as? String) ?: return@mapNotNull null
                val type = try { ShapeType.valueOf(typeName) } catch (_: Exception) { return@mapNotNull null }
                ArtShape(
                    type = type,
                    x = (sm["x"] as? Double)?.toFloat() ?: 0.5f,
                    y = (sm["y"] as? Double)?.toFloat() ?: 0.5f,
                    size = (sm["size"] as? Double)?.toFloat() ?: 0.1f,
                    rotation = (sm["rotation"] as? Double)?.toFloat() ?: 0f,
                    color = try {
                        val c = (sm["color"] as? String)
                        if (c != null) colorHex(c) else 0xFF9E9E9E
                    } catch (_: Exception) { 0xFF9E9E9E },
                    alpha = (sm["alpha"] as? Double)?.toFloat() ?: 0.7f
                )
            }

            val hexPalette = if (paletteRaw.isNotEmpty()) paletteRaw else listOf("#FF9E9E9E")
            val composition = ArtComposition(shapes, layout, intensity)
            val artworkId = "art_${entryIds.first()}_${System.currentTimeMillis()}"

            MemoryArtwork(
                id = artworkId,
                entryIds = entryIds,
                style = style,
                palette = hexPalette,
                composition = composition,
                aiPrompt = content,
                promptFormat = "AI Generated",
                isFavorite = false,
                seriesId = null,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun colorHex(hex: String): Long {
        val clean = hex.removePrefix("#")
        return when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16))
            8 -> clean.toLong(16)
            else -> 0xFF9E9E9E
        }
    }

    private fun artworkToConfig(artwork: MemoryArtwork, entry: DiaryEntry): MemoryArtConfig {
        return MemoryArtConfig(
            diaryId = entry.id,
            seed = entry.createdAt,
            colorPalette = artwork.palette.map { colorHex(it) },
            shapes = artwork.composition.shapes
        )
    }

    fun generateEvolutionSeries(themeKeyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val allEntries = dao.getAllEntriesOnce()
            val themeEntries = allEntries.filter {
                it.plainText.contains(themeKeyword, ignoreCase = true) ||
                it.title.contains(themeKeyword, ignoreCase = true)
            }.sortedBy { it.createdAt }

            if (themeEntries.size < 2) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
                return@launch
            }

            val seriesId = "series_${themeKeyword}_${System.currentTimeMillis()}"
            val seriesArtworks = mutableListOf<MemoryArtwork>()
            val emotionTransition = buildTransitionDescription(themeEntries)

            themeEntries.forEachIndexed { index, entry ->
                if (index == 0 || index == themeEntries.lastIndex || (index >= themeEntries.size / 3 && index % 2 == 0)) {
                    val style = when (entry.moodLevel ?: 3) {
                        1, 2 -> ArtStyle.INK
                        3, 4 -> ArtStyle.WATERCOLOR
                        else -> ArtStyle.OIL
                    }
                    _uiState.value = _uiState.value.copy(selectedStyle = style)
                    
                    if (aiService.isAiEnabled() && index == 0) {
                        generateArtWithAi(entry)
                    } else {
                        generateArtLocal(entry)
                    }

                    val current = _uiState.value.currentArtwork
                    if (current != null) {
                        val seriesArtwork = current.copy(
                            id = "${seriesId}_${index}",
                            seriesId = seriesId,
                            palette = moodBasedPalette(entry.moodLevel ?: 3)
                        )
                        seriesArtworks.add(seriesArtwork)
                        saveArtwork(seriesArtwork)
                    }
                }
            }

            val series = EvolutionSeries(
                seriesId = seriesId,
                theme = themeKeyword,
                artworks = seriesArtworks,
                emotionTransition = emotionTransition
            )

            val updated = _uiState.value.evolutionSeries.toMutableList()
            updated.add(series)
            _uiState.value = _uiState.value.copy(
                evolutionSeries = updated,
                isGenerating = false
            )
        }
    }

    private fun moodBasedPalette(mood: Int): List<String> {
        return when (mood) {
            1 -> listOf("#FFCD5C5C", "#FFB22222", "#FF8B0000", "#FFE53935", "#FFD32F2F")
            2 -> listOf("#FFFFA07A", "#FFFF8C00", "#FFFF6600", "#FFFF9800", "#FFFFB74D")
            3 -> listOf("#FFF5DEB3", "#FFDAA520", "#FFBDB76B", "#FFFFC107", "#FFFFE082")
            4 -> listOf("#FF98FB98", "#FF3CB371", "#FF2E8B57", "#FF4CAF50", "#FF81C784")
            5 -> listOf("#FF87CEEB", "#FF4682B4", "#FF191970", "#FF2196F3", "#FF64B5F6")
            6 -> listOf("#FFDDA0DD", "#FF9370DB", "#FF4B0082", "#FF9C27B0", "#FFBA68C8")
            else -> listOf("#FFA9A9A9", "#FF808080", "#FF696969", "#FF9E9E9E", "#FFBDBDBD")
        }
    }

    private fun buildTransitionDescription(entries: List<DiaryEntry>): String {
        if (entries.isEmpty()) return ""
        val firstMood = entries.first().moodLevel ?: 3
        val lastMood = entries.last().moodLevel ?: 3
        val moodLabels = mapOf(1 to "沮丧", 2 to "低落", 3 to "平静", 4 to "愉悦", 5 to "兴奋", 6 to "激动")
        return "对'${entries.first().title.take(10)}...'的感受从'${moodLabels[firstMood] ?: "平静"}'变'${moodLabels[lastMood] ?: "平静"}'"
    }

    fun generateCollage(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val entries = dao.getEntriesByDateRange(startTime, endTime)
            if (entries.isEmpty()) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
                return@launch
            }

            val collageId = "collage_${startTime}_${System.currentTimeMillis()}"
            val collageArtworks = mutableListOf<MemoryArtwork>()
            val dominantColors = mutableSetOf<Long>()

            entries.forEach { entry ->
                _uiState.value = _uiState.value.copy(selectedStyle = ArtStyle.ABSTRACT)
                generateArtLocal(entry)
                val current = _uiState.value.currentArtwork
                if (current != null) {
                    val collagedArtwork = current.copy(
                        id = "${collageId}_${entry.id}",
                        palette = moodBasedPalette(entry.moodLevel ?: 3)
                    )
                    collageArtworks.add(collagedArtwork)
                    saveArtwork(collagedArtwork)
                    moodBasedPalette(entry.moodLevel ?: 3).forEach { hex ->
                        dominantColors.add(colorHex(hex))
                    }
                }
            }

            val topColors = dominantColors.take(6).toList()

            val collage = MemoryCollage(
                id = collageId,
                entryIds = entries.map { it.id },
                period = "${startTime}_${endTime}",
                artworks = collageArtworks,
                dominantColors = if (topColors.isEmpty()) listOf(0xFF9E9E9E) else topColors
            )

            _uiState.value = _uiState.value.copy(
                collage = collage,
                isGenerating = false
            )
        }
    }

    val filteredArtworks: List<MemoryArtwork>
        get() {
            val state = _uiState.value
            var result = state.artworks
            if (state.showFavoritesOnly) {
                result = result.filter { it.isFavorite }
            }
            state.styleFilter?.let { style ->
                result = result.filter { it.style == style }
            }
            state.seriesFilter?.let { seriesId ->
                result = result.filter { it.seriesId == seriesId }
            }
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.lowercase()
                result = result.filter { it.style.lowercase().contains(q) || it.aiPrompt.lowercase().contains(q) }
            }
            return result.sortedByDescending { it.createdAt }
        }

    fun exportPrompt(artwork: MemoryArtwork, format: PromptFormat): String {
        val baseDescription = artwork.aiPrompt.takeIf { it.isNotBlank() }
            ?: "An abstract artwork in ${artwork.style} style with ${artwork.composition.shapes.size} elements"

        return when (format) {
            PromptFormat.MIDJOURNEY -> {
                "/imagine prompt: $baseDescription, digital art, ${artwork.style} style, " +
                    "colors: ${artwork.palette.take(3).joinToString(", ")}, " +
                    "---ar 1:1 --v 6"
            }
            PromptFormat.STABLE_DIFFUSION -> {
                "$baseDescription, ${artwork.style} style, " +
                    "color palette: ${artwork.palette.joinToString(", ")}, " +
                    "negative prompt: text, watermark, signature, " +
                    "steps: 30, sampler: DPM++ 2M Karras"
            }
            PromptFormat.DALLE -> {
                "$baseDescription, ${artwork.style} painting style, " +
                    "vibrant colors: ${artwork.palette.take(3).joinToString(", ")}"
            }
        }
    }

    fun toggleFavorite(artworkId: String) {
        val updated = _uiState.value.artworks.map { art ->
            if (art.id == artworkId) art.copy(isFavorite = !art.isFavorite) else art
        }
        saveAllArtworks(updated)
        val current = _uiState.value.currentArtwork
        _uiState.value = _uiState.value.copy(
            artworks = updated,
            currentArtwork = if (current?.id == artworkId) {
                current.copy(isFavorite = !current.isFavorite)
            } else current
        )
    }

    private fun saveArtwork(artwork: MemoryArtwork) {
        val all = _uiState.value.artworks.toMutableList()
        val idx = all.indexOfFirst { it.id == artwork.id }
        if (idx >= 0) all[idx] = artwork else all.add(artwork)
        saveAllArtworks(all)
        _uiState.value = _uiState.value.copy(artworks = all)
    }

    private fun saveAllArtworks(artworks: List<MemoryArtwork>) {
        val json = gson.toJson(artworks)
        prefs.edit().putString("artworks", json).apply()
    }

    private fun loadArtworks() {
        val json = prefs.getString("artworks", null) ?: return
        try {
            val type = object : TypeToken<List<MemoryArtwork>>() {}.type
            val artworks: List<MemoryArtwork> = gson.fromJson(json, type)
            _uiState.value = _uiState.value.copy(artworks = artworks)
        } catch (_: Exception) {}
    }
}
