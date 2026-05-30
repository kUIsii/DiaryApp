package com.diary.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Unified animation configuration for a softer, more gentle feel.
 */
object AnimationConfig {
    // Spring animations - softer damping
    val SpringGentle = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpringSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // Tween animations - smoother transitions
    val TweenFast = tween<Float>(150)
    val TweenNormal = tween<Float>(250)
    val TweenSlow = tween<Float>(400)

    // Specific use cases
    val ButtonPress = SpringGentle
    val CardScale = SpringGentle
    val PanelExpand = TweenNormal
    val FadeIn = TweenNormal
    val FadeOut = TweenFast
    val ListItemEnter = tween<Float>(300)
}
