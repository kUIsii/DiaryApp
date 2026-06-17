package com.diary.app.ui.map

import android.os.Bundle
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewList
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
import com.amap.api.maps.model.BitmapDescriptorFactory
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
    var showMap by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }

    // Get unique locations with counts
    val locations = remember(state.markers) {
        state.markers
            .filter { it.location.isNotBlank() }
            .groupBy { it.location }
            .map { (location, markers) ->
                LocationGroup(
                    name = location,
                    count = markers.size,
                    markers = markers,
                    // Use the first marker's coordinates for the location
                    latitude = markers.first().latitude,
                    longitude = markers.first().longitude
                )
            }
            .sortedByDescending { it.count }
    }

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
                        text = "足迹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${locations.size} 个地点，${state.markers.size} 篇日记",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle map/list view
                if (showMap) {
                    IconButton(
                        onClick = {
                            showMap = false
                            selectedLocation = null
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = "列表",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        subtitle = "写日记时添加位置信息，就能在这里看到",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                showMap -> {
                    // Map view showing selected location
                    MapViewWithLocation(
                        location = selectedLocation,
                        markers = if (selectedLocation != null) {
                            locations.find { it.name == selectedLocation }?.markers ?: emptyList()
                        } else {
                            state.markers
                        },
                        onMarkerClick = { markerId ->
                            onNavigateToDetail(markerId)
                        },
                        onBack = {
                            showMap = false
                            selectedLocation = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    // Location list
                    LocationList(
                        locations = locations,
                        onLocationClick = { location ->
                            selectedLocation = location.name
                            showMap = true
                        }
                    )
                }
            }
        }
    }
}

data class LocationGroup(
    val name: String,
    val count: Int,
    val markers: List<MapMarker>,
    val latitude: Double,
    val longitude: Double
)

@Composable
private fun LocationList(
    locations: List<LocationGroup>,
    onLocationClick: (LocationGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(locations) { location ->
            LocationItem(
                location = location,
                onClick = { onLocationClick(location) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LocationItem(
    location: LocationGroup,
    onClick: () -> Unit
) {
    GlassCard(
        cornerRadius = 16.dp,
        innerPadding = 16.dp,
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
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
                    text = location.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${location.count} 篇日记",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.Map,
                contentDescription = "查看地图",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MapViewWithLocation(
    location: String?,
    markers: List<MapMarker>,
    onMarkerClick: (Long) -> Unit,
    onBack: () -> Unit,
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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

                        // Zoom to show all markers
                        try {
                            val bounds = boundsBuilder.build()
                            aMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 100)
                            )
                        } catch (e: Exception) {
                            // Fallback to first marker
                            val first = markers.first()
                            aMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(first.latitude, first.longitude),
                                    15f
                                )
                            )
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
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DiaryMapScreen", "Failed to update map", e)
                }
            }
        )

        // Location name overlay
        if (location != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                GlassCard(
                    cornerRadius = 12.dp,
                    innerPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = location,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${markers.size} 篇日记",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
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
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timestamp))
}
