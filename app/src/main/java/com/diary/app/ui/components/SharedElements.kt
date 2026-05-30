package com.diary.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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

