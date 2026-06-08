package com.diary.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MetadataChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null,
    centerContent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = accentColor ?: MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val bgColor = if (isActive) primary.copy(alpha = 0.1f) else surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected || isActive) primary else onSurfaceVariant.copy(alpha = 0.7f)
    val borderColor = if (isSelected || isActive) primary.copy(alpha = 0.3f) else outlineVariant.copy(alpha = 0.5f)

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 34.dp)
            .clip(shape)
            .background(bgColor)
            .border(0.5.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = contentColor,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
