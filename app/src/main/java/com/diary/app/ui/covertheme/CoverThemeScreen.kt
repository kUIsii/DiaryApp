package com.diary.app.ui.covertheme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.CoverTheme
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.readingcenter.ReadingSessionSnapshot
import com.diary.app.ui.readingcenter.buildReadingThemePreviewDescription
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverThemeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CoverThemeViewModel = viewModel()
) {
    val app = LocalContext.current.applicationContext as DiaryApplication
    val themes by viewModel.themes.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val aiRecommendations by viewModel.aiRecommendations.collectAsState()
    val customThemes by viewModel.customThemes.collectAsState()
    val usageCounts by viewModel.usageCounts.collectAsState()
    val defaultThemeName by viewModel.defaultThemeName.collectAsState()
    val readingSession by app.readingSessionStore.session.collectAsState()

    var showCustomEditor by remember { mutableStateOf(false) }
    var detailPreset by remember { mutableStateOf<PresetCover?>(null) }
    var detailCustom by remember { mutableStateOf<CustomCoverTheme?>(null) }
    var detailRoom by remember { mutableStateOf<CoverTheme?>(null) }

    val showDetail = detailPreset != null || detailCustom != null || detailRoom != null

    val filteredPresets = remember(selectedCategory) { viewModel.getFilteredPresets() }
    val filteredCustom = remember(selectedCategory, customThemes) { viewModel.getFilteredCustomThemes() }
    val sortedPresets = remember(filteredPresets, usageCounts) {
        filteredPresets.sortedByDescending { usageCounts[it.name] ?: 0 }
    }
    val sortedCustom = remember(filteredCustom, usageCounts) {
        filteredCustom.sortedByDescending { usageCounts[it.name] ?: 0 }
    }

    val mostUsedName = remember(usageCounts) { viewModel.getMostUsedTheme() }
    val sortedThemes = remember(themes, usageCounts) {
        if (themes.isNotEmpty()) themes.sortedByDescending { usageCounts[it.name] ?: 0 } else emptyList()
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.SpacingLg)
        ) {
            PageHeader(title = "阅读主题", onNavigateBack = onNavigateBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CoverPreviewSection(
                    activeTheme = activeTheme,
                    readingSession = readingSession,
                    defaultThemeName = defaultThemeName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .heightIn(max = 300.dp)
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                LazyColumn(
                    modifier = Modifier.weight(1f),
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
                        FilterChipRow(
                            categories = viewModel.categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.setCategory(it) }
                        )
                    }

                    if (aiRecommendations.isNotEmpty()) {
                        item {
                            AiRecommendationSection(
                                recommendations = aiRecommendations,
                                onSelect = { name ->
                                    val preset = viewModel.presets.find { it.name == name }
                                    if (preset != null) detailPreset = preset
                                }
                            )
                        }
                    }

                    item {
                        ThemeGrid(
                            sortedPresets = sortedPresets,
                            sortedCustom = sortedCustom,
                            selectedCategory = selectedCategory,
                            activeThemeName = activeTheme?.name,
                            usageCounts = usageCounts,
                            mostUsedName = mostUsedName,
                            onPresetClick = { detailPreset = it },
                            onCustomClick = { detailCustom = it },
                            onCreateClick = { showCustomEditor = true }
                        )
                    }

                    if (sortedThemes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                            Text(
                                text = "已保存的主题",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        }

                        items(sortedThemes, key = { it.id }) { theme ->
                            SavedThemeCard(
                                theme = theme,
                                usageCount = viewModel.getUsageCount(theme.name),
                                isMostUsed = theme.name == mostUsedName && viewModel.getUsageCount(theme.name) > 0,
                                defaultThemeName = defaultThemeName,
                                onClick = { detailRoom = theme },
                                onDelete = { viewModel.deleteTheme(theme) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                    }
                }
            }
        }
    }

    if (showDetail) {
        ThemeDetailSheet(
            preset = detailPreset,
            custom = detailCustom,
            room = detailRoom,
            usageCount = when {
                detailPreset != null -> viewModel.getUsageCount(detailPreset!!.name)
                detailCustom != null -> viewModel.getUsageCount(detailCustom!!.name)
                detailRoom != null -> viewModel.getUsageCount(detailRoom!!.name)
                else -> 0
            },
            isDefault = when {
                detailPreset != null -> defaultThemeName == detailPreset!!.name
                detailCustom != null -> defaultThemeName == detailCustom!!.name
                detailRoom != null -> defaultThemeName == detailRoom!!.name
                else -> false
            },
            category = when {
                detailPreset != null -> viewModel.getCategoryForPreset(detailPreset!!.name)
                else -> "自定义"
            },
            onApply = { setDefault ->
                when {
                    detailPreset != null -> {
                        viewModel.applyPresetTheme(detailPreset!!, setDefault)
                        detailPreset = null
                    }
                    detailCustom != null -> {
                        viewModel.applyCustomTheme(detailCustom!!, setDefault)
                        detailCustom = null
                    }
                    detailRoom != null -> {
                        viewModel.applyThemeFromRoom(detailRoom!!, setDefault)
                        detailRoom = null
                    }
                }
            },
            onToggleDefault = { isDefault ->
                val name = detailPreset?.name ?: detailCustom?.name ?: detailRoom?.name
                viewModel.toggleDefaultTheme(name, isDefault)
            },
            onDismiss = {
                detailPreset = null
                detailCustom = null
                detailRoom = null
            }
        )
    }

    if (showCustomEditor) {
        CustomEditorSheet(
            presetColors = presetColors,
            textureOptions = textureOptions,
            onSave = { theme ->
                viewModel.saveCustomTheme(theme)
                showCustomEditor = false
            },
            onDismiss = { showCustomEditor = false }
        )
    }
}

