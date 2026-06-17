package com.diary.app.ui.map

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun DiaryMapScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showList by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<MapMarker?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar - minimal, floating style
            MapTopBar(
                markerCount = state.markers.size,
                showList = showList,
                onNavigateBack = onNavigateBack,
                onToggleView = { showList = !showList }
            )

            // Stats card
            if (!state.isLoading && state.markers.isNotEmpty()) {
                MapStatsCard(stats = state.stats)
            }

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.markers.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.LocationOn,
                        title = "还没有带位置的日记",
                        subtitle = "写日记时添加位置信息，就能在地图上看到",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showList) {
                            // List view grouped by location
                            DiaryLocationList(
                                markers = state.markers,
                                onMarkerClick = { marker ->
                                    selectedMarker = marker
                                    showList = false
                                },
                                onNavigateToDetail = onNavigateToDetail
                            )
                        } else {
                            // Full screen map with overlay
                            Box(modifier = Modifier.fillMaxSize()) {
                                AmapView(
                                    markers = state.markers,
                                    selectedMarker = selectedMarker,
                                    onMarkerClick = { markerId ->
                                        val marker = state.markers.find { it.id == markerId }
                                        selectedMarker = marker
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Bottom sheet - marker info
                                if (selectedMarker != null) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    ) {
                                        selectedMarker?.let { marker ->
                                            MarkerInfoSheet(
                                                marker = marker,
                                                onClick = {
                                                    onNavigateToDetail(marker.id)
                                                    selectedMarker = null
                                                },
                                                onDismiss = { selectedMarker = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapTopBar(
    markerCount: Int,
    showList: Boolean,
    onNavigateBack: () -> Unit,
    onToggleView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "足迹地图",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$markerCount 个地点",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Toggle view button
        IconButton(
            onClick = onToggleView,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (showList) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
        ) {
            Icon(
                if (showList) Icons.Default.Map else Icons.Default.List,
                contentDescription = if (showList) "地图" else "列表",
                tint = if (showList) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MapStatsCard(stats: MapStats) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.totalEntries.toString(),
                label = "日记"
            )
            StatItem(
                value = stats.uniqueLocations.toString(),
                label = "地点"
            )
            StatItem(
                value = stats.citiesVisited.toString(),
                label = "城市"
            )
            if (stats.firstEntryDate != null) {
                StatItem(
                    value = calculateDuration(stats.firstEntryDate),
                    label = "记录"
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateDuration(startDate: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - startDate
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        days < 30 -> "${days}天"
        days < 365 -> "${days / 30}月"
        else -> "${days / 365}年"
    }
}

@Composable
private fun MarkerInfoSheet(
    marker: MapMarker,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        GlassCard(
            cornerRadius = 20.dp,
            innerPadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = marker.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (marker.location.isNotBlank()) {
                        Text(
                            text = marker.location,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(marker.createdAt),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryLocationList(
    markers: List<MapMarker>,
    onMarkerClick: (MapMarker) -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    // Group by location
    val grouped = markers.groupBy { it.location.ifBlank { "未知位置" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (location, locationMarkers) ->
            // Location header
            item {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = location,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${locationMarkers.size} 篇",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Diary items
            items(locationMarkers) { marker ->
                DiaryLocationItem(
                    marker = marker,
                    onClick = { onNavigateToDetail(marker.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DiaryLocationItem(
    marker: MapMarker,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(marker.createdAt),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AmapView(
    markers: List<MapMarker>,
    selectedMarker: MapMarker?,
    onMarkerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            mapView.onCreate(Bundle())
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
            try {
                val aMap = mv.map ?: return@AndroidView

                aMap.clear()

                if (markers.isNotEmpty()) {
                    val boundsBuilder = LatLngBounds.Builder()

                    markers.forEach { marker ->
                        val position = LatLng(marker.latitude, marker.longitude)
                        val markerOptions = MarkerOptions()
                            .position(position)
                            .title(marker.title)
                            .snippet(marker.location.ifBlank { null })
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                if (marker.id == selectedMarker?.id)
                                    BitmapDescriptorFactory.HUE_AZURE
                                else
                                    BitmapDescriptorFactory.HUE_RED
                            ))

                        aMap.addMarker(markerOptions)
                        boundsBuilder.include(position)
                    }

                    // Zoom to selected or show all
                    if (selectedMarker != null) {
                        aMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(selectedMarker.latitude, selectedMarker.longitude),
                                15f
                            )
                        )
                    } else {
                        try {
                            val bounds = boundsBuilder.build()
                            aMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 100)
                            )
                        } catch (e: Exception) {
                            aMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(35.86, 104.19), 4f)
                            )
                        }
                    }

                    // Marker click
                    aMap.setOnMarkerClickListener { amapMarker ->
                        val clickedMarker = markers.find {
                            it.latitude == amapMarker.position.latitude &&
                            it.longitude == amapMarker.position.longitude
                        }
                        clickedMarker?.let { onMarkerClick(it.id) }
                        true
                    }

                    // Map click to dismiss selection
                    aMap.setOnMapClickListener {
                        // Could add logic here to dismiss selected marker
                    }
                } else {
                    aMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(35.86, 104.19), 4f)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timestamp))
}
