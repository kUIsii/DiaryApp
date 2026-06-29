package com.diary.app.ui.entrygraph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryTagPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class GraphNode(
    val entryId: Long,
    val title: String,
    val moodLevel: Int?,
    val wordCount: Int,
    val x: Float,
    val y: Float
)

data class GraphEdge(
    val fromId: Long,
    val toId: Long,
    val type: EdgeType,
    val label: String
)

enum class EdgeType { TAG, PERSON, ANCHOR, LOCATION }

data class GraphState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val selectedEntryId: Long? = null,
    val isLoading: Boolean = true
)

class EntryGraphViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val (nodes, edges) = withContext(Dispatchers.IO) { computeGraph() }
            _state.value = GraphState(nodes = nodes, edges = edges, isLoading = false)
        }
    }

    fun selectEntry(entryId: Long?) {
        _state.value = _state.value.copy(selectedEntryId = entryId)
    }

    private suspend fun computeGraph(): Pair<List<GraphNode>, List<GraphEdge>> {
        val entries = dao.getAllEntriesOnce()
            .sortedByDescending { it.createdAt }
            .take(40)
        if (entries.isEmpty()) return emptyList<GraphNode>() to emptyList<GraphEdge>()

        val entryIds = entries.map { it.id }.toSet()
        val allTags = dao.getAllDiaryTagPairsOnce()

        val tagToEntries: Map<Long, List<Long>> = allTags.groupBy(DiaryTagPair::tagId, DiaryTagPair::diaryId)

        val edges = mutableSetOf<GraphEdge>()

        for ((tagId, diaryIds) in tagToEntries) {
            val filtered = diaryIds.filter { it in entryIds }
            val tagName = allTags.firstOrNull { it.tagId == tagId }?.name ?: ""
            for (i in filtered.indices) {
                for (j in i + 1 until filtered.size) {
                    edges.add(GraphEdge(filtered[i], filtered[j], EdgeType.TAG, tagName))
                }
            }
        }

        val radius = 260f
        val angleStep = (2.0 * Math.PI / entries.size).toFloat()
        val nodes = entries.mapIndexed { index, entry ->
            val angle = index * angleStep
            GraphNode(
                entryId = entry.id,
                title = entry.title.ifBlank { "无标题" },
                moodLevel = entry.moodLevel,
                wordCount = entry.plainText.length,
                x = (radius * cos(angle.toDouble())).toFloat(),
                y = (radius * sin(angle.toDouble())).toFloat()
            )
        }

        val refinedNodes = refineLayout(nodes, edges.toList(), 60)
        return refinedNodes to edges.toList()
    }

    private fun refineLayout(
        nodes: List<GraphNode>,
        edges: List<GraphEdge>,
        iterations: Int
    ): List<GraphNode> {
        val positions = nodes.mapIndexed { idx, node ->
            if (idx == 0) floatArrayOf(0f, 0f) to node.entryId
            else floatArrayOf(node.x, node.y) to node.entryId
        }.toMutableList()

        val repulsion = 8000f
        val attraction = 0.005f
        val damping = 0.85f
        val vel = positions.map { floatArrayOf(0f, 0f) }.toMutableList()

        for (iter in 0 until iterations) {
            for (i in positions.indices) {
                var fx = 0f
                var fy = 0f

                for (j in positions.indices) {
                    if (i == j) continue
                    val dx = positions[i].first[0] - positions[j].first[0]
                    val dy = positions[i].first[1] - positions[j].first[1]
                    val dist = max(sqrt(dx * dx + dy * dy), 1f)
                    fx += (dx / dist) * repulsion / (dist * dist)
                    fy += (dy / dist) * repulsion / (dist * dist)
                }

                for (edge in edges) {
                    val fromIdx = positions.indexOfFirst { it.second == edge.fromId }
                    val toIdx = positions.indexOfFirst { it.second == edge.toId }
                    if (fromIdx < 0 || toIdx < 0) continue
                    if (i != fromIdx && i != toIdx) continue
                    val otherIdx = if (i == fromIdx) toIdx else fromIdx
                    val dx = positions[otherIdx].first[0] - positions[i].first[0]
                    val dy = positions[otherIdx].first[1] - positions[i].first[1]
                    val dist = max(sqrt(dx * dx + dy * dy), 1f)
                    fx += dx * attraction * dist
                    fy += dy * attraction * dist
                }

                vel[i][0] = (vel[i][0] + fx) * damping
                vel[i][1] = (vel[i][1] + fy) * damping
                positions[i].first[0] += vel[i][0]
                positions[i].first[1] += vel[i][1]
            }
        }

        val maxCoord = positions.maxOf { max(abs(it.first[0]), abs(it.first[1])) }
        val scale = if (maxCoord > 0f) 240f / maxCoord else 1f

        return nodes.map { node ->
            val pos = positions.first { it.second == node.entryId }
            node.copy(
                x = pos.first[0] * scale,
                y = pos.first[1] * scale
            )
        }
    }

    fun navigateToEntry(onNavigateToDetail: (Long) -> Unit) {
        val id = _state.value.selectedEntryId ?: return
        onNavigateToDetail(id)
    }
}
