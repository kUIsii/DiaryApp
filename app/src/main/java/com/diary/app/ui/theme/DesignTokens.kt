package com.diary.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified design tokens for consistent UI across the app.
 * Based on 4dp grid system.
 */
object DesignTokens {
    // Corner Radius
    val CornerSmall = 8.dp      // Small elements: chips, tags
    val CornerMedium = 12.dp    // Medium elements: buttons, inputs, small cards
    val CornerLarge = 16.dp     // Large elements: main cards, dialogs
    val CornerXLarge = 20.dp    // Extra large: bottom sheets, full-screen cards

    // Spacing (4dp grid)
    val SpacingXs = 4.dp        // Tight spacing
    val SpacingSm = 8.dp        // Small spacing
    val SpacingMd = 12.dp       // Medium spacing
    val SpacingLg = 16.dp       // Large spacing
    val SpacingXl = 20.dp       // Extra large spacing
    val SpacingXxl = 24.dp      // Page margins

    // Font Sizes
    val FontCaption = 11.sp     // Captions, labels
    val FontSmall = 12.sp       // Secondary text
    val FontBody = 14.sp        // Body text
    val FontMedium = 16.sp      // Medium text, subtitles
    val FontLarge = 18.sp       // Large text
    val FontTitle = 20.sp       // Titles
    val FontHeadline = 24.sp    // Headlines
    val FontDisplay = 28.sp     // Display text

    // Icon Sizes
    val IconSmall = 14.dp       // Small icons
    val IconMedium = 18.dp      // Medium icons
    val IconLarge = 24.dp       // Large icons

    // Animation Durations (ms)
    const val AnimationFast = 150
    const val AnimationNormal = 250
    const val AnimationSlow = 400

    // Line Heights
    val LineHeightTight = 1.4f
    val LineHeightNormal = 1.6f
    val LineHeightRelaxed = 2.0f
}
