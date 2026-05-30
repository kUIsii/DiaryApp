package com.diary.app.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.Tag

@Composable
fun TagEditor(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    onAddTag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        allTags.forEach { tag ->
            val isSelected = tag.id in selectedTagIds
            val tagColor = Color(tag.color)

            TagChip(
                tagName = tag.name,
                tagColor = tagColor,
                isSelected = isSelected,
                onClick = { onTagToggle(tag.id) }
            )
        }

        // 新建标签按钮
        AddTagButton(onClick = onAddTag)
    }
}

@Composable
private fun TagChip(
    tagName: String,
    tagColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 按压缩放动画
    var pressed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "tag_press_scale"
    )

    // 选中弹性动画
    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.97f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "tag_selected_scale"
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .scale(scale * selectedScale)
            .clip(shape)
            .background(
                if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(
                            tagColor.copy(alpha = 0.2f),
                            tagColor.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) tagColor else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) tagColor else tagColor.copy(alpha = 0.6f)
                    )
            )
            Text(
                text = tagName,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }

    // 重置 pressed 状态
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Composable
private fun AddTagButton(onClick: () -> Unit) {
    // 按压缩放动画
    var pressed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "add_tag_press_scale"
    )

    val shape = RoundedCornerShape(20.dp)
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 虚线边框 - 使用 Canvas 绘制
        Canvas(modifier = Modifier.matchParentSize()) {
            val dashLength = 8f
            val gapLength = 6f
            val strokeWidth = 1.5.dp.toPx()
            val cornerRadius = 20.dp.toPx()
            val rect = androidx.compose.ui.geometry.Rect(
                offset = Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth
                )
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = rect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                    )
                )
            }
            drawPath(
                path = path,
                color = dashColor,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashLength, gapLength), 0f
                    )
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建标签",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "新建标签",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // 重置 pressed 状态
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}
