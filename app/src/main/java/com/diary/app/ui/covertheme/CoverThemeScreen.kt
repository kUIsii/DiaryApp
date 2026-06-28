package com.diary.app.ui.covertheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverThemeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CoverThemeViewModel = viewModel()
) {
    val themes by viewModel.themes.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "封面主题", onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)
                ) {
                    item {
                        Text(
                            text = "预设主题",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                    }

                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                            modifier = Modifier.height(550.dp)
                        ) {
                            items(viewModel.presets) { preset ->
                                val isActive = activeTheme?.name == preset.name
                                PresetCoverCard(
                                    preset = preset,
                                    isActive = isActive,
                                    onClick = { viewModel.applyTheme(preset) }
                                )
                            }
                        }
                    }

                    if (themes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                            Text(
                                text = "已保存的主题",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        }

                        items(themes) { theme ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                innerPadding = DesignTokens.SpacingMd
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = theme.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (theme.isActive) {
                                            Text(
                                                text = "当前使用中",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row {
                                        if (theme.isActive) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteTheme(theme) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error
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
    }
}

@Composable
private fun PresetCoverCard(
    preset: PresetCover,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val accentColor = preset.accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.CornerLarge))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                .background(
                    when (preset.texturePath) {
                        "paper_warm", "sand", "clay" -> Color(0xFFF5F0E1)
                        "moss" -> Color(0xFFF6F7F4)
                        "ocean" -> Color(0xFFF2FBFC)
                        "petal" -> Color(0xFFFFF8F7)
                        "ink" -> Color(0xFFF3F5FA)
                        else -> surfaceColor
                    }
                )
                .then(
                    if (accentColor != MaterialTheme.colorScheme.surfaceVariant) {
                        Modifier.border(2.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(DesignTokens.CornerMedium))
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "当前主题",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = preset.name,
            fontSize = 12.sp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
