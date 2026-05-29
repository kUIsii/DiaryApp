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
 * Shared element transition effect for diary title.
 * Simulates a shared element transition by animating scale and alpha.
 * Use on the destination screen to create a "pop-in" effect.
 */
@Composable
fun Modifier.sharedElementTransition(
    visible: Boolean,
    durationMillis: Int = 350
): Modifier {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(visible) {
        if (visible) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis)
            )
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    return this
        .graphicsLayer {
            translationY = offsetY.value
        }
        .scale(scale.value)
        .alpha(alpha.value)
}

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

/**
 * Bottom nav item transition effect.
 * Creates a smooth scale + fade effect when switching tabs.
 */
@Composable
fun Modifier.navItemTransition(selected: Boolean): Modifier {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        scale.animateTo(
            targetValue = if (selected) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    return this.scale(scale.value)
}
