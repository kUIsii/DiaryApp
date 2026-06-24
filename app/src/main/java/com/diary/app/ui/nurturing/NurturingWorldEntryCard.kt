package com.diary.app.ui.nurturing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.R
import com.diary.app.ui.components.GlassCard

@Composable
fun NurturingWorldEntryCard(
    state: NurturingWorldPreviewState,
    onOpenPet: () -> Unit,
    onOpenIsland: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        enableShadow = true,
        gradientColors = listOf(
            Color(0xFF17342E),
            Color(0xFF24334A),
            Color(0xFF42506A)
        ),
        innerPadding = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "养成世界",
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp,
                        color = Color(0xFFD8C7A0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.headline,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4EFE4)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "陪伴会留下回声，探索会让夜晚慢慢发光。",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFE4DDD1).copy(alpha = 0.88f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x33FFF2C6), Color(0x0DFFF2C6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = Color(0xFFF6E6BF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x19FFF8EE))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nurturing_world_entry),
                    contentDescription = "养成世界预览场景",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "今夜速览",
                    fontSize = 12.sp,
                    color = Color(0xFFD8C7A0)
                )
                Text(
                    text = state.petSnippet,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFFF7F2E8),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PreviewChip(
                    modifier = Modifier.weight(1f),
                    title = "陪伴精灵",
                    snippet = state.petSnippet,
                    icon = Icons.Default.Pets,
                    onClick = onOpenPet
                )
                PreviewChip(
                    modifier = Modifier.weight(1f),
                    title = "心情小岛",
                    snippet = state.islandSnippet,
                    icon = Icons.Default.Landscape,
                    onClick = onOpenIsland
                )
                PreviewChip(
                    modifier = Modifier.weight(1f),
                    title = "珍藏陈列",
                    snippet = state.collectionSnippet,
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenCollection
                )
            }
        }
    }
}

@Composable
private fun PreviewChip(
    title: String,
    snippet: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x14FFFDF6))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFF6E6BF),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF4EFE4)
            )
            Text(
                text = snippet,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFE4DDD1).copy(alpha = 0.86f),
                maxLines = 3
            )
        }
    }
}
