package com.diary.app.ui.immersive

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GradientBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImmersiveReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImmersiveReaderViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val warmLightEnabled by viewModel.warmLightEnabled.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val pagerState = rememberPagerState(pageCount = { entries.size })

    val warmOverlay = Color(0xFFFFF3E0).copy(alpha = 0.3f)

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("还没有日记可以阅读", fontSize = 16.sp)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val entry = entries[page]
                        DiaryPage(
                            entry = entry,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }

                    Row {
                        IconButton(onClick = { viewModel.toggleWarmLight() }) {
                            Icon(
                                Icons.Default.LightMode,
                                contentDescription = "暖光",
                                tint = if (warmLightEnabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            val newSize = if (fontSize >= 24) 14 else fontSize + 2
                            viewModel.setFontSize(newSize)
                        }) {
                            Icon(Icons.Default.TextFields, contentDescription = "字体大小")
                        }
                    }
                }

                // Page indicator
                if (entries.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${entries.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Warm light overlay
        if (warmLightEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(warmOverlay)
                    .clickable(enabled = false) {}
            )
        }
    }
}

@Composable
private fun DiaryPage(
    entry: DiaryPreview,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
    val dateStr = dateFormat.format(Date(entry.createdAt))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 80.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Date
        Text(
            text = dateStr,
            fontSize = (fontSize - 4).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Title
        if (entry.title.isNotBlank()) {
            Text(
                text = entry.title,
                fontSize = (fontSize + 4).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Content
        Text(
            text = entry.plainText,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.8).sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Mood indicator
        entry.moodLevel?.let { mood ->
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (mood) {
                                1 -> Color(0xFFE53935)
                                2 -> Color(0xFFFF9800)
                                3 -> Color(0xFFFFC107)
                                4 -> Color(0xFF8BC34A)
                                5 -> Color(0xFF4CAF50)
                                6 -> Color(0xFF00BCD4)
                                else -> Color.Gray
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "心情 ${mood}/6",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
