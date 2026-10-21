// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tunable constants for the physics-driven 3-dot overflow menu animation.
 */
object AppDropdownMenuDefaults {
    /** Target opening animation duration in milliseconds (~250-350ms). */
    const val EnterDurationMs: Int = 300

    /** Fast reverse exit animation duration in milliseconds (~150-200ms). */
    const val ExitDurationMs: Int = 180

    /** Spring damping ratio: moderate damping producing a gentle overshoot (~1.05) and settling at 1.0. */
    const val SpringDampingRatio: Float = 0.68f

    /** Spring stiffness for responsive, organic expansion. */
    const val SpringStiffness: Float = 380f

    /** Default transform origin: top-end corner aligned with the 3-dot trigger icon. */
    val DefaultTransformOrigin: TransformOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0f)

    /** Standard corner radius conforming to AR-004 (12dp Row / Panel token). */
    val MenuCornerRadius: Dp = 12.dp

    /** Standard border definition conforming to AR-008 (1dp BorderSubtle). */
    val MenuBorderWidth: Dp = 1.dp
}

/**
 * Checks whether system-wide animations are disabled ("Remove animations" accessibility setting).
 */
fun isAnimationDisabled(context: Context): Boolean {
    return try {
        val durationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        durationScale == 0f
    } catch (_: Exception) {
        false
    }
}

/**
 * Standardized dropdown menu preset with spring overshoot expansion anchored to the 3-dot trigger icon,
 * rapid reverse exit without jiggle, and full support for system accessibility settings.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    transformOrigin: TransformOrigin = AppDropdownMenuDefaults.DefaultTransformOrigin,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val animationsDisabled = remember(context) { isAnimationDisabled(context) }

    var isPopupOpen by remember { mutableStateOf(expanded) }
    val scaleAnim = remember { Animatable(if (expanded) 1f else 0f) }
    val alphaAnim = remember { Animatable(if (expanded) 1f else 0f) }

    LaunchedEffect(expanded, animationsDisabled) {
        if (animationsDisabled) {
            isPopupOpen = expanded
            scaleAnim.snapTo(if (expanded) 1f else 0f)
            alphaAnim.snapTo(if (expanded) 1f else 0f)
            return@LaunchedEffect
        }

        if (expanded) {
            isPopupOpen = true
            launch {
                scaleAnim.snapTo(0f)
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = AppDropdownMenuDefaults.SpringDampingRatio,
                        stiffness = AppDropdownMenuDefaults.SpringStiffness
                    )
                )
            }
            launch {
                alphaAnim.snapTo(0f)
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = AppDropdownMenuDefaults.EnterDurationMs,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        } else if (isPopupOpen) {
            launch {
                scaleAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = AppDropdownMenuDefaults.ExitDurationMs,
                        easing = FastOutLinearInEasing
                    )
                )
            }
            launch {
                alphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = AppDropdownMenuDefaults.ExitDurationMs,
                        easing = FastOutLinearInEasing
                    )
                )
            }
            delay(AppDropdownMenuDefaults.ExitDurationMs.toLong())
            isPopupOpen = false
        }
    }

    if (isPopupOpen) {
        DropdownMenu(
            expanded = isPopupOpen,
            onDismissRequest = onDismissRequest,
            offset = offset,
            scrollState = scrollState,
            properties = properties,
            modifier = modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                    this.transformOrigin = transformOrigin
                }
                .background(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(AppDropdownMenuDefaults.MenuCornerRadius)
                )
                .border(
                    width = AppDropdownMenuDefaults.MenuBorderWidth,
                    color = ElectricSlateTokens.BorderSubtle,
                    shape = RoundedCornerShape(AppDropdownMenuDefaults.MenuCornerRadius)
                ),
            content = content
        )
    }
}

/**
 * 3-dot overflow menu trigger icon button with tactile spring press feedback and rotational state animation.
 */
@Composable
fun AppOverflowMenuTrigger(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Options",
    iconVector: ImageVector = Icons.Default.MoreVert,
    iconTint: Color = ElectricSlateTokens.TextPrimary,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
    overlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val animationsDisabled = remember(context) { isAnimationDisabled(context) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = if (animationsDisabled) {
            tween(durationMillis = 0)
        } else {
            spring(
                dampingRatio = AppDropdownMenuDefaults.SpringDampingRatio,
                stiffness = AppDropdownMenuDefaults.SpringStiffness
            )
        },
        label = "overflow_rotation"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .jigglePressClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    rotationZ = rotationAngle
                }
        )

        overlayContent?.invoke(this)
    }
}

/**
 * Standardized dropdown menu item adhering to Electric Slate tokens and typography.
 */
@Composable
fun ColumnScope.AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val itemColor = if (isDestructive) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextPrimary
    val iconColor = if (isDestructive) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = itemColor,
                fontSize = 14.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingText?.let {
            {
                Text(
                    text = it,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = itemColor,
            leadingIconColor = iconColor,
            trailingIconColor = ElectricSlateTokens.TextMuted,
            disabledTextColor = ElectricSlateTokens.TextMuted.copy(alpha = 0.4f),
            disabledLeadingIconColor = ElectricSlateTokens.TextMuted.copy(alpha = 0.4f)
        )
    )
}
