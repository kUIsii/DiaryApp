package com.diary.app.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.pressableScale(
    scale: Float = 0.97f,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = animationSpec,
        label = "pressableScale"
    )

    this
        .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        )
}

fun Modifier.pressableScale(
    onClick: () -> Unit,
    scale: Float = 0.97f,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = animationSpec,
        label = "pressableScale"
    )

    this
        .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
