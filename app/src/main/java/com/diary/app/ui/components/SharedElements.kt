package com.diary.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Staggered list item animation modifier.
 * Items appear one by one with a slight delay, creating a cascade effect.
 */
@Composable
fun Modifier.staggeredListItem(
    index: Int,
    visible: Boolean = true,
    initialDelayMs: Int = 0,
    itemDelayMs: Int = 50
): Modifier {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }
    val scale = remember { Animatable(0.95f) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay((initialDelayMs + index * itemDelayMs).toLong())
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(250)
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    return this
        .graphicsLayer {
            translationY = offsetY.value
        }
        .scale(scale.value)
        .alpha(alpha.value)
}

@Composable
fun PageHeader(
    title: String,
    onNavigateBack: () -> Unit,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        if (action != null) {
            action()
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

