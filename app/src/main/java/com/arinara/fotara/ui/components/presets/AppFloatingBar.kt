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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp

@Composable
fun AppFloatingBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    containerColor: Color = ElectricSlateTokens.SurfaceElevated,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    applyWindowInsets: Boolean = true,
    longPressTimeoutMillis: Long = 750L,
    content: @Composable RowScope.() -> Unit
) {
    val currentViewConfig = LocalViewConfiguration.current
    val customViewConfig = remember(currentViewConfig, longPressTimeoutMillis) {
        object : ViewConfiguration by currentViewConfig {
            override val longPressTimeoutMillis: Long
                get() = longPressTimeoutMillis
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides customViewConfig) {
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

    val shouldRender by remember { derivedStateOf { offsetYAnim.value < 119f } }
    if (shouldRender) {
        val outerModifier = if (applyWindowInsets) {
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        } else {
            modifier
        }

        Box(
            modifier = outerModifier,
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    content()
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppFloatingBarItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    activeColor: Color = ElectricSlateTokens.PrimaryAccent,
    inactiveColor: Color = ElectricSlateTokens.TextSecondary,
    itemSize: Dp = 42.dp,
    iconSize: Dp = 20.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.jigglePressClickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.White else inactiveColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppFloatingBarItem(
    painter: Painter,
    contentDescription: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    activeColor: Color = ElectricSlateTokens.PrimaryAccent,
    inactiveColor: Color = ElectricSlateTokens.TextSecondary,
    itemSize: Dp = 42.dp,
    iconSize: Dp = 20.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.jigglePressClickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.White else inactiveColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppFloatingBarCustomItem(
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    activeColor: Color = ElectricSlateTokens.PrimaryAccent,
    itemSize: Dp = 42.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.jigglePressClickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
