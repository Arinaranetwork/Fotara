// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.profile

import kotlin.math.max
import kotlin.math.min

enum class CropCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

data class CropRectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class SourceCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

object CropBoxGeometry {

    /**
     * Computes the largest centered crop rectangle with the given target aspect ratio
     * (width / height) fitting completely within image bounds [0, 0, imageWidth, imageHeight].
     */
    fun computeInitialBox(
        imageWidth: Float,
        imageHeight: Float,
        aspectRatio: Float
    ): CropRectF {
        if (imageWidth <= 0f || imageHeight <= 0f || aspectRatio <= 0f) {
            return CropRectF(0f, 0f, 0f, 0f)
        }
        val targetRatio = aspectRatio
        val imageRatio = imageWidth / imageHeight

        val boxWidth: Float
        val boxHeight: Float
        if (imageRatio > targetRatio) {
            // Image is wider than target box: fit to height
            boxHeight = imageHeight
            boxWidth = boxHeight * targetRatio
        } else {
            // Image is taller than or equal to target box: fit to width
            boxWidth = imageWidth
            boxHeight = boxWidth / targetRatio
        }

        val left = (imageWidth - boxWidth) / 2f
        val top = (imageHeight - boxHeight) / 2f
        return CropRectF(
            left = left,
            top = top,
            right = left + boxWidth,
            bottom = top + boxHeight
        )
    }

    /**
     * Moves the current box by (dx, dy) while strictly clamping within [0, 0, imageWidth, imageHeight].
     * The width and height are strictly preserved.
     */
    fun pan(
        current: CropRectF,
        dx: Float,
        dy: Float,
        imageWidth: Float,
        imageHeight: Float
    ): CropRectF {
        val w = current.width
        val h = current.height
        if (w >= imageWidth && h >= imageHeight) {
            return CropRectF(0f, 0f, imageWidth, imageHeight)
        }

        val maxLeft = (imageWidth - w).coerceAtLeast(0f)
        val maxTop = (imageHeight - h).coerceAtLeast(0f)

        val newLeft = (current.left + dx).coerceIn(0f, maxLeft)
        val newTop = (current.top + dy).coerceIn(0f, maxTop)

        return CropRectF(
            left = newLeft,
            top = newTop,
            right = newLeft + w,
            bottom = newTop + h
        )
    }

    /**
     * Resizes the box by dragging one of its 4 corners, keeping the opposite corner strictly fixed
     * and maintaining the target aspect ratio (width / height).
     * Clamped within image bounds [0, 0, imageWidth, imageHeight] and above minSize.
     */
    fun resizeCorner(
        current: CropRectF,
        corner: CropCorner,
        touchX: Float,
        touchY: Float,
        imageWidth: Float,
        imageHeight: Float,
        aspectRatio: Float,
        minWidth: Float = 64f,
        minHeight: Float = minWidth / aspectRatio
    ): CropRectF {
        val effectiveMinWidth = max(minWidth, minHeight * aspectRatio)
        val effectiveMinHeight = effectiveMinWidth / aspectRatio

        // Fixed opposite corner (anchor)
        val anchorX: Float
        val anchorY: Float
        when (corner) {
            CropCorner.TOP_LEFT -> {
                anchorX = current.right
                anchorY = current.bottom
            }
            CropCorner.TOP_RIGHT -> {
                anchorX = current.left
                anchorY = current.bottom
            }
            CropCorner.BOTTOM_LEFT -> {
                anchorX = current.right
                anchorY = current.top
            }
            CropCorner.BOTTOM_RIGHT -> {
                anchorX = current.left
                anchorY = current.top
            }
        }

        // Maximum available width and height from anchor in the direction of the dragged corner
        val maxAvailW: Float
        val maxAvailH: Float
        when (corner) {
            CropCorner.TOP_LEFT -> {
                maxAvailW = anchorX
                maxAvailH = anchorY
            }
            CropCorner.TOP_RIGHT -> {
                maxAvailW = imageWidth - anchorX
                maxAvailH = anchorY
            }
            CropCorner.BOTTOM_LEFT -> {
                maxAvailW = anchorX
                maxAvailH = imageHeight - anchorY
            }
            CropCorner.BOTTOM_RIGHT -> {
                maxAvailW = imageWidth - anchorX
                maxAvailH = imageHeight - anchorY
            }
        }

        // Maximum box size that fits inside available boundary while keeping aspect ratio
        val maxW = min(maxAvailW, maxAvailH * aspectRatio).coerceAtLeast(effectiveMinWidth)

        // Raw distance from anchor to touch point
        val deltaX = Math.abs(touchX - anchorX)
        val deltaY = Math.abs(touchY - anchorY)

        // Project touch onto aspect ratio vector (aspectRatio, 1)
        val proj = (deltaX * aspectRatio + deltaY) / (aspectRatio * aspectRatio + 1f)
        val candidateW = proj * aspectRatio

        val finalW = candidateW.coerceIn(effectiveMinWidth, maxW)
        val finalH = finalW / aspectRatio

        return when (corner) {
            CropCorner.TOP_LEFT -> CropRectF(
                left = anchorX - finalW,
                top = anchorY - finalH,
                right = anchorX,
                bottom = anchorY
            )
            CropCorner.TOP_RIGHT -> CropRectF(
                left = anchorX,
                top = anchorY - finalH,
                right = anchorX + finalW,
                bottom = anchorY
            )
            CropCorner.BOTTOM_LEFT -> CropRectF(
                left = anchorX - finalW,
                top = anchorY,
                right = anchorX,
                bottom = anchorY + finalH
            )
            CropCorner.BOTTOM_RIGHT -> CropRectF(
                left = anchorX,
                top = anchorY,
                right = anchorX + finalW,
                bottom = anchorY + finalH
            )
        }
    }

