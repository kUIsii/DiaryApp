package com.diary.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ToolbarCategory(val icon: String, val label: String)

@Composable
internal fun EditorToolbar(
    showToolbar: Boolean,
    activeCategory: Int,
    onCategoryChange: (Int) -> Unit,
    activeFormats: Map<String, Any> = emptyMap(),
    onFormat: (String) -> Unit,
    onHeading: (Int) -> Unit,
    onInsert: (String) -> Unit,
    onImageInsert: () -> Unit = {},
    onHideKeyboard: () -> Unit = {},
    onShowKeyboard: () -> Unit = {},
    onHideToolbar: () -> Unit = {},
    fontSize: Int = 14,
    onFontSizeChange: (Int) -> Unit = {}
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary

    data class Category(val icon: ImageVector, val label: String, val index: Int)
    val categories = listOf(
        Category(Icons.Default.FormatSize, "\u683c\u5f0f", 0),
        Category(Icons.Default.FormatListBulleted, "\u5217\u8868", 1),
        Category(Icons.Default.Image, "\u63d2\u5165", 2),
        Category(Icons.Default.Palette, "\u989c\u8272", 3)
    )

    if (!showToolbar) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.92f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(borderColor.copy(alpha = 0.7f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hide toolbar button
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "收起工具栏",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onHideToolbar() }
            )

            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
                    .clickable {
                        val sizes = listOf(10, 12, 14, 16, 18, 20)
                        val currentIndex = sizes.indexOf(fontSize).coerceAtLeast(0)
                        if (currentIndex > 0) onFontSizeChange(sizes[currentIndex - 1])
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A-",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Text(
                text = "$fontSize",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = activeColor,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .clickable {
                        val sizes = listOf(10, 12, 14, 16, 18, 20)
                        val currentIndex = sizes.indexOf(fontSize).coerceIn(0, sizes.lastIndex)
                        if (currentIndex < sizes.lastIndex) onFontSizeChange(sizes[currentIndex + 1])
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A+",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                CategoryButton(
                    icon = category.icon,
                    label = category.label,
                    isActive = activeCategory == category.index,
                    onClick = {
                        if (activeCategory == category.index) {
                            onCategoryChange(-1)
                            onShowKeyboard()
                        } else {
                            onCategoryChange(category.index)
                            onHideKeyboard()
                        }
                    },
                    textColor = textColor,
                    activeColor = activeColor
                )
            }
        }

        AnimatedVisibility(
            visible = activeCategory >= 0,
            enter = expandVertically(tween(200)) + fadeIn(),
            exit = shrinkVertically(tween(150)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor.copy(alpha = 0.95f))
                    .animateContentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(borderColor)
                )

                when (activeCategory) {
                    0 -> FormatSubPanel(
                        onFormat = onFormat,
                        onHeading = onHeading,
                        textColor = textColor,
                        activeColor = activeColor,
                        activeFormats = activeFormats
                    )

                    1 -> ListSubPanel(
                        onFormat = onFormat,
                        textColor = textColor,
                        activeColor = activeColor,
                        activeFormats = activeFormats
                    )

                    2 -> InsertSubPanel(
                        onFormat = onFormat,
                        onInsert = onInsert,
                        onImageInsert = onImageInsert,
                        textColor = textColor
                    )

                    3 -> ColorSubPanel(
                        onFormat = onFormat,
                        textColor = textColor,
                        activeColor = activeColor,
                        activeFormats = activeFormats
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    activeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) activeColor else textColor
        )
    }
}

