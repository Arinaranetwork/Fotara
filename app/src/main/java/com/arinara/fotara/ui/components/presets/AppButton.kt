// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

enum class AppButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINED;

    companion object {
        val Primary = PRIMARY
        val Secondary = SECONDARY
        val Destructive = DESTRUCTIVE
        val Outlined = OUTLINED
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    icon: ImageVector? = null,
    height: Dp = 48.dp,
    cornerRadius: Dp = 12.dp,
    horizontalPadding: Dp = 16.dp,
    fontSize: TextUnit = 14.sp
) {
    val effectiveLeadingIcon = icon ?: leadingIcon
    val containerColor = when (variant) {
        AppButtonVariant.PRIMARY -> ElectricSlateTokens.PrimaryAccent
        AppButtonVariant.SECONDARY -> ElectricSlateTokens.TrackInactive.copy(alpha = 0.50f)
        AppButtonVariant.DESTRUCTIVE -> ElectricSlateTokens.Destructive
        AppButtonVariant.OUTLINED -> ElectricSlateTokens.TrackInactive.copy(alpha = 0.25f)
    }

    val contentColor = when (variant) {
        AppButtonVariant.PRIMARY -> Color.White
        AppButtonVariant.SECONDARY -> ElectricSlateTokens.TextPrimary
        AppButtonVariant.DESTRUCTIVE -> Color.White
        AppButtonVariant.OUTLINED -> ElectricSlateTokens.TextPrimary
    }

    val borderColor = when (variant) {
        AppButtonVariant.SECONDARY -> ElectricSlateTokens.BorderSubtle.copy(alpha = 0.6f)
        AppButtonVariant.OUTLINED -> ElectricSlateTokens.BorderSubtle
        else -> Color.Transparent
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .alpha(if (enabled) 1.0f else 0.45f)
            .height(height)
            .clip(shape)
            .background(containerColor)
            .then(
                if (variant == AppButtonVariant.OUTLINED || variant == AppButtonVariant.SECONDARY) {
                    Modifier.border(BorderStroke(1.dp, borderColor), shape)
                } else Modifier
            )
            .jigglePressClickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (effectiveLeadingIcon != null) {
                val iconSize = if (height <= 36.dp) 15.dp else 18.dp
                val iconSpace = if (horizontalPadding <= 8.dp) 4.dp else 8.dp
                Icon(
                    imageVector = effectiveLeadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(iconSpace))
            }

            Text(
                text = text,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            if (trailingIcon != null) {
                val iconSize = if (height <= 36.dp) 15.dp else 18.dp
                val iconSpace = if (horizontalPadding <= 8.dp) 4.dp else 8.dp
                Spacer(modifier = Modifier.width(iconSpace))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun AppFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Action",
    enabled: Boolean = true,
    size: Dp = 56.dp,
    containerColor: Color = ElectricSlateTokens.PrimaryAccent,
    iconColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1.0f else 0.45f)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
            .jigglePressClickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
