package com.diary.app.ui.travellog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TravelLogScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: TravelLogViewModel = viewModel()
) {
    val trips by viewModel.trips.collectAsState()

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = "旅行日志",
                onNavigateBack = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "记录旅途中的每一个故事",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                if (trips.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            innerPadding = 32.dp
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Flight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("还没有带地点的日记", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("写日记时添加位置即可生成旅行日志", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                items(trips) { trip ->
                    TripCard(trip = trip)
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripGroup) {
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* navigate to trip detail */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(trip.destination, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${trip.entryCount} 篇", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(dateFormat.format(Date(trip.startDate)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
