// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

object SpringPhysics {
    /**
     * JigglePress:
     * Damping 0.5f, Stiffness 180f.
     * Triggered on touch down: scales down to 0.90f, bounces to 1.08f on release, settles at 1.00f.
     */
    val JigglePressSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.5f,
        stiffness = 180f
    )

    /**
     * Generic spring specification for JigglePress physics on any property type (e.g. Color, Dp).
     */
    fun <T> jigglePressSpec(): SpringSpec<T> = spring(
        dampingRatio = 0.5f,
        stiffness = 180f
    )

    val JigglePressColorSpec: SpringSpec<androidx.compose.ui.graphics.Color> = spring(
        dampingRatio = 0.5f,
        stiffness = 180f
    )

    /**
     * ViscousGooey:
     * Damping 0.45f, Stiffness 100f.
     * Applied to fluid transitions, organic slider stretches, and droplet inspector attachments.
     */
    val ViscousGooeySpec: SpringSpec<Float> = spring(
        dampingRatio = 0.45f,
        stiffness = 100f
    )

    /**
     * DismissFade:
     * Damping 0.6f, Stiffness 300f.
     * Applied to exit transitions: slight scale pop up to 1.05f, then collapses to 0f with alpha fade.
     */
    val DismissFadeSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 300f
    )
}

/**
 * Modifier extension to wrap interactive components with automatic jiggle press physics.
 * Scales down to 0.90f on touch down, bounces to 1.08f on release, and settles at 1.00f.
 */
fun Modifier.jigglePressClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim = remember { Animatable(1.0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scaleAnim.animateTo(
                targetValue = 0.90f,
                animationSpec = SpringPhysics.JigglePressSpec
            )
        } else if (scaleAnim.value < 0.99f) {
            scaleAnim.animateTo(
                targetValue = 1.08f,
                animationSpec = SpringPhysics.JigglePressSpec
            )
            scaleAnim.animateTo(
                targetValue = 1.00f,
                animationSpec = SpringPhysics.JigglePressSpec
            )
        } else {
            scaleAnim.animateTo(
                targetValue = 1.00f,
                animationSpec = SpringPhysics.JigglePressSpec
            )
        }
    }

    this
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Rotation jiggle modifier designed for back buttons and directional icon actions.
 * Swings -12 deg -> +6 deg -> 0 deg on press/release.
 */
fun Modifier.jiggleRotationClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rotationAnim = remember { Animatable(0.0f) }
    val scaleAnim = remember { Animatable(1.0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scaleAnim.animateTo(0.92f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(-12f, SpringPhysics.JigglePressSpec)
        } else if (rotationAnim.value != 0f || scaleAnim.value < 0.99f) {
            rotationAnim.animateTo(6f, SpringPhysics.JigglePressSpec)
            scaleAnim.animateTo(1.06f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
            scaleAnim.animateTo(1.00f, SpringPhysics.JigglePressSpec)
        }
    }

    this
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            rotationZ = rotationAnim.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Shaking modifier designed for destructive items (e.g., delete trash icons)
 * prior to confirmation.
 */
fun Modifier.jiggleShakeClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rotationAnim = remember { Animatable(0.0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            rotationAnim.animateTo(-10f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(10f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(-6f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(6f, SpringPhysics.JigglePressSpec)
            rotationAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
        }
    }

    this
        .graphicsLayer {
            rotationZ = rotationAnim.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}
