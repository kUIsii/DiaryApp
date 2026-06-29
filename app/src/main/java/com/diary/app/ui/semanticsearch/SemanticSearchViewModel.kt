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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

data class SearchResult(
    val entry: DiaryPreview,
    val score: Float,
    val snippet: String
)

enum class SortOrder { RELEVANCE, DATE }

data class SemanticSearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isIndexing: Boolean = true,
    val hasSearched: Boolean = false,
    val sortOrder: SortOrder = SortOrder.RELEVANCE,
    val searchTimeMs: Long = 0L,
    val searchHistory: List<String> = emptyList(),
    val groupedResults: Map<String, List<SearchResult>> = emptyMap(),
    val groupByMonth: Boolean = false
)

class SemanticSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _state = MutableStateFlow(SemanticSearchState())
    val state: StateFlow<SemanticSearchState> = _state.asStateFlow()

    private var tfidfIndex: TfIdfIndex? = null
    private var cachedRawResults: List<SearchResult> = emptyList()

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
        val currentHistory = _state.value.searchHistory.toMutableList()
        if (query.isNotBlank() && !currentHistory.contains(query)) {
            currentHistory.add(0, query)
            if (currentHistory.size > 5) currentHistory.removeAt(5)
        }
        _state.value = _state.value.copy(query = query, hasSearched = true, searchHistory = currentHistory)
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), isSearching = false, groupedResults = emptyMap())
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            val startTime = System.currentTimeMillis()
            val results = withContext(Dispatchers.Default) { computeSearch(query) }
            val elapsed = System.currentTimeMillis() - startTime
            cachedRawResults = results
            val stateVal = _state.value
            val sorted = sortResults(results, stateVal.sortOrder)
            val grouped = if (stateVal.groupByMonth) groupByMonth(sorted) else emptyMap()
            _state.value = _state.value.copy(results = sorted, isSearching = false, searchTimeMs = elapsed, groupedResults = grouped)
        }
    }

    fun setSortOrder(order: SortOrder) {
        _state.value = _state.value.copy(sortOrder = order)
        val sorted = sortResults(cachedRawResults, order)
        val grouped = if (_state.value.groupByMonth) groupByMonth(sorted) else emptyMap()
        _state.value = _state.value.copy(results = sorted, groupedResults = grouped)
    }

    fun setGroupByMonth(enabled: Boolean) {
        _state.value = _state.value.copy(groupByMonth = enabled)
        val grouped = if (enabled) groupByMonth(_state.value.results) else emptyMap()
        _state.value = _state.value.copy(groupedResults = grouped)
    }

    fun searchFromHistory(query: String) {
        search(query)
    }

    private fun sortResults(results: List<SearchResult>, order: SortOrder): List<SearchResult> {
        return when (order) {
            SortOrder.RELEVANCE -> results.sortedByDescending { it.score }
            SortOrder.DATE -> results.sortedByDescending { it.entry.createdAt }
        }
    }

    private fun groupByMonth(results: List<SearchResult>): Map<String, List<SearchResult>> {
        val sdf = SimpleDateFormat("yyyy\u5E74MM\u6708", Locale.getDefault())
        return results.groupBy { sdf.format(Date(it.entry.createdAt)) }
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
                val start = (idx - 40).coerceAtLeast(0)
                val end = (idx + query.length + 80).coerceAtMost(entry.plainText.length)
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
