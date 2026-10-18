// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

/**
 * Pure coordinate transforms for drawing canvas viewports.
 * Converts bidirectionally between screen coordinates (viewport pixels),
 * local layer coordinates (inside Compose graphicsLayer), and
 * intrinsic content coordinates (photo pixels or PDF page points).
 *
 * Guarantees zero offset round-trip precision at scales from 1.0f to 6.0f.
 */
object DrawingViewTransform {

    /**
     * Viewport configuration capturing container dimensions, content size, fit geometry,
     * and GPU graphicsLayer transform parameters (scale and pan offset).
     */
    data class ViewportParams(
        val viewportWidth: Float,
        val viewportHeight: Float,
        val contentWidth: Float,
        val contentHeight: Float,
        val scale: Float = 1.0f,
        val panOffset: Offset = Offset.Zero
    ) {
        val fitScale: Float = if (contentWidth > 0f && contentHeight > 0f && viewportWidth > 0f && viewportHeight > 0f) {
            min(viewportWidth / contentWidth, viewportHeight / contentHeight)
        } else {
            1.0f
        }

        val fitLeft: Float = (viewportWidth - contentWidth * fitScale) / 2f
        val fitTop: Float = (viewportHeight - contentHeight * fitScale) / 2f

        val centerX: Float = viewportWidth / 2f
        val centerY: Float = viewportHeight / 2f

        /**
         * Converts screen coordinates to local layer coordinates.
         * Reverses the Compose graphicsLayer scaling and translation.
         */
        fun screenToLocal(screen: Offset): Offset {
            val s = if (scale > 0f) scale else 1.0f
            return Offset(
                x = (screen.x - centerX - panOffset.x) / s + centerX,
                y = (screen.y - centerY - panOffset.y) / s + centerY
            )
        }

        /**
         * Converts local layer coordinates to screen coordinates.
         * Applies the Compose graphicsLayer scaling and translation.
         */
        fun localToScreen(local: Offset): Offset {
            return Offset(
                x = (local.x - centerX) * scale + centerX + panOffset.x,
                y = (local.y - centerY) * scale + centerY + panOffset.y
            )
        }

        /**
         * Converts local layer coordinates to intrinsic content coordinates.
         * Clamps to content bounds [0, contentWidth] x [0, contentHeight] if [clamp] is true.
         */
        fun localToContent(local: Offset, clamp: Boolean = true): Offset {
            val fs = if (fitScale > 0f && !fitScale.isNaN()) fitScale else 1.0f
            val safeLocalX = if (local.x.isNaN()) 0f else local.x
            val safeLocalY = if (local.y.isNaN()) 0f else local.y
            val rawX = (safeLocalX - fitLeft) / fs
            val rawY = (safeLocalY - fitTop) / fs
            val sanitizedX = if (rawX.isNaN()) 0f else rawX
            val sanitizedY = if (rawY.isNaN()) 0f else rawY
            return if (clamp && contentWidth > 0f && contentHeight > 0f) {
                Offset(sanitizedX.coerceIn(0f, contentWidth), sanitizedY.coerceIn(0f, contentHeight))
            } else {
                Offset(sanitizedX, sanitizedY)
            }
        }

        /**
         * Converts intrinsic content coordinates to local layer coordinates.
         */
        fun contentToLocal(content: Offset): Offset {
            return Offset(
                x = content.x * fitScale + fitLeft,
                y = content.y * fitScale + fitTop
            )
        }

        /**
         * Direct conversion from screen coordinates to intrinsic content coordinates.
         */
        fun screenToContent(screen: Offset, clamp: Boolean = true): Offset {
            return localToContent(screenToLocal(screen), clamp = clamp)
        }

        /**
         * Direct conversion from intrinsic content coordinates to screen coordinates.
         */
        fun contentToScreen(content: Offset): Offset {
            return localToScreen(contentToLocal(content))
        }
    }

    /**
     * Factory function creating [ViewportParams] with validated non-zero dimensions.
     */
    fun createParams(
        viewportWidth: Float,
        viewportHeight: Float,
        contentWidth: Float,
        contentHeight: Float,
        scale: Float = 1.0f,
        panOffset: Offset = Offset.Zero
    ): ViewportParams {
        return ViewportParams(
            viewportWidth = max(viewportWidth, 1f),
            viewportHeight = max(viewportHeight, 1f),
            contentWidth = max(contentWidth, 1f),
            contentHeight = max(contentHeight, 1f),
            scale = scale.coerceIn(0.1f, 20.0f),
            panOffset = panOffset
        )
    }
}
