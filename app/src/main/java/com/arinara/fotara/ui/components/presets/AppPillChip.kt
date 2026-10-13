// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun AppPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingCount: Int? = null,
    enabled: Boolean = true
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfacePanel,
        animationSpec = SpringPhysics.JigglePressColorSpec,
        label = "pillBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else ElectricSlateTokens.TextMuted,
        animationSpec = SpringPhysics.JigglePressColorSpec,
        label = "pillTextColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
        animationSpec = SpringPhysics.JigglePressColorSpec,
        label = "pillBorderColor"
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .jigglePressClickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                fontFamily = ElmsSans,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                color = contentColor
            )

            if (trailingCount != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else ElectricSlateTokens.BorderSubtle)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trailingCount.toString(),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = contentColor
                    )
                }
            }
        }
    }
}
