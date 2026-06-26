package com.diary.app.ui.map

import kotlin.math.roundToInt

data class HeatmapSpot(
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val radiusMeters: Double,
    val alpha: Float
)

internal fun buildHeatmapSpots(
    markers: List<MapMarker>,
    thresholdMeters: Double = 150.0
): List<HeatmapSpot> {
    if (markers.isEmpty()) return emptyList()

    val clusters = clusterLocations(markers, thresholdMeters)
    val maxCount = clusters.maxOfOrNull { it.count } ?: 1

    return clusters.map { cluster ->
        val normalized = (cluster.count.toFloat() / maxCount).coerceIn(0f, 1f)
        HeatmapSpot(
            latitude = cluster.latitude,
            longitude = cluster.longitude,
            count = cluster.count,
            radiusMeters = 70.0 + (normalized * 190.0),
            alpha = (0.20f + normalized * 0.45f).coerceIn(0.20f, 0.65f)
        )
    }.sortedByDescending { it.count }
}

internal fun heatmapSpotColorArgb(count: Int, maxCount: Int): Int {
    val safeMax = maxCount.coerceAtLeast(1)
    val normalized = (count.toFloat() / safeMax).coerceIn(0f, 1f)
    val red = (255 - normalized * 18f).roundToInt().coerceIn(0, 255)
    val green = (184 - normalized * 82f).roundToInt().coerceIn(0, 255)
    val blue = (112 - normalized * 52f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
