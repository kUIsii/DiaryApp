package com.diary.app.ui.nurturing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard

@Composable
fun NurturingJourneyCard(
    state: NurturingJourneyState,
    onOpenPet: () -> Unit,
    onOpenIsland: () -> Unit,
    onOpenAchievement: () -> Unit,
    title: String = "今晚路线",
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = state.headline,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = state.summary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.steps.forEachIndexed { index, step ->
                    JourneyStepRow(
                        index = index + 1,
                        step = step,
                        onClick = when (step.target) {
                            NurturingRouteTarget.PET -> onOpenPet
                            NurturingRouteTarget.ISLAND -> onOpenIsland
                            NurturingRouteTarget.ACHIEVEMENT -> onOpenAchievement
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyStepRow(
    index: Int,
    step: NurturingJourneyStep,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                fontSize = 11.sp,
                color = Color(0xFFF6E6BF),
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFDF6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.target.icon(),
                contentDescription = null,
                tint = Color(0xFFF6E6BF),
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = step.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = step.detail,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
            )
        }
    }
}

private fun NurturingRouteTarget.icon(): ImageVector = when (this) {
    NurturingRouteTarget.PET -> Icons.Default.Pets
    NurturingRouteTarget.ISLAND -> Icons.Default.Landscape
    NurturingRouteTarget.ACHIEVEMENT -> Icons.Default.AutoAwesome
}
