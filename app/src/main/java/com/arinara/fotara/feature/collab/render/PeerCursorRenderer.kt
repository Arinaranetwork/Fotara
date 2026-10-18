// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.arinara.fotara.feature.collab.model.CollabPeer

/**
 * Geometric bounds for a floating participant name tag pill.
 */
data class CursorPillRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Pure rendering helper and math evaluator for collaborative peer cursors and identification pills.
 */
object PeerCursorRenderer {

    const val DEFAULT_ACTIVE_WINDOW_MS: Long = 15_000L
    const val DEFAULT_PILL_HEIGHT: Float = 24.0f
    const val DEFAULT_CHAR_WIDTH: Float = 8.0f
    const val DEFAULT_PILL_HORIZONTAL_PADDING: Float = 16.0f
    const val POINTER_SIZE_DP: Float = 16.0f

    /**
     * Determines whether a collaborator has reported recent activity.
     */
    fun isPeerActive(
        peer: CollabPeer,
        nowMs: Long = System.currentTimeMillis(),
        activeWindowMs: Long = DEFAULT_ACTIVE_WINDOW_MS
    ): Boolean {
        return (nowMs - peer.lastActiveMs) <= activeWindowMs
    }

    /**
     * Parses a hex color string into a Compose Color, falling back to brand primary #2563EB on error.
     */
    fun parseColor(colorHex: String?, fallback: Color = Color(0xFF2563EB)): Color {
        if (colorHex.isNullOrBlank()) return fallback
        val clean = colorHex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val colorInt = clean.toLong(16) or 0xFF000000L
                    Color(colorInt)
                }
                8 -> {
                    val colorInt = clean.toLong(16)
                    Color(colorInt)
                }
                3 -> {
                    val r = clean.substring(0, 1).repeat(2)
                    val g = clean.substring(1, 2).repeat(2)
                    val b = clean.substring(2, 3).repeat(2)
                    val colorInt = "$r$g$b".toLong(16) or 0xFF000000L
                    Color(colorInt)
                }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Computes the bounding rectangle for a floating peer identification tag pill.
     * Implements edge-flipping when the cursor is near viewport boundaries to prevent screen clipping.
     */
    fun calculatePillBounds(
        cursorX: Float,
        cursorY: Float,
        tagText: String,
        charWidth: Float = DEFAULT_CHAR_WIDTH,
        pillHeight: Float = DEFAULT_PILL_HEIGHT,
        horizontalPadding: Float = DEFAULT_PILL_HORIZONTAL_PADDING,
        viewportWidth: Float = Float.MAX_VALUE,
        viewportHeight: Float = Float.MAX_VALUE,
        pointerOffset: Float = 12.0f
    ): CursorPillRect {
        val estimatedTextWidth = tagText.length * charWidth
        val pillWidth = estimatedTextWidth + horizontalPadding

        // Check if pill fits to the right, else flip to the left
        var targetLeft = cursorX + pointerOffset
        if (targetLeft + pillWidth > viewportWidth && cursorX - pointerOffset - pillWidth >= 0f) {
            targetLeft = cursorX - pointerOffset - pillWidth
        }

        // Check if pill fits below, else flip above
        var targetTop = cursorY + pointerOffset
        if (targetTop + pillHeight > viewportHeight && cursorY - pointerOffset - pillHeight >= 0f) {
            targetTop = cursorY - pointerOffset - pillHeight
        }

        // Clamp to non-negative bounds
        val finalLeft = targetLeft.coerceAtLeast(0f)
        val finalTop = targetTop.coerceAtLeast(0f)

        return CursorPillRect(
            left = finalLeft,
            top = finalTop,
            right = finalLeft + pillWidth,
            bottom = finalTop + pillHeight
        )
    }

    /**
     * Builds the directional pointer arrow path with its tip placed at the given coordinates.
     */
    fun buildPointerPath(tipX: Float, tipY: Float, scale: Float = 1.0f): Path {
        val path = Path()
        val s = if (scale > 0f) scale else 1.0f

        path.moveTo(tipX, tipY)
        path.lineTo(tipX + 14f * s, tipY + 12f * s)
        path.lineTo(tipX + 6f * s, tipY + 12f * s)
        path.lineTo(tipX + 2f * s, tipY + 18f * s)
        path.close()

        return path
    }

    /**
     * Renders active peer cursors directly onto a Compose DrawScope.
     */
    fun renderPeerCursors(
        drawScope: DrawScope,
        peers: List<CollabPeer>,
        viewportWidth: Float = Float.MAX_VALUE,
        viewportHeight: Float = Float.MAX_VALUE,
        nowMs: Long = System.currentTimeMillis(),
        activeWindowMs: Long = DEFAULT_ACTIVE_WINDOW_MS
    ) {
        val activePeers = peers.filter { isPeerActive(it, nowMs, activeWindowMs) }

        for (peer in activePeers) {
            val peerColor = parseColor(peer.colorHex)
            val pointerPath = buildPointerPath(peer.lastCursorX, peer.lastCursorY)

            // Draw dark outline for contrast against any canvas background
            drawScope.drawPath(
                path = pointerPath,
                color = Color(0xFF0A0D14),
                style = Stroke(width = 3.0f)
            )

            // Fill arrow with participant's identity color
            drawScope.drawPath(
                path = pointerPath,
                color = peerColor,
                style = Fill
            )

            // Draw pointer tip focal dot
            drawScope.drawCircle(
                color = peerColor,
                radius = 3.0f,
                center = Offset(peer.lastCursorX, peer.lastCursorY)
            )

            // Compute pill bounding box
            val pill = calculatePillBounds(
                cursorX = peer.lastCursorX,
                cursorY = peer.lastCursorY,
                tagText = peer.userTag,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight
            )

            // Draw pill background card (#111726)
            drawScope.drawRoundRect(
                color = Color(0xFF111726),
                topLeft = Offset(pill.left, pill.top),
                size = Size(pill.width, pill.height),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw pill 1dp colored border halo
            drawScope.drawRoundRect(
                color = peerColor,
                topLeft = Offset(pill.left, pill.top),
                size = Size(pill.width, pill.height),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
