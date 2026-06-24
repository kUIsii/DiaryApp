package com.diary.app.ui.pet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.CombinationEffect
import com.diary.app.data.PetGrowthStage
import com.diary.app.data.PetHiddenStateType
import com.diary.app.data.PetState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.nurturing.PetArtKey
import com.diary.app.ui.nurturing.petArtRes

@Composable
fun PetSceneCard(
    petName: String,
    petState: PetState,
    moodCopy: String,
    growthLabel: String,
    sceneLabel: String,
    companionHint: String,
    artKey: PetArtKey,
    interactionType: InteractionType,
    interactionCounter: Int,
    appearanceLevel: Int,
    growthStage: PetGrowthStage,
    hiddenState: PetHiddenStateType?,
    activeEffects: List<CombinationEffect>,
    onTapPet: () -> Unit,
    onFeedPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 30.dp,
        enableShadow = true,
        gradientColors = listOf(
            Color(0xFF1C2D3C),
            Color(0xFF1A3A34),
            Color(0xFF2B4760)
        ),
        innerPadding = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "陪伴角落",
                        fontSize = 12.sp,
                        color = Color(0xFFDCC99A),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "$petName 的夜间栖居地",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F0E6)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x1AF7E6B9))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = growthLabel,
                        fontSize = 12.sp,
                        color = Color(0xFFF8E8BC),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x1FFFFFFF),
                                Color(0x0DF6F0E6)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sceneLabel,
                            fontSize = 12.sp,
                            color = Color(0xFFDCC99A)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = petArtRes(artKey)),
                                contentDescription = "$petName 的主视觉",
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x330C1622)
                                        )
                                    )
                                )
                        )
                        PetComposable(
                            state = petState,
                            modifier = Modifier.size(170.dp),
                            interactionType = interactionType,
                            interactionCounter = interactionCounter,
                            appearanceLevel = appearanceLevel,
                            growthStage = growthStage,
                            hiddenState = hiddenState,
                            activeEffects = activeEffects
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = companionHint,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFE4DDD1).copy(alpha = 0.82f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x18FFF7EC))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "今晚回应",
                    fontSize = 12.sp,
                    color = Color(0xFFDCC99A)
                )
                Text(
                    text = moodCopy,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFFF6F1E8),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SceneActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PanTool,
                    title = "轻触回应",
                    subtitle = "让它抬头看看你",
                    onClick = onTapPet
                )
                SceneActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Fastfood,
                    title = "投喂一下",
                    subtitle = "给这晚一点温柔补给",
                    onClick = onFeedPet
                )
                SceneActionPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TipsAndUpdates,
                    title = petState.displayName,
                    subtitle = "现在的情绪天气",
                    onClick = onTapPet
                )
            }
        }
    }
}

@Composable
private fun SceneActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        innerPadding = 14.dp,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFF5D89D),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3EFE4)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFE0D8CB).copy(alpha = 0.8f)
            )
        }
    }
}
