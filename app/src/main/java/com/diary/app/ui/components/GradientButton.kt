package com.diary.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart

enum class ButtonSize(
    val verticalPadding: Dp,
    val horizontalPadding: Dp,
    val fontSize: TextUnit,
    val cornerRadius: Dp,
    val iconSize: Dp
) {
    SMALL(10.dp, 12.dp, 12.sp, 10.dp, 16.dp),
    MEDIUM(14.dp, 16.dp, 14.sp, 14.dp, 18.dp),
    LARGE(18.dp, 20.dp, 16.sp, 16.dp, 20.dp)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(DarkAccentStart, DarkAccentEnd),
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "button_scale"
    )

    // Capture in local vals to avoid DrawScope.size shadowing inside drawBehind
    val btnCornerRadius = size.cornerRadius
    val btnHorizontalPadding = size.horizontalPadding
    val btnVerticalPadding = size.verticalPadding
    val btnFontSize = size.fontSize
    val btnIconSize = size.iconSize

    val shape = RoundedCornerShape(btnCornerRadius)

    Box(
        modifier = modifier
            .scale(pressScale)
            .drawBehind {
                if (enabled) {
                    val drawSize = this.size
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = gradientColors.map { it.copy(alpha = 0.3f) }
                        ),
                        topLeft = Offset(0f, 4.dp.toPx()),
                        size = Size(
                            drawSize.width,
                            drawSize.height + 8.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(
                            btnCornerRadius.toPx(),
                            btnCornerRadius.toPx()
                        )
                    )
                }
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = if (enabled) gradientColors
                    else gradientColors.map { it.copy(alpha = 0.4f) }
                )
            )
            .clickable(
                enabled = enabled && !isLoading,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .padding(
                horizontal = btnHorizontalPadding,
                vertical = btnVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                leadingIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
                        modifier = Modifier.size(btnIconSize)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
                    fontSize = btnFontSize,
                    fontWeight = FontWeight.Medium
                )

                trailingIcon?.let { icon ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
                        modifier = Modifier.size(btnIconSize)
                    )
                }
            }
        }
    }
}
