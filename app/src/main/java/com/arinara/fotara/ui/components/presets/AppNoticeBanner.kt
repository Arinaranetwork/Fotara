// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagEmerald
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import com.arinara.fotara.ui.theme.jigglePressClickable

enum class AppNoticeVariant {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

@Composable
fun AppNoticeBanner(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    variant: AppNoticeVariant = AppNoticeVariant.INFO,
    onDismiss: (() -> Unit)? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val scaleAnim = remember { Animatable(if (visible) 1f else 0f) }
    val alphaAnim = remember { Animatable(if (visible) 1f else 0f) }
    val translateYAnim = remember { Animatable(if (visible) 0f else -30f) }

    LaunchedEffect(visible) {
        if (visible) {
            translateYAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
            scaleAnim.animateTo(1.0f, SpringPhysics.JigglePressSpec)
            alphaAnim.animateTo(1.0f, SpringPhysics.JigglePressSpec)
        } else {
            scaleAnim.animateTo(1.05f, SpringPhysics.DismissFadeSpec)
            scaleAnim.animateTo(0.0f, SpringPhysics.DismissFadeSpec)
            alphaAnim.animateTo(0.0f, SpringPhysics.DismissFadeSpec)
        }
    }

    val (tintColor, icon) = when (variant) {
        AppNoticeVariant.INFO -> ElectricSlateTokens.PrimaryAccent to Icons.Default.Info
        AppNoticeVariant.WARNING -> TagAmber to Icons.Default.Warning
        AppNoticeVariant.ERROR -> ElectricSlateTokens.Destructive to Icons.Default.Warning
        AppNoticeVariant.SUCCESS -> TagEmerald to Icons.Default.CheckCircle
    }

    val shouldRender by remember { derivedStateOf { scaleAnim.value > 0.01f } }
    if (shouldRender) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = translateYAnim.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
                .clip(RoundedCornerShape(16.dp))
                .background(ElectricSlateTokens.SurfacePanel)
                .border(
                    BorderStroke(1.dp, tintColor.copy(alpha = 0.5f)),
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tintColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        if (title != null) {
                            Text(
                                text = title,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Text(
                            text = message,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )

                        if (actionText != null && onActionClick != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = actionText,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = tintColor,
                                modifier = Modifier.jigglePressClickable(onClick = onActionClick)
                            )
                        }
                    }
                }

                if (onDismiss != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .jigglePressClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
