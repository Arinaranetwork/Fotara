// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import com.arinara.fotara.ui.theme.jigglePressClickable

@Composable
fun AppFloatingBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    containerColor: Color = ElectricSlateTokens.SurfaceElevated,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    content: @Composable RowScope.() -> Unit
) {
    val offsetYAnim = remember { Animatable(100f) }

    LaunchedEffect(visible) {
        if (visible) {
            offsetYAnim.animateTo(
                targetValue = 0f,
                animationSpec = SpringPhysics.ViscousGooeySpec
            )
        } else {
            offsetYAnim.animateTo(
                targetValue = 120f,
                animationSpec = SpringPhysics.DismissFadeSpec
            )
        }
    }

    if (offsetYAnim.value < 119f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = offsetYAnim.value
                    }
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(BorderStroke(1.dp, borderColor), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppFloatingBarItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ElectricSlateTokens.PrimaryAccent,
    inactiveColor: Color = ElectricSlateTokens.TextMuted
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) activeColor else Color.Transparent,
                shape = CircleShape
            )
            .jigglePressClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
