// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import com.arinara.fotara.ui.theme.jigglePressClickable
import com.arinara.fotara.ui.theme.jiggleShakeClickable

sealed interface AppListItemAction {
    object Chevron : AppListItemAction
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : AppListItemAction
    data class ShakingTrash(val onDelete: () -> Unit) : AppListItemAction
    data class Custom(val content: @Composable () -> Unit) : AppListItemAction
}

@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = ElectricSlateTokens.PrimaryAccent,
    leadingBoxBackground: Color = ElectricSlateTokens.SurfaceElevated,
    action: AppListItemAction? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.jigglePressClickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(leadingBoxBackground)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column {
                Text(
                    text = title,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }
            }
        }

        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            when (action) {
                is AppListItemAction.Chevron -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is AppListItemAction.Toggle -> {
                    BouncySwitch(
                        checked = action.checked,
                        onCheckedChange = action.onCheckedChange
                    )
                }
                is AppListItemAction.ShakingTrash -> {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.Destructive.copy(alpha = 0.15f))
                            .jiggleShakeClickable(onClick = action.onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ElectricSlateTokens.Destructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                is AppListItemAction.Custom -> {
                    action.content()
                }
            }
        }
    }
}

@Composable
fun BouncySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
            uncheckedThumbColor = ElectricSlateTokens.TextMuted,
            uncheckedTrackColor = ElectricSlateTokens.TrackInactive,
            uncheckedBorderColor = Color.Transparent
        )
    )
}