@Composable
private fun CoverPreviewSection(
    activeTheme: CoverTheme?,
    readingSession: ReadingSessionSnapshot,
    defaultThemeName: String?,
    modifier: Modifier = Modifier
) {
    var showMockCovers by remember { mutableStateOf(false) }

    val previewKey = activeTheme?.name ?: "default"
    val previewBg = remember(activeTheme) {
        activeTheme?.accentColor?.let { Color(it) }?.copy(alpha = 0.08f)
            ?: Color(0xFFF5F0E1)
    }
    val previewAccent = remember(activeTheme) {
        activeTheme?.accentColor?.let { Color(it) } ?: Color(0xFFD4A574)
    }
    val previewTexture = getTextureBg(activeTheme?.texturePath)

    Crossfade(
        targetState = previewKey,
        animationSpec = tween(DesignTokens.AnimationNormal)
    ) { key ->
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.CornerLarge))
            .background(previewTexture)
            .border(1.dp, previewAccent.copy(alpha = 0.2f), RoundedCornerShape(DesignTokens.CornerLarge))
            .padding(DesignTokens.SpacingLg)
    ) {
        Column {
            Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE).format(Date()),
                    fontSize = DesignTokens.FontSmall,
                    color = previewAccent.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = readingSession.title ?: "沉浸阅读预览",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = previewAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = readingSession.previewText ?: "主题切换后，这里展示的是实际阅读页的排版和正文气质，而不是抽象封面。",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = previewAccent.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(previewAccent.copy(alpha = 0.4f))
                )
            }

            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = buildReadingThemePreviewDescription(
                            themeName = activeTheme?.name ?: (defaultThemeName ?: "默认主题"),
                            isDefault = defaultThemeName == (activeTheme?.name ?: defaultThemeName)
                        ),
                        fontSize = 11.sp,
                        color = previewAccent.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showMockCovers = !showMockCovers }
                    )
                }
                if (showMockCovers) {
                    Row(
                        modifier = Modifier
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(previewAccent.copy(alpha = 0.15f))
                                    .border(0.5.dp, previewAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(previewAccent.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
        modifier = Modifier.fillMaxWidth()
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { onCategorySelected(cat) },
                label = {
                    Text(
                        text = cat,
                        fontSize = DesignTokens.FontSmall
                    )
                },
                modifier = Modifier.heightIn(min = 32.dp)
            )
        }
    }
}