@Composable
private fun FormatSubPanel(
    onFormat: (String) -> Unit,
    onHeading: (Int) -> Unit,
    textColor: Color,
    activeColor: Color,
    activeFormats: Map<String, Any> = emptyMap()
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val selectedBg = activeColor.copy(alpha = 0.15f)
    val currentHeader = activeFormats["header"]?.toString()?.toIntOrNull() ?: 0
    val isBold = activeFormats["bold"] == true
    val isItalic = activeFormats["italic"] == true
    val isUnderline = activeFormats["underline"] == true
    val isStrike = activeFormats["strike"] == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(1, "\u4e00\u7ea7\u6807\u9898", 22.sp),
                Triple(2, "\u4e8c\u7ea7\u6807\u9898", 18.sp),
                Triple(3, "\u4e09\u7ea7\u6807\u9898", 16.sp)
            ).forEach { (level, desc, fontSize) ->
                val isActive = currentHeader == level
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "h$level"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) selectedBg else btnBg)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onHeading(level)
                        }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = desc,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) activeColor else textColor
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatToggleButton(
                label = "B",
                description = "\u52a0\u7c97",
                isActive = isBold,
                textStyle = TextStyle(fontWeight = FontWeight.Bold),
                onClick = { onFormat("toggleBold()") },
                textColor = textColor,
                selectedBg = selectedBg,
                normalBg = btnBg,
                modifier = Modifier.weight(1f)
            )
            FormatToggleButton(
                label = "I",
                description = "\u659c\u4f53",
                isActive = isItalic,
                textStyle = TextStyle(fontStyle = FontStyle.Italic),
                onClick = { onFormat("toggleItalic()") },
                textColor = textColor,
                selectedBg = selectedBg,
                normalBg = btnBg,
                modifier = Modifier.weight(1f)
            )
            FormatToggleButton(
                label = "U",
                description = "\u4e0b\u5212\u7ebf",
                isActive = isUnderline,
                textStyle = TextStyle(textDecoration = TextDecoration.Underline),
                onClick = { onFormat("toggleUnderline()") },
                textColor = textColor,
                selectedBg = selectedBg,
                normalBg = btnBg,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatToggleButton(
                label = "S",
                description = "\u5220\u9664\u7ebf",
                isActive = isStrike,
                textStyle = TextStyle(textDecoration = TextDecoration.LineThrough),
                onClick = { onFormat("toggleStrike()") },
                textColor = textColor,
                selectedBg = selectedBg,
                normalBg = btnBg,
                modifier = Modifier.weight(1f)
            )
            SubFunctionButton(
                label = "\u6e05\u9664",
                icon = Icons.Default.FormatClear,
                description = "\u6e05\u9664\u683c\u5f0f",
                onClick = { onFormat("clearFormatting()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FormatToggleButton(
    label: String,
    description: String,
    isActive: Boolean,
    textStyle: TextStyle,
    onClick: () -> Unit,
    textColor: Color,
    selectedBg: Color,
    normalBg: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "fmt_$label"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) selectedBg else normalBg)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = if (isActive) primaryColor else textColor,
                style = textStyle,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                fontSize = 9.sp,
                color = (if (isActive) primaryColor else textColor).copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ListSubPanel(
    onFormat: (String) -> Unit,
    textColor: Color,
    activeColor: Color,
    activeFormats: Map<String, Any> = emptyMap()
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val currentList = activeFormats["list"]?.toString()
    val isBlockquote = activeFormats["blockquote"] == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(
                label = "\u65e0\u5e8f\u5217\u8868",
                icon = Icons.Default.FormatListBulleted,
                onClick = { onFormat("setBulletList()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f),
                isActive = currentList == "bullet",
                activeColor = activeColor
            )
            SubFunctionButton(
                label = "\u6709\u5e8f\u5217\u8868",
                icon = Icons.Default.FormatListNumbered,
                onClick = { onFormat("setOrderedList()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f),
                isActive = currentList == "ordered",
                activeColor = activeColor
            )
            SubFunctionButton(
                label = "\u590d\u9009\u6846",
                icon = Icons.Default.CheckBox,
                onClick = { onFormat("toggleCheckbox()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f),
                isActive = currentList == "checked" || currentList == "unchecked",
                activeColor = activeColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(
                label = "\u5f15\u7528",
                icon = Icons.Default.FormatQuote,
                onClick = { onFormat("toggleBlockquote()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f),
                isActive = isBlockquote,
                activeColor = activeColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsertSubPanel(
    onFormat: (String) -> Unit,
    onInsert: (String) -> Unit,
    onImageInsert: () -> Unit,
    textColor: Color
) {
    val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubFunctionButton(
                label = "\u56fe\u7247",
                icon = Icons.Default.Image,
                onClick = onImageInsert,
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f)
            )
            SubFunctionButton(
                label = "\u5206\u5272\u7ebf",
                icon = Icons.Default.HorizontalRule,
                onClick = { onInsert("divider") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f)
            )
            SubFunctionButton(
                label = "\u94fe\u63a5",
                icon = Icons.Default.Link,
                onClick = { onFormat("insertLink()") },
                textColor = textColor,
                bg = btnBg,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColorSubPanel(
    onFormat: (String) -> Unit,
    textColor: Color,
    activeColor: Color,
    activeFormats: Map<String, Any> = emptyMap()
) {
    var isTextColorMode by remember { mutableStateOf(true) }
    val textColors = listOf(
        0xFFE74C3C,
        0xFFE67E22,
        0xFFF1C40F,
        0xFF2ECC71,
        0xFF3498DB,
        0xFF9B59B6,
        0xFF1A1A1A,
        0xFFFFFFFF
    )
    val bgColors = listOf(
        0xFFFFF9C4,
        0xFFFFE0B2,
        0xFFC8E6C9,
        0xFFBBDEFB,
        0xFFD1C4E9,
        0xFFF8BBD0,
        0xFFB3E5FC,
        0xFFFFF3E0
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val btnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTextColorMode) activeColor.copy(alpha = 0.15f) else btnBg)
                    .border(
                        width = if (isTextColorMode) 1.dp else 0.dp,
                        color = if (isTextColorMode) activeColor.copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { isTextColorMode = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u5b57\u4f53\u989c\u8272",
                    fontSize = 13.sp,
                    fontWeight = if (isTextColorMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isTextColorMode) activeColor else textColor
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isTextColorMode) activeColor.copy(alpha = 0.15f) else btnBg)
                    .border(
                        width = if (!isTextColorMode) 1.dp else 0.dp,
                        color = if (!isTextColorMode) activeColor.copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { isTextColorMode = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u80cc\u666f\u989c\u8272",
                    fontSize = 13.sp,
                    fontWeight = if (!isTextColorMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isTextColorMode) activeColor else textColor
                )
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100))
        ) {
            val colors = if (isTextColorMode) textColors else bgColors
            val command = if (isTextColorMode) "setTextColor" else "setBackgroundColor"
            val activeColorKey = if (isTextColorMode) "color" else "background"
            val currentColorHex = normalizeEditorColor(activeFormats[activeColorKey]?.toString())

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { color ->
                    val hex = normalizeEditorColor("#${Integer.toHexString(color.toInt()).substring(2)}")
                    val isSelected = currentColorHex == hex

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(color))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) activeColor else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isSelected) {
                                    val clearCmd = if (isTextColorMode) {
                                        "setTextColor(false)"
                                    } else {
                                        "setBackgroundColor(false)"
                                    }
                                    onFormat(clearCmd)
                                } else {
                                    onFormat("$command('#${Integer.toHexString(color.toInt()).substring(2)}')")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (
                                    color == 0xFFFFFFFFL ||
                                    color == 0xFFFFF9C4L ||
                                    color == 0xFFFFE0B2L ||
                                    color == 0xFFF8BBD0L ||
                                    color == 0xFFFFF3E0L ||
                                    color == 0xFFB3E5FCL ||
                                    color == 0xFFBBDEFBL ||
                                    color == 0xFFC8E6C9L
                                ) {
                                    Color(0xFF37474F)
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onFormat("clearFormatting()") }) {
                Icon(
                    imageVector = Icons.Default.FormatClear,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "\u6e05\u9664\u683c\u5f0f",
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun SubFunctionButton(
    label: String,
    icon: ImageVector? = null,
    description: String = "",
    onClick: () -> Unit,
    textColor: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "subFuncScale"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.15f) else bg)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = if (isActive) activeColor else textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (isActive) activeColor else textColor,
                    style = textStyle,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    fontSize = 9.sp,
                    color = (if (isActive) activeColor else textColor).copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GridItem(
    label: String,
    description: String,
    onClick: () -> Unit,
    textColor: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "gridItemScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                style = textStyle
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f)
            )
        }
    }
}
