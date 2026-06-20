package com.diary.app.ui.memory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A ticket-stub shaped composable that displays a photo with date, location, and mood accent.
 * Uses Canvas to draw the characteristic ticket shape with perforations and torn edge.
 */
@Composable
fun TicketStubCard(
    imageUri: String,
    date: Long,
    location: String?,
    moodColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val yearText = remember(date) {
        Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy"))
    }
    val monthDayText = remember(date) {
        Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM.dd"))
    }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
    ) {
        // Canvas draws the ticket shape background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTicketShape(
                size = size,
                bgColor = surfaceVariant,
                accentColor = moodColor,
                perforationColor = moodColor.copy(alpha = 0.3f)
            )
        }

        // Content layered on top of the canvas
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Left: Photo area
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Perforation divider (vertical dots)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotRadius = 3.dp.toPx()
                    val gap = 10.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(
                            color = moodColor.copy(alpha = 0.25f),
                            radius = dotRadius,
                            center = Offset(size.width / 2, y)
                        )
                        y += gap
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Info area
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Year
                Text(
                    text = yearText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Month.Day (large)
                Text(
                    text = monthDayText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Mood accent bar
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(moodColor)
                )
                Spacer(modifier = Modifier.weight(1f))
                // Location
                if (!location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = moodColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location,
                            fontSize = 11.sp,
                            color = onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // "DIARY" label
                Text(
                    text = "DIARY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant.copy(alpha = 0.35f),
                    letterSpacing = 3.sp
                )
            }
        }
    }
}

/**
 * Draws the ticket shape: rounded rect background, scalloped perforation line, torn edge on right.
 */
private fun DrawScope.drawTicketShape(
    size: Size,
    bgColor: Color,
    accentColor: Color,
    perforationColor: Color
) {
    val cornerRadius = 16.dp.toPx()
    val scallopRadius = 6.dp.toPx()
    val tornEdgeWidth = 12.dp.toPx()
    val perforationX = size.width * 0.6f

    // Main rounded rect background
    drawRoundRect(
        color = bgColor,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        size = size
    )

    // Subtle accent border
    drawRoundRect(
        color = accentColor.copy(alpha = 0.12f),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        size = size,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Scalloped perforation line (semicircles cut out along the vertical divider)
    val scallopPath = Path()
    var y = scallopRadius
    while (y < size.height - scallopRadius) {
        // Left semicircle cutout
        scallopPath.arcTo(
            rect = Rect(
                left = perforationX - scallopRadius,
                top = y - scallopRadius,
                right = perforationX + scallopRadius,
                bottom = y + scallopRadius
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        y += scallopRadius * 2.5f
    }
    drawPath(
        path = scallopPath,
        color = perforationColor,
        style = Stroke(width = 1.dp.toPx())
    )

    // Torn edge effect on the right side
    val tornPath = Path()
    tornPath.moveTo(size.width - tornEdgeWidth, 0f)
    var x = size.width - tornEdgeWidth
    var ty = 0f
    val step = 8.dp.toPx()
    while (ty < size.height) {
        val offsetX = (Math.random().toFloat() - 0.5f) * 6.dp.toPx()
        tornPath.lineTo(x + offsetX, ty)
        ty += step
    }
    tornPath.lineTo(size.width, size.height)
    tornPath.lineTo(size.width, 0f)
    tornPath.close()

    // Draw torn edge as a subtle overlay
    drawPath(
        path = tornPath,
        color = accentColor.copy(alpha = 0.04f)
    )
}