@Composable
private fun AiRecommendationSection(
    recommendations: List<String>,
    onSelect: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AI 推荐",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "智能",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)) {
            items(recommendations) { name ->
                val color = getPresetAccent(name)
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(DesignTokens.CornerMedium))
                        .clickable { onSelect(name) }
                        .padding(DesignTokens.SpacingMd)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "推荐",
                                fontSize = 10.sp,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                        Text(
                            text = name,
                            fontSize = DesignTokens.FontBody,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeGrid(
    sortedPresets: List<PresetCover>,
    sortedCustom: List<CustomCoverTheme>,
    selectedCategory: String,
    activeThemeName: String?,
    usageCounts: Map<String, Int>,
    mostUsedName: String?,
    onPresetClick: (PresetCover) -> Unit,
    onCustomClick: (CustomCoverTheme) -> Unit,
    onCreateClick: () -> Unit
) {
    val gridItems = remember(sortedPresets, sortedCustom, selectedCategory) {
        buildList<Any> {
            addAll(sortedPresets)
            if (selectedCategory == "全部" || selectedCategory == "自定义") {
                addAll(sortedCustom)
            }
            add("CREATE")
        }
    }

    val gridRows = (gridItems.size + 2) / 3
    val gridHeight = ((gridRows * 110) + ((gridRows - 1).coerceAtLeast(0) * 12)).dp.coerceAtLeast(0.dp)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
        modifier = Modifier.height(gridHeight)
    ) {
        items(gridItems, key = {
            when (it) {
                is PresetCover -> "preset_${it.name}"
                is CustomCoverTheme -> "custom_${it.id}"
                else -> "create_button"
            }
        }) { item ->
            Box(modifier = Modifier.animateItemPlacement()) {
                when (item) {
                    is PresetCover -> {
                        val isActive = item.name == activeThemeName
                        val count = usageCounts[item.name] ?: 0
                        val isMost = item.name == mostUsedName && count > 0
                        PresetCoverCard(
                            preset = item,
                            isActive = isActive,
                            usageCount = count,
                            isMostUsed = isMost,
                            onClick = { onPresetClick(item) }
                        )
                    }
                    is CustomCoverTheme -> {
                        val isActive = item.isActive
                        val count = usageCounts[item.name] ?: 0
                        val isMost = item.name == mostUsedName && count > 0
                        CustomCoverCard(
                            theme = item,
                            isActive = isActive,
                            usageCount = count,
                            isMostUsed = isMost,
                            onClick = { onCustomClick(item) }
                        )
                    }
                    else -> CreateCustomButton(onClick = onCreateClick)
                }
            }
        }
    }
}

@Composable
private fun PresetCoverCard(
    preset: PresetCover,
    isActive: Boolean,
    usageCount: Int,
    isMostUsed: Boolean,
    onClick: () -> Unit
) {
    val accentColor = preset.accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant

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
                .background(getTextureBg(preset.texturePath))
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
            if (isMostUsed && !isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "最常用",
                        fontSize = 8.sp,
                        color = Color.White
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
        if (usageCount > 0) {
            Text(
                text = "已使用 ${usageCount}次",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun CustomCoverCard(
    theme: CustomCoverTheme,
    isActive: Boolean,
    usageCount: Int,
    isMostUsed: Boolean,
    onClick: () -> Unit
) {
    val bgColor = try {
        Color(("FF" + theme.bgColor.replace("#", "")).toLong(16))
    } catch (_: Exception) { Color(0xFFF5F0E1) }

    val accentColor = try {
        Color(("FF" + theme.accentColor.replace("#", "")).toLong(16))
    } catch (_: Exception) { Color(0xFFD4A574) }

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
                .background(bgColor)
                .border(2.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(DesignTokens.CornerMedium)),
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
            if (isMostUsed && !isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "最常用",
                        fontSize = 8.sp,
                        color = Color.White
                    )
                }
            }
            Text(
                text = theme.name.take(2),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = theme.name,
            fontSize = 12.sp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (usageCount > 0) {
            Text(
                text = "已使用 ${usageCount}次",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun CreateCustomButton(onClick: () -> Unit) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    RoundedCornerShape(DesignTokens.CornerMedium)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "自定义",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDetailSheet(
    preset: PresetCover?,
    custom: CustomCoverTheme?,
    room: CoverTheme?,
    usageCount: Int,
    isDefault: Boolean,
    category: String,
    onApply: (Boolean) -> Unit,
    onToggleDefault: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var setAsDefault by remember { mutableStateOf(isDefault) }

    val name = preset?.name ?: custom?.name ?: room?.name ?: ""
    val accentColor = when {
        preset != null -> preset.accentColor?.let { Color(it) } ?: Color(0xFFD4A574)
        custom != null -> try {
            Color(("FF" + custom.accentColor.replace("#", "")).toLong(16))
        } catch (_: Exception) { Color(0xFFD4A574) }
        room != null -> room.accentColor?.let { Color(it) } ?: Color(0xFFD4A574)
        else -> Color(0xFFD4A574)
    }
    val bgColor = when {
        custom != null -> try {
            Color(("FF" + custom.bgColor.replace("#", "")).toLong(16))
        } catch (_: Exception) { getTextureBg(custom.textureName) }
        else -> getTextureBg(preset?.texturePath ?: room?.texturePath)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(DesignTokens.CornerLarge))
                    .background(bgColor)
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(DesignTokens.CornerLarge))
                    .padding(DesignTokens.SpacingLg),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE).format(Date()),
                        fontSize = DesignTokens.FontSmall,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)
            ) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }
                if (usageCount > 0) {
                    Text(
                        text = "已使用 ${usageCount}次",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Button(
                onClick = {
                    onApply(setAsDefault)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("应用")
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设为默认",
                    fontSize = DesignTokens.FontBody
                )
                Switch(
                    checked = setAsDefault,
                    onCheckedChange = {
                        setAsDefault = it
                        onToggleDefault(it)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomEditorSheet(
    presetColors: List<String>,
    textureOptions: List<Pair<String, String>>,
    onSave: (CustomCoverTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var selectedTextureIndex by remember { mutableStateOf(0) }

    val previewBg = remember(selectedColorIndex) {
        try {
            Color(("FF" + presetColors[selectedColorIndex].replace("#", "")).toLong(16))
        } catch (_: Exception) { Color(0xFFF5F0E1) }
    }
    val previewAccent = remember(selectedColorIndex) {
        val c = presetColors[selectedColorIndex]
        try {
            Color(("FF" + c.replace("#", "")).toLong(16))
        } catch (_: Exception) { Color(0xFFD4A574) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "创建自定义主题",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 10) name = it },
                label = { Text("主题名称") },
                placeholder = { Text("最多10个字") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "颜色",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            val colorDots = presetColors
            val chunked = colorDots.chunked(6)
            chunked.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { _, hex ->
                        val idx = colorDots.indexOf(hex)
                        val dotColor = try {
                            Color(("FF" + hex.replace("#", "")).toLong(16))
                        } catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .then(
                                    if (selectedColorIndex == idx) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColorIndex = idx }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

            Text(
                text = "纹理",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
                modifier = Modifier.fillMaxWidth()
            ) {
                textureOptions.forEachIndexed { index, (label, _) ->
                    val isSelected = selectedTextureIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                            .background(
                                if (isSelected) previewAccent.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, previewAccent, RoundedCornerShape(DesignTokens.CornerMedium))
                                } else Modifier
                            )
                            .clickable { selectedTextureIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = DesignTokens.FontSmall,
                            color = if (isSelected) previewAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Text(
                text = "预览",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(DesignTokens.CornerMedium))
                    .background(previewBg)
                    .border(1.dp, previewAccent.copy(alpha = 0.2f), RoundedCornerShape(DesignTokens.CornerMedium))
                    .padding(DesignTokens.SpacingMd),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = if (name.isBlank()) "主题名称" else name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = previewAccent
                    )
                    Text(
                        text = SimpleDateFormat("M月d日", Locale.CHINESE).format(Date()),
                        fontSize = DesignTokens.FontSmall,
                        color = previewAccent.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingLg))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val texture = textureOptions[selectedTextureIndex].second
                        val hex = presetColors[selectedColorIndex]
                        val theme = CustomCoverTheme(
                            name = name.trim(),
                            bgColor = hex,
                            textColor = "#333333",
                            accentColor = hex,
                            textureName = texture
                        )
                        onSave(theme)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun SavedThemeCard(
    theme: CoverTheme,
    usageCount: Int,
    isMostUsed: Boolean,
    defaultThemeName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        innerPadding = DesignTokens.SpacingMd,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isMostUsed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "最常用",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (theme.name == defaultThemeName && !isMostUsed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "默认",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
                if (usageCount > 0) {
                    Text(
                        text = "已使用 ${usageCount}次",
                        fontSize = DesignTokens.FontSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                if (theme.isActive) {
                    Text(
                        text = "当前使用中",
                        fontSize = DesignTokens.FontSmall,
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
                IconButton(onClick = onDelete) {
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

private fun getPresetAccent(name: String): Color = when (name) {
    "素雅白" -> Color(0xFF888888)
    "暖纸纹" -> Color(0xFFD4A574)
    "水墨风" -> Color(0xFF4D6AA8)
    "苔藓绿" -> Color(0xFF7BA06E)
    "沙金褐" -> Color(0xFFB89860)
    "陶土棕" -> Color(0xFFB89080)
    "海潮蓝" -> Color(0xFF5A9EA0)
    "玫瑰粉" -> Color(0xFFC48880)
    "墨水蓝" -> Color(0xFF4D6AA8)
    else -> Color(0xFFD4A574)
}

private fun getTextureBg(texturePath: String?): Color = when (texturePath) {
    "paper_warm", "sand", "clay" -> Color(0xFFF5F0E1)
    "moss" -> Color(0xFFF6F7F4)
    "ocean" -> Color(0xFFF2FBFC)
    "petal" -> Color(0xFFFFF8F7)
    "ink" -> Color(0xFFF3F5FA)
    "watercolor" -> Color(0xFFDCE4F0)
    "geometric" -> Color(0xFFE8E0D8)
    else -> Color(0xFFF5F0E1)
}
