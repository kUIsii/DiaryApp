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
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground

@Composable
fun DiaryMapScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMap by remember { mutableStateOf(true) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }

    val locations = remember(state.markers) {
        state.markers
            .filter { it.location.isNotBlank() }
            .groupBy { it.location }
            .map { (location, markers) ->
                LocationGroup(
                    name = location,
                    count = markers.size,
                    markers = markers,
                    latitude = markers.first().latitude,
                    longitude = markers.first().longitude
                )
            }
            .sortedByDescending { it.count }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            DiaryMapHeader(
                locationCount = locations.size,
                entryCount = state.markers.size,
                showMap = showMap,
                onNavigateBack = onNavigateBack,
                onSwitchToList = {
                    showMap = false
                    selectedLocation = null
                }
            )

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
                        subtitle = "写日记时补充位置，这里就能按地点把回忆整理成入口。",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                showMap -> {
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
                    LocationList(
                        locations = locations,
                        onLocationClick = { location ->
                            selectedLocation = location.name
                            showMap = true
                        },
                        onEntryClick = { entryId ->
                            onNavigateToDetail(entryId)
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
    val longitude: Double,
    val latestMarker: MapMarker? = markers.maxByOrNull { it.createdAt }
)

@Composable
private fun DiaryMapHeader(
    locationCount: Int,
    entryCount: Int,
    showMap: Boolean,
    onNavigateBack: () -> Unit,
    onSwitchToList: () -> Unit
) {
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "日记地图",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$locationCount 个地点，$entryCount 篇位置日记",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showMap) {
            IconButton(
                onClick = onSwitchToList,
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
}

@Composable
private fun LocationList(
    locations: List<LocationGroup>,
    onLocationClick: (LocationGroup) -> Unit,
    onEntryClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LocationOverviewCard(locations = locations)
        }

        items(locations) { location ->
            LocationItem(
                location = location,
                onClick = { onLocationClick(location) },
                onEntryClick = onEntryClick
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LocationOverviewCard(locations: List<LocationGroup>) {
    val topLocation = locations.maxByOrNull { it.count }
    val totalEntries = locations.sumOf { it.count }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$totalEntries 篇位置日记",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "分布在 ${locations.size} 个地点",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (topLocation != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "最常去",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topLocation.name.take(8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationItem(
    location: LocationGroup,
    onClick: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val latest = location.latestMarker
    val latestPreview = latest?.plainText?.take(40)?.let {
        if (it.length >= 40) "$it..." else it
    }?.ifBlank { null }

    GlassCard(
        cornerRadius = 18.dp,
        innerPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.Top
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${location.count} 篇",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latest != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEntryClick(latest.id) }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = latest.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (latestPreview != null) {
                            Text(
                                text = latestPreview,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.Map,
                contentDescription = "查看地图",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
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

                        try {
                            val bounds = boundsBuilder.build()
                            aMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 100)
                            )
                        } catch (_: Exception) {
                            val first = markers.first()
                            aMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(first.latitude, first.longitude),
                                    15f
                                )
                            )
                        }

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

