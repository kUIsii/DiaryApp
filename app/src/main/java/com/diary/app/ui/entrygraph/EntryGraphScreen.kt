package com.diary.app.ui.entrygraph

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import kotlin.math.sqrt

@Composable
fun EntryGraphScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: EntryGraphViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = "条目关联图谱",
                onNavigateBack = onNavigateBack
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("计算布局中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (state.nodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("写一些日记后再来看看", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    GraphCanvas(state = state, onNodeClick = { viewModel.selectEntry(it) })
                }
            }

            state.selectedEntryId?.let { selectedId ->
                val node = state.nodes.find { it.entryId == selectedId }
                if (node != null) {
                    val connectedEdges = state.edges.filter {
                        it.fromId == selectedId || it.toId == selectedId
                    }
                    val tagLabels = connectedEdges.filter { it.type == EdgeType.TAG }.map { it.label }.distinct()
                    val personLabels = connectedEdges.filter { it.type == EdgeType.PERSON }.map { it.label }.distinct()

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        cornerRadius = 16.dp,
                        innerPadding = 12.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        node.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        "${node.wordCount}字 · ${connectedEdges.size}条关联",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.selectEntry(null) }) {
                                    Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (tagLabels.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "共同标签: ${tagLabels.joinToString("、")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (personLabels.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "共同人物: ${personLabels.joinToString("、")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = { viewModel.navigateToEntry(onNavigateToDetail) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Article, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("查看日记", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GraphCanvas(state: GraphState, onNodeClick: (Long) -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (state.selectedEntryId != null) 1.15f else 1f,
        animationSpec = tween(300)
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(state.nodes) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val press = event.changes.firstOrNull() ?: break
                        if (press.pressed) {
                            press.consume()
                            val px = press.position.x - size.width / 2f
                            val py = press.position.y - size.height / 2f
                            val tapped = state.nodes.minByOrNull { node ->
                                val dx = node.x * scale - px
                                val dy = node.y * scale - py
                                dx * dx + dy * dy
                            }
                            if (tapped != null) {
                                val dx = tapped.x * scale - px
                                val dy = tapped.y * scale - py
                                if (sqrt(dx * dx + dy * dy) < 50f) {
                                    onNodeClick(tapped.entryId)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        for (edge in state.edges) {
            val from = state.nodes.find { it.entryId == edge.fromId } ?: continue
            val to = state.nodes.find { it.entryId == edge.toId } ?: continue
            val color = when (edge.type) {
                EdgeType.TAG -> Color(0xFF90CAF9)
                EdgeType.PERSON -> Color(0xFFCE93D8)
                EdgeType.ANCHOR -> Color(0xFFA5D6A7)
                EdgeType.LOCATION -> Color(0xFFFFCC80)
            }
            val isConnected = state.selectedEntryId != null &&
                    (edge.fromId == state.selectedEntryId || edge.toId == state.selectedEntryId)
            drawLine(
                color = color.copy(alpha = if (isConnected) 0.6f else 0.15f),
                start = Offset(cx + from.x * scale, cy + from.y * scale),
                end = Offset(cx + to.x * scale, cy + to.y * scale),
                strokeWidth = if (isConnected) 2f else 0.8f
            )
        }

        for (node in state.nodes) {
            val moodColor = when (node.moodLevel) {
                1 -> Color(0xFF90CAF9)
                2 -> Color(0xFF64B5F6)
                3 -> Color(0xFFA5D6A7)
                4 -> Color(0xFFFFF59D)
                5 -> Color(0xFFFFCC80)
                6 -> Color(0xFFEF9A9A)
                else -> Color(0xFFBDBDBD)
            }
            val isSelected = node.entryId == state.selectedEntryId
            val nodeRadius = if (isSelected) 18f
            else (10f + (node.wordCount.toFloat() / 500f).coerceIn(3f, 12f))
            val visible = state.selectedEntryId == null || isSelected ||
                    state.edges.any { it.fromId == node.entryId || it.toId == node.entryId }
            val alpha = if (visible) 1f else 0.2f

            drawCircle(
                color = moodColor.copy(alpha = alpha),
                radius = nodeRadius,
                center = Offset(cx + node.x * scale, cy + node.y * scale)
            )

            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = nodeRadius + 3f,
                    center = Offset(cx + node.x * scale, cy + node.y * scale),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
