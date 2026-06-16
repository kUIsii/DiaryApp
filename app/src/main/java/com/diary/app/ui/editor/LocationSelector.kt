package com.diary.app.ui.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.diary.app.data.RecentLocation
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationSelector(
    selectedLocation: String?,
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (String?, Double?, Double?) -> Unit,
    recentLocations: List<RecentLocation> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGettingLocation by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    // Edit location dialog
    if (showEditDialog && selectedLocation != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEditDialog = false }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Text(
                    text = "编辑位置名称",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入位置名称") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (editName.isNotBlank()) {
                            onLocationSelected(editName.trim(), latitude, longitude)
                        }
                        showEditDialog = false
                    }) {
                        Text("确认")
                    }
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            getCurrentLocation(context) { locationName, lat, lng ->
                onLocationSelected(locationName, lat, lng)
                isGettingLocation = false
            }
        } else {
            isGettingLocation = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedLocation != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable {
                        editName = selectedLocation
                        showEditDialog = true
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedLocation,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "编辑",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onLocationSelected(null, null, null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "清除",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocationActionButton(
                    icon = Icons.Default.MyLocation,
                    label = if (isGettingLocation) "定位中..." else "当前定位",
                    enabled = !isGettingLocation,
                    onClick = {
                        isGettingLocation = true
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (fineGranted || coarseGranted) {
                            getCurrentLocation(context) { locationName, lat, lng ->
                                onLocationSelected(locationName, lat, lng)
                                isGettingLocation = false
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                LocationActionButton(
                    icon = Icons.Default.EditLocation,
                    label = "手动输入",
                    onClick = { showManualInput = !showManualInput },
                    modifier = Modifier.weight(1f)
                )

                LocationActionButton(
                    icon = Icons.Default.Map,
                    label = "地图选点",
                    onClick = { showMapPicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Recent locations
            if (recentLocations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最近使用",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentLocations.forEach { loc ->
                        val isLongLocation = loc.location.length > 18
                        Box(
                            modifier = Modifier
                                .then(if (isLongLocation) Modifier.fillMaxWidth() else Modifier)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    onLocationSelected(loc.location, loc.latitude, loc.longitude)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = if (isLongLocation) Modifier.fillMaxWidth() else Modifier,
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp)
                                )
                                Text(
                                    text = loc.location,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = if (isLongLocation) Modifier.weight(1f) else Modifier,
                                    maxLines = if (isLongLocation) 3 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            if (showManualInput) {
                Spacer(modifier = Modifier.height(8.dp))
                ManualInputField(
                    onConfirm = { name ->
                        onLocationSelected(name, null, null)
                        showManualInput = false
                    }
                )
            }
        }

        // Map picker dialog
        if (showMapPicker) {
            val mainHandler = Handler(Looper.getMainLooper())
            var mapWebView by remember { mutableStateOf<WebView?>(null) }
            DisposableEffect(Unit) {
                onDispose {
                    mapWebView?.apply {
                        stopLoading()
                        destroy()
                    }
                }
            }
            AlertDialog(
                onDismissRequest = { showMapPicker = false },
                title = { Text("地图选点") },
                text = {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        mapWebView = this
                                        webViewClient = WebViewClient()
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.allowContentAccess = true
                                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        addJavascriptInterface(object {
                                            @JavascriptInterface
                                            fun onLocationPicked(lat: Double, lng: Double, name: String) {
                                                mainHandler.post {
                                                    onLocationSelected(name, lat, lng)
                                                    showMapPicker = false
                                                }
                                            }
                                        }, "MapBridge")
                                        loadUrl("file:///android_asset/map_picker.html")
                                        if (latitude != null && longitude != null) {
                                            postDelayed({
                                                evaluateJavascript("setInitialLocation($latitude, $longitude)", null)
                                            }, 1500)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // System map option
                        TextButton(
                            onClick = {
                                val lat = latitude ?: 31.23
                                val lng = longitude ?: 121.47
                                try {
                                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val uri = Uri.parse("https://www.google.com/maps?q=$lat,$lng")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("打开系统地图查看", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMapPicker = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun ManualInputField(
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("输入地点名称", fontSize = 14.sp) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onConfirm(text.trim())
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "确认",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LocationActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getCurrentLocation(
    context: Context,
    onResult: (String?, Double?, Double?) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                val placeName = addr.featureName
                                    ?: addr.subLocality
                                    ?: addr.locality
                                    ?: addr.subAdminArea
                                    ?: addr.adminArea
                                onResult(placeName, lat, lng)
                                return
                            }
                        } catch (_: Exception) {}
                        onResult("${lat.toString().take(7)}, ${lng.toString().take(7)}", lat, lng)
                        return
                    }
                }
            } catch (_: Exception) {}
        }
        onResult(null, null, null)
    } catch (e: Exception) {
        onResult(null, null, null)
    }
}