    /**
     * Scales the box around its center by scaleFactor, maintaining the target aspect ratio
     * and clamping within [0, 0, imageWidth, imageHeight] and above minSize.
     */
    fun pinchScale(
        current: CropRectF,
        scaleFactor: Float,
        imageWidth: Float,
        imageHeight: Float,
        aspectRatio: Float,
        minWidth: Float = 64f,
        minHeight: Float = minWidth / aspectRatio
    ): CropRectF {
        val cx = current.centerX
        val cy = current.centerY
        val effectiveMinWidth = max(minWidth, minHeight * aspectRatio)

        // Maximum width possible around this center without exceeding boundaries:
        val maxW = min(
            min(2f * cx, 2f * (imageWidth - cx)),
            min(2f * cy * aspectRatio, 2f * (imageHeight - cy) * aspectRatio)
        ).coerceAtLeast(effectiveMinWidth)

        val targetW = (current.width * scaleFactor).coerceIn(effectiveMinWidth, maxW)
        val targetH = targetW / aspectRatio

        return CropRectF(
            left = cx - targetW / 2f,
            top = cy - targetH / 2f,
            right = cx + targetW / 2f,
            bottom = cy + targetH / 2f
        )
    }

    /**
     * Maps display crop rect (in display coordinates [0, 0, displayWidth, displayHeight])
     * to the raw source image pixel rectangle [srcLeft, srcTop, srcRight, srcBottom]
     * taking into account EXIF rotation (0, 90, 180, 270 degrees).
     */
    fun mapDisplayToSourceRect(
        displayCrop: CropRectF,
        displayWidth: Float,
        displayHeight: Float,
        rawSourceWidth: Int,
        rawSourceHeight: Int,
        rotationDegrees: Int
    ): SourceCropRect {
        if (displayWidth <= 0f || displayHeight <= 0f || rawSourceWidth <= 0 || rawSourceHeight <= 0) {
            return SourceCropRect(0, 0, rawSourceWidth.coerceAtLeast(1), rawSourceHeight.coerceAtLeast(1))
        }

        val u0 = (displayCrop.left / displayWidth).coerceIn(0f, 1f)
        val u1 = (displayCrop.right / displayWidth).coerceIn(u0 + 0.0001f, 1f)
        val v0 = (displayCrop.top / displayHeight).coerceIn(0f, 1f)
        val v1 = (displayCrop.bottom / displayHeight).coerceIn(v0 + 0.0001f, 1f)

        val normRot = ((rotationDegrees % 360) + 360) % 360

        val (rawL, rawT, rawR, rawB) = when (normRot) {
            90 -> {
                val l = (v0 * rawSourceWidth).toInt().coerceIn(0, rawSourceWidth - 1)
                val r = (v1 * rawSourceWidth).toInt().coerceIn(l + 1, rawSourceWidth)
                val t = ((1f - u1) * rawSourceHeight).toInt().coerceIn(0, rawSourceHeight - 1)
                val b = ((1f - u0) * rawSourceHeight).toInt().coerceIn(t + 1, rawSourceHeight)
                listOf(l, t, r, b)
            }
            180 -> {
                val l = ((1f - u1) * rawSourceWidth).toInt().coerceIn(0, rawSourceWidth - 1)
                val r = ((1f - u0) * rawSourceWidth).toInt().coerceIn(l + 1, rawSourceWidth)
                val t = ((1f - v1) * rawSourceHeight).toInt().coerceIn(0, rawSourceHeight - 1)
                val b = ((1f - v0) * rawSourceHeight).toInt().coerceIn(t + 1, rawSourceHeight)
                listOf(l, t, r, b)
            }
            270 -> {
                val l = ((1f - v1) * rawSourceWidth).toInt().coerceIn(0, rawSourceWidth - 1)
                val r = ((1f - v0) * rawSourceWidth).toInt().coerceIn(l + 1, rawSourceWidth)
                val t = (u0 * rawSourceHeight).toInt().coerceIn(0, rawSourceHeight - 1)
                val b = (u1 * rawSourceHeight).toInt().coerceIn(t + 1, rawSourceHeight)
                listOf(l, t, r, b)
            }
            else -> { // 0
                val l = (u0 * rawSourceWidth).toInt().coerceIn(0, rawSourceWidth - 1)
                val r = (u1 * rawSourceWidth).toInt().coerceIn(l + 1, rawSourceWidth)
                val t = (v0 * rawSourceHeight).toInt().coerceIn(0, rawSourceHeight - 1)
                val b = (v1 * rawSourceHeight).toInt().coerceIn(t + 1, rawSourceHeight)
                listOf(l, t, r, b)
            }
        }

        return SourceCropRect(
            left = rawL,
            top = rawT,
            right = rawR,
            bottom = rawB
        )
    }
}
