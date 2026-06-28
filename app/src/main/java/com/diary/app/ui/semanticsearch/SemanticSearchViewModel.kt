package com.diary.app.ui.semanticsearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.sqrt

data class SearchResult(
    val entry: DiaryPreview,
    val score: Float,
    val snippet: String
)

data class SemanticSearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isIndexing: Boolean = true,
    val hasSearched: Boolean = false
)

class SemanticSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _state = MutableStateFlow(SemanticSearchState())
    val state: StateFlow<SemanticSearchState> = _state.asStateFlow()

    private var tfidfIndex: TfIdfIndex? = null

    data class TfIdfIndex(
        val entries: List<DiaryPreview>,
        val totalDocs: Int,
        val termDocFreq: Map<String, Int>,
        val docVectors: List<Map<String, Float>>
    )

    init {
        buildIndex()
    }

    private fun buildIndex() {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            val termDocFreq = mutableMapOf<String, MutableSet<Long>>()
            val docVectors = mutableListOf<Map<String, Float>>()

            for (entry in entries) {
                val words = tokenize(entry.title + " " + entry.plainText)
                val tf = mutableMapOf<String, Float>()
                for (word in words) {
                    tf[word] = (tf[word] ?: 0f) + 1f
                }
                val maxTf = tf.values.maxOrNull() ?: 1f
                val normalized = tf.mapValues { it.value / maxTf }
                docVectors.add(normalized)

                for (word in words) {
                    termDocFreq.getOrPut(word) { mutableSetOf() }.add(entry.id)
                }
            }

            tfidfIndex = TfIdfIndex(
                entries = entries,
                totalDocs = entries.size,
                termDocFreq = termDocFreq.mapValues { it.value.size },
                docVectors = docVectors
            )

            _state.value = _state.value.copy(isIndexing = false)
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query, hasSearched = true)
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), isSearching = false)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            val results = withContext(Dispatchers.Default) { computeSearch(query) }
            _state.value = _state.value.copy(results = results, isSearching = false)
        }
    }

    private fun computeSearch(query: String): List<SearchResult> {
        val index = tfidfIndex ?: return emptyList()
        val queryWords = tokenize(query).distinct()
        if (queryWords.isEmpty()) return emptyList()

        val queryVec = mutableMapOf<String, Float>()
        for (word in queryWords) {
            val tf = 1f
            val idf = ln((index.totalDocs.toFloat() + 1) / (1 + (index.termDocFreq[word] ?: 0).toFloat())) + 1
            queryVec[word] = tf * idf
        }
        var queryNormSq = 0.0
        for ((_, w) in queryVec) { queryNormSq += (w * w).toDouble() }
        val queryNorm = sqrt(queryNormSq).toFloat()

        val scored = index.entries.mapIndexed { i, entry ->
            val docVec = index.docVectors[i]
            var dotProduct = 0f
            for ((word, qWeight) in queryVec) {
                val idf = ln((index.totalDocs.toFloat() + 1) / (1 + (index.termDocFreq[word] ?: 0).toFloat())) + 1
                val docWeight = (docVec[word] ?: 0f) * idf
                dotProduct += qWeight * docWeight
            }
            var docNormSq = 0.0
            for ((word, tf) in docVec) {
                val idf = ln((index.totalDocs.toFloat() + 1) / (1 + (index.termDocFreq[word] ?: 0).toFloat())) + 1
                val w = tf * idf
                docNormSq += (w * w).toDouble()
            }
            val docNorm = sqrt(docNormSq).toFloat()

            val score = if (queryNorm > 0f && docNorm > 0f) dotProduct / (queryNorm * docNorm) else 0f

            var snippet = entry.plainText.take(150)
            val lowerText = entry.plainText.lowercase()
            val lowerQuery = query.lowercase()
            val idx = lowerText.indexOf(lowerQuery)
            if (idx >= 0) {
                val start = (idx - 30).coerceAtLeast(0)
                val end = (idx + query.length + 60).coerceAtMost(entry.plainText.length)
                snippet = (if (start > 0) "..." else "") + entry.plainText.substring(start, end) + (if (end < entry.plainText.length) "..." else "")
            }

            SearchResult(entry = entry, score = score, snippet = snippet)
        }

        return scored.filter { it.score > 0.01f }.sortedByDescending { it.score }.take(30)
    }

    private fun tokenize(text: String): List<String> {
        val cleaned = text.lowercase()
            .replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9 ]"), " ")
        val asciiWords = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() && it.length <= 50 }

        val result = mutableListOf<String>()
        for (word in asciiWords) {
            if (word.all { it in 'a'..'z' || it in '0'..'9' }) {
                result.add(word)
            } else {
                val chineseOnly = word.filter { it in '\u4e00'..'\u9fa5' || it in 'a'..'z' || it in '0'..'9' }
                if (chineseOnly.length <= 20) {
                    result.add(chineseOnly)
                }
                for (i in 0 until chineseOnly.length - 1) {
                    val bigram = chineseOnly.substring(i, i + 2)
                    if (bigram.all { it in '\u4e00'..'\u9fa5' }) {
                        result.add(bigram)
                    }
                }
            }
        }
        return result.distinct()
    }
}
