package com.diary.app.ui.writinghint

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.PageHeader
import com.diary.app.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingHintScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEditor: (() -> Unit)? = null,
    viewModel: WritingHintViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.SpacingLg)
        ) {
            PageHeader(
                title = "写作灵感",
                onNavigateBack = onNavigateBack,
                action = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.totalGenerated}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        IconButton(onClick = { viewModel.generateHints() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "换一批")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            TabRow(
                selectedTabIndex = state.activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.activeTab == WritingHintTab.HINTS,
                    onClick = { viewModel.setActiveTab(WritingHintTab.HINTS) },
                    text = { Text("灵感", fontSize = 14.sp) }
                )
                Tab(
                    selected = state.activeTab == WritingHintTab.SAVED,
                    onClick = { viewModel.setActiveTab(WritingHintTab.SAVED) },
                    text = { Text("收藏", fontSize = 14.sp) }
                )
                Tab(
                    selected = state.activeTab == WritingHintTab.CUSTOM,
                    onClick = { viewModel.setActiveTab(WritingHintTab.CUSTOM) },
                    text = { Text("自定义", fontSize = 14.sp) }
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            when (state.activeTab) {
                WritingHintTab.HINTS -> HintsTab(state, viewModel, onNavigateToEditor)
                WritingHintTab.SAVED -> SavedTab(state, viewModel, onNavigateToEditor)
                WritingHintTab.CUSTOM -> CustomTab(state, viewModel, onNavigateToEditor)
            }
        }

        state.refineDialogHint?.let { hint ->
            RefineDialog(state = state, hint = hint, onDismiss = { viewModel.clearRefineDialog() })
        }
    }
}

@Composable
private fun HintsTab(
    state: WritingHintState,
    viewModel: WritingHintViewModel,
    onNavigateToEditor: (() -> Unit)?
) {
    Column {
        WritingSummaryStrip(
            state = state,
            onRefresh = { viewModel.generateHints() },
            onOpenEditor = onNavigateToEditor
        )

        if (state.customPreview.isNotEmpty()) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
            FirstClassCustomRail(
                customHints = state.customPreview,
                onUse = { hint ->
                    viewModel.markCustomHintAsUsed(hint)
                    onNavigateToEditor?.invoke()
                },
                onExpand = { hint ->
                    viewModel.expandHint(WritingHint(hint.category, hint.content, hint.id))
                }
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
        CategoryChips(
            selected = state.selectedCategory,
            onSelect = { viewModel.setCategory(it) }
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        when {
            state.isLoading -> {
                ShimmerSection()
            }
            state.filteredHints.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = if (state.selectedCategory != null) "此类别暂无提示" else "暂无写作灵感",
                    subtitle = if (state.selectedCategory != null) "试试切换其他类别" else "点击刷新生成新的灵感",
                    action = {
                        TextButton(onClick = { viewModel.generateHints() }) {
                            Text("生成灵感")
                        }
                    }
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                    contentPadding = PaddingValues(vertical = DesignTokens.SpacingSm)
                ) {
                    items(state.filteredHints, key = { "${it.category}:${it.content}" }) { hint ->
                        HintCard(
                            hint = hint,
                            isFavorite = state.savedHints.any { s -> s.id == hint.id && s.isFavorite },
                            onFavorite = { viewModel.toggleFavorite(hint) },
                            onExpand = { viewModel.expandHint(hint) },
                            onUse = {
                                viewModel.markAsUsed(hint)
                                onNavigateToEditor?.invoke()
                            }
                        )
                    }
                }
            }
        }

        if (state.errorMsg != null) {
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Text(
                text = state.errorMsg,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = DesignTokens.SpacingSm)
            )
        }
    }
}

