package com.diary.app.ui.map

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
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
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiaryMapScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showList by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "日记地图",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.markers.size} 个地点",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle list view button
                if (state.markers.isNotEmpty()) {
                    IconButton(
                        onClick = { showList = !showList },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (showList) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            if (showList) Icons.Default.Map else Icons.Default.List,
                            contentDescription = if (showList) "地图" else "列表",
                            tint = if (showList) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
                    if (showList) {
                        // List view
                        LocationList(
                            markers = state.markers,
                            onMarkerClick = { marker ->
                                viewModel.selectMarker(marker)
                                showList = false
                            },
                            onNavigateToDetail = onNavigateToDetail,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Map view
                        Box(modifier = Modifier.weight(1f)) {
                            AmapView(
                                markers = state.markers,
                                selectedMarker = state.selectedMarker,
                                onMarkerClick = { markerId ->
                                    val marker = state.markers.find { it.id == markerId }
                                    viewModel.selectMarker(marker)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Selected marker info card
                            state.selectedMarker?.let { marker ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                ) {
                                    MarkerInfoCard(
                                        marker = marker,
                                        onClick = {
                                            onNavigateToDetail(marker.id)
                                            viewModel.selectMarker(null)
                                        }
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

@Composable
private fun MarkerInfoCard(
    marker: MapMarker,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (marker.location.isNotBlank()) {
                    Text(
                        text = marker.location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatDate(marker.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "查看",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LocationList(
    markers: List<MapMarker>,
    onMarkerClick: (MapMarker) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group markers by location
    val groupedMarkers = markers.groupBy { it.location.ifBlank { "未知位置" } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedMarkers.forEach { (location, locationMarkers) ->
            item {
                Text(
                    text = location,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(locationMarkers) { marker ->
                LocationListItem(
                    marker = marker,
                    onClick = { onNavigateToDetail(marker.id) }
                )
            }
        }
    }
}

@Composable
private fun LocationListItem(
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
                    fontSize = 12.sp,
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
                Lifecycle.Event.ON_START -> {}
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {}
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                Lifecycle.Event.ON_ANY -> {}
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

                        aMap.addMarker(markerOptions)
                        boundsBuilder.include(position)
                    }

                    // If a marker is selected, zoom to it
                    if (selectedMarker != null) {
                        aMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(selectedMarker.latitude, selectedMarker.longitude),
                                15f
                            )
                        )
                    } else {
                        // Show all markers
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

                    aMap.setOnMarkerClickListener { amapMarker ->
                        val clickedMarker = markers.find {
                            it.latitude == amapMarker.position.latitude &&
                            it.longitude == amapMarker.position.longitude
                        }
                        clickedMarker?.let { onMarkerClick(it.id) }
                        true
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