@Composable
private fun WritingSummaryStrip(
    state: WritingHintState,
    onRefresh: () -> Unit,
    onOpenEditor: (() -> Unit)?
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        innerPadding = 14.dp,
        cornerRadius = DesignTokens.CornerLarge
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.summaryHeadline,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.summarySubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.lifecycleLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新灵感")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.summaryChips.take(3).forEach { chip ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (state.quickUsePreview.isNotEmpty()) {
                Text(
                    text = "可直接使用",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.quickUsePreview.first().content,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (onOpenEditor != null) {
                    TextButton(onClick = onOpenEditor, modifier = Modifier.align(Alignment.End)) {
                        Text("去写作")
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstClassCustomRail(
    customHints: List<SavedHint>,
    onUse: (SavedHint) -> Unit,
    onExpand: (SavedHint) -> Unit
) {
    Column {
        Text(
            text = "自定义库优先展示",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingXs))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
            items(customHints, key = { it.id }) { hint ->
                Surface(
                    modifier = Modifier.width(220.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hint.category,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "使用 ${hint.usageCount} 次",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = hint.content,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onExpand(hint) }) { Text("扩展") }
                            TextButton(onClick = { onUse(hint) }) { Text("使用") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomLibraryHeader(state: WritingHintState) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.CornerLarge,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "自定义写作库",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "把你常用的写作切口沉淀下来，后面可以直接收藏、扩展、复用。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(
                        text = "${state.customHints.size} 条自定义",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
                    Text(
                        text = "${state.favoriteHints.size} 条收藏",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedTab(
    state: WritingHintState,
    viewModel: WritingHintViewModel,
    onNavigateToEditor: (() -> Unit)?
) {
    val favorites = state.favoriteHints
    val hasContent = favorites.isNotEmpty() || state.generationHistory.isNotEmpty()

    if (!hasContent) {
        EmptyState(
            icon = Icons.Default.Star,
            title = "暂无收藏",
            subtitle = "在灵感列表中点击收藏你喜欢的写作提示"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
            contentPadding = PaddingValues(vertical = DesignTokens.SpacingSm)
        ) {
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        "固定收藏",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = DesignTokens.SpacingXs)
                    )
                }
                items(favorites, key = { it.id }) { hint ->
                    SavedHintCard(
                        hint = hint,
                        onToggleFavorite = { viewModel.toggleFavoriteSaved(hint) },
                        onExpand = { viewModel.expandHint(WritingHint(hint.category, hint.content, hint.id)) },
                        onUse = {
                            viewModel.markAsUsed(WritingHint(hint.category, hint.content, hint.id))
                            onNavigateToEditor?.invoke()
                        }
                    )
                }
            }

            if (state.generationHistory.isNotEmpty()) {
                item {
                    Text(
                        "历史生成",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = DesignTokens.SpacingMd, bottom = DesignTokens.SpacingXs)
                    )
                }
                items(state.generationHistory, key = { it.timestamp }) { gen ->
                    GenerationHistoryCard(history = gen, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun GuidanceBlock(
    title: String,
    items: List<String>
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { item ->
            Text(
                text = "• $item",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GenerationHistoryCard(
    history: GenerationHistory,
    viewModel: WritingHintViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(history.timestamp)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${history.hints.size} 条提示",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                DividerLine()
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                history.hints.forEach { hint ->
                    HintRow(
                        hint = hint,
                        isFavorite = viewModel.state.value.savedHints.any { it.id == hint.id && it.isFavorite },
                        onFavorite = { viewModel.toggleFavorite(hint) }
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                }
            }
        }
    }
}

@Composable
private fun HintRow(
    hint: WritingHint,
    isFavorite: Boolean,
    onFavorite: () -> Unit
) {
    val catColor = categoryColor(hint.category)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = catColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(DesignTokens.CornerSmall)
        ) {
            Text(
                text = hint.category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = catColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        Text(
            text = hint.content,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
        IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconMedium)
            )
        }
    }
}

@Composable
private fun CustomTab(
    state: WritingHintState,
    viewModel: WritingHintViewModel,
    onNavigateToEditor: (() -> Unit)?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var hintToDelete by remember { mutableStateOf<SavedHint?>(null) }

    Column {
        CustomLibraryHeader(state = state)

        Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.SpacingSm)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(DesignTokens.IconMedium))
            Spacer(Modifier.width(DesignTokens.SpacingSm))
            Text("添加自定义提示", fontSize = 14.sp)
        }

        if (state.customHints.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Edit,
                title = "暂无自定义提示",
                subtitle = "点击上方按钮创建属于你自己的写作提示"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd),
                contentPadding = PaddingValues(vertical = DesignTokens.SpacingSm)
            ) {
                items(state.customHints.sortedWith(compareByDescending<SavedHint> { it.usageCount }.thenByDescending { it.createdAt }), key = { it.id }) { hint ->
                    CustomHintCard(
                        hint = hint,
                        onDelete = { hintToDelete = hint },
                        onExpand = { viewModel.expandHint(WritingHint(hint.category, hint.content, hint.id)) },
                        onUse = {
                            viewModel.markCustomHintAsUsed(hint)
                            onNavigateToEditor?.invoke()
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomHintDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category, content ->
                viewModel.saveCustomHint(category, content)
                showAddDialog = false
            }
        )
    }

    hintToDelete?.let { hint ->
        AlertDialog(
            onDismissRequest = { hintToDelete = null },
            title = { Text("删除提示") },
            text = { Text("确定删除此提示？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomHint(hint.id)
                    hintToDelete = null
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { hintToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm),
        contentPadding = PaddingValues(horizontal = DesignTokens.SpacingXs),
        modifier = Modifier.padding(vertical = DesignTokens.SpacingXs)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("全部", fontSize = 13.sp) }
            )
        }
        items(WritingHintState.allCategories) { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun HintCard(
    hint: WritingHint,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onExpand: () -> Unit,
    onUse: () -> Unit
) {
    val catColor = categoryColor(hint.category)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Surface(
                color = catColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(DesignTokens.CornerSmall)
            ) {
                Text(
                    text = hint.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = catColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = hint.content,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                        tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
                IconButton(onClick = onExpand, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "扩展",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
                IconButton(onClick = onUse, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "使用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedHintCard(
    hint: SavedHint,
    onToggleFavorite: () -> Unit,
    onExpand: () -> Unit,
    onUse: () -> Unit
) {
    val catColor = categoryColor(hint.category)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = catColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(DesignTokens.CornerSmall)
                ) {
                    Text(
                        text = hint.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = catColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (hint.isUsed) {
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Text(
                        "已使用",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = hint.content,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "取消收藏",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
                IconButton(onClick = onExpand, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "扩展",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
                IconButton(onClick = onUse, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "使用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomHintCard(
    hint: SavedHint,
    onDelete: () -> Unit,
    onExpand: () -> Unit,
    onUse: () -> Unit
) {
    val catColor = categoryColor(hint.category)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = catColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(DesignTokens.CornerSmall)
                ) {
                    Text(
                        text = hint.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = catColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Text(
                text = hint.content,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onExpand, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "扩展",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
                IconButton(onClick = onUse, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "使用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconMedium)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMd)) {
        repeat(3) {
            ShimmerHintCard()
        }
    }
}

@Composable
private fun ShimmerHintCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerCard")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun RefineDialog(
    state: WritingHintState,
    hint: WritingHint,
    onDismiss: () -> Unit
) {
    val catColor = categoryColor(hint.category)

    Dialog(onDismissRequest = { if (!state.isRefining) onDismiss() }) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = DesignTokens.CornerXLarge
        ) {
            Column {
                Text(
                    "扩展建议",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Surface(
                    color = catColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(DesignTokens.CornerSmall)
                ) {
                    Text(
                        text = hint.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = catColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))
                Text(
                    text = hint.content,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                DividerLine()
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                if (state.isRefining) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    state.refinedContent?.let { content ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSm)) {
                            Text(
                                text = content,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                            state.refineGuidance?.let { guidance ->
                                GuidanceBlock(title = "可追问的问题", items = guidance.questions)
                                GuidanceBlock(title = "可切入的角度", items = guidance.angles)
                                GuidanceBlock(title = "建议开写顺序", items = guidance.nextSteps)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !state.isRefining) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomHintDialog(
    onDismiss: () -> Unit,
    onSave: (category: String, content: String) -> Unit
) {
    var category by remember { mutableStateOf(WritingHintState.allCategories.first()) }
    var content by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = DesignTokens.CornerXLarge
        ) {
            Column {
                Text(
                    "添加自定义提示",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类别") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        WritingHintState.allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.SpacingSm))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("提示内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(DesignTokens.SpacingMd))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(DesignTokens.SpacingSm))
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                onSave(category, content.trim())
                            }
                        },
                        enabled = content.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryColor(category: String): Color {
    return when (category) {
        "反思" -> Color(0xFF5C6BC0)
        "感恩" -> Color(0xFFEF5350)
        "观察" -> Color(0xFF66BB6A)
        "规划" -> Color(0xFF42A5F5)
        "情绪" -> Color(0xFFAB47BC)
        "回忆" -> Color(0xFFFF7043)
        "创造" -> Color(0xFFFFA726)
        "日常" -> Color(0xFF78909C)
        "对比" -> Color(0xFF26A69A)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    )
}
