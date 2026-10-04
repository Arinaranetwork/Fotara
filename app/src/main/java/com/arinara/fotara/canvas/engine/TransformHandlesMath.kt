// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Pure math utilities for canvas selection transform handles:
 * - 44dp hit-testing
 * - Oriented bounding box geometry
 * - Anchor-fixed stretching (corner and side)
 * - Box center rotation
 * - 24dp size clamping with anti-mirroring
 */
object TransformHandlesMath {

    const val HANDLE_TOUCH_RADIUS_DP = 22.0f // 44dp touch target
    const val MIN_SIZE_CLAMP_DP = 24.0f
    const val ROTATION_STEM_OFFSET_DP = 28.0f
    const val SUPPRESS_SIDE_HANDLES_THRESHOLD_DP = 88.0f
    const val BODY_DRAG_PADDING_DP = 16.0f

    /**
     * Data snapshot of elements before transform begins.
     */
    data class TransformStartState(
        val originalStrokes: Map<String, List<StrokePoint>>,
        val originalImageBounds: Map<String, CanvasRect>,
        val originalImageRotations: Map<String, Float>,
        val originalImages: Map<String, ImageElement> = emptyMap(),
        val worldCenter: Pair<Float, Float>,
        val worldWidth: Float,
        val worldHeight: Float,
        val rotationDegrees: Float,
        val startScreenX: Float,
        val startScreenY: Float
    )

    /**
     * Hit tests screen coordinates against oriented selection handles and body.
     * Returns:
     * - 0..7: NW, N, NE, E, SE, S, SW, W handles
     * - 8: Rotation handle (stem above top center)
     * - -1: Inside selection bounding box (body drag)
     * - null: Outside
     */
    fun hitTest(
        screenX: Float,
        screenY: Float,
        bounds: CanvasRect,
        rotationDegrees: Float,
        viewport: ViewportState,
        density: Float = 1.0f
    ): Int? {
        if (bounds.isEmpty) return null

        val (scx, scy) = ViewportTransform.worldToScreen(bounds.centerX, bounds.centerY, viewport)
        val sW = bounds.width * viewport.scale
        val sH = bounds.height * viewport.scale

        val rad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cosR = cos(rad)
        val sinR = sin(rad)

        val handleTouchRadius = HANDLE_TOUCH_RADIUS_DP * density
        val stemOffset = ROTATION_STEM_OFFSET_DP * density
        val suppressSide = sW < (SUPPRESS_SIDE_HANDLES_THRESHOLD_DP * density) ||
                sH < (SUPPRESS_SIDE_HANDLES_THRESHOLD_DP * density)

        // 1. Rotation handle (8)
        val rotLocalX = 0.0f
        val rotLocalY = -sH / 2.0f - stemOffset
        val rotScreenX = scx + rotLocalX * cosR - rotLocalY * sinR
        val rotScreenY = scy + rotLocalX * sinR + rotLocalY * cosR

        if (hypot(screenX - rotScreenX, screenY - rotScreenY) <= handleTouchRadius) {
            return 8
        }

        // 2. Resize handles (0..7)
        val halfW = sW / 2.0f
        val halfH = sH / 2.0f

        val handles = arrayOf(
            Pair(-halfW, -halfH), // 0: NW
            Pair(0.0f, -halfH),   // 1: N
            Pair(halfW, -halfH),  // 2: NE
            Pair(halfW, 0.0f),    // 3: E
            Pair(halfW, halfH),   // 4: SE
            Pair(0.0f, halfH),    // 5: S
            Pair(-halfW, halfH),  // 6: SW
            Pair(-halfW, 0.0f)    // 7: W
        )

        for (i in handles.indices) {
            if (suppressSide && (i == 1 || i == 3 || i == 5 || i == 7)) {
                continue // Suppress side-midpoint handles on compact boxes
            }
            val (lx, ly) = handles[i]
            val hScreenX = scx + lx * cosR - ly * sinR
            val hScreenY = scy + lx * sinR + ly * cosR

            if (hypot(screenX - hScreenX, screenY - hScreenY) <= handleTouchRadius) {
                return i
            }
        }

        // 3. Body hit test (inside oriented box with padding for thin/straight strokes)
        val dx = screenX - scx
        val dy = screenY - scy
        val localTouchX = dx * cos(-rad) - dy * sin(-rad)
        val localTouchY = dx * sin(-rad) + dy * cos(-rad)

        val padding = BODY_DRAG_PADDING_DP * density
        val safeHalfW = max(halfW, 8.0f * density) + padding
        val safeHalfH = max(halfH, 8.0f * density) + padding

        if (kotlin.math.abs(localTouchX) <= safeHalfW && kotlin.math.abs(localTouchY) <= safeHalfH) {
            return -1 // Body drag
        }

        return null
    }

    /**
     * Computes transformed stroke points during move, stretch, or rotation
     * from the original start points without accumulating incremental rounding errors.
     */
    fun transformStrokePoints(
        originalPoints: List<StrokePoint>,
        handleId: Int,
        startState: TransformStartState,
        currentScreenX: Float,
        currentScreenY: Float,
        viewport: ViewportState,
        density: Float = 1.0f
    ): List<StrokePoint> {
        if (originalPoints.isEmpty()) return emptyList()

        if (handleId == -1) {
            // Whole body move
            val worldDx = (currentScreenX - startState.startScreenX) / viewport.scale
            val worldDy = (currentScreenY - startState.startScreenY) / viewport.scale
            return originalPoints.map { p ->
                val nx = p.x + worldDx
                val ny = p.y + worldDy
                p.copy(
                    x = if (nx.isNaN() || nx.isInfinite()) p.x else nx,
                    y = if (ny.isNaN() || ny.isInfinite()) p.y else ny
                )
            }
        }

        val (scx, scy) = ViewportTransform.worldToScreen(startState.worldCenter.first, startState.worldCenter.second, viewport)

        if (handleId == 8) {
            // Rotation around selection centroid
            val startAngle = atan2(startState.startScreenY - scy, startState.startScreenX - scx)
            val currentAngle = atan2(currentScreenY - scy, currentScreenX - scx)
            val deltaAngle = currentAngle - startAngle

            val cosD = cos(deltaAngle)
            val sinD = sin(deltaAngle)
            val (cx, cy) = startState.worldCenter

            return originalPoints.map { p ->
                val rx = p.x - cx
                val ry = p.y - cy
                val nx = cx + rx * cosD - ry * sinD
                val ny = cy + rx * sinD + ry * cosD
                p.copy(
                    x = if (nx.isNaN() || nx.isInfinite()) p.x else nx,
                    y = if (ny.isNaN() || ny.isInfinite()) p.y else ny
                )
            }
        }

        // Resize handles (0..7): anchor stays fixed
        val sW = startState.worldWidth * viewport.scale
        val sH = startState.worldHeight * viewport.scale

        val rad = Math.toRadians(startState.rotationDegrees.toDouble()).toFloat()
        val cosNeg = cos(-rad)
        val sinNeg = sin(-rad)

        // Convert current touch to local box frame
        val curDx = currentScreenX - scx
        val curDy = currentScreenY - scy
        val curLx = curDx * cosNeg - curDy * sinNeg
        val curLy = curDx * sinNeg + curDy * cosNeg

        val halfW = sW / 2.0f
        val halfH = sH / 2.0f

        // Determine anchor in local box frame
        val (anchorX, anchorY) = when (handleId) {
            0 -> Pair(halfW, halfH)   // NW dragged -> SE anchor
            1 -> Pair(0.0f, halfH)    // N dragged -> S anchor
            2 -> Pair(-halfW, halfH)  // NE dragged -> SW anchor
            3 -> Pair(-halfW, 0.0f)   // E dragged -> W anchor
            4 -> Pair(-halfW, -halfH) // SE dragged -> NW anchor
            5 -> Pair(0.0f, -halfH)   // S dragged -> N anchor
            6 -> Pair(halfW, -halfH)  // SW dragged -> NE anchor
            7 -> Pair(halfW, 0.0f)    // W dragged -> E anchor
            else -> Pair(0.0f, 0.0f)
        }

        val origDx = when (handleId) {
            0, 6, 7 -> -halfW - anchorX
            2, 3, 4 -> halfW - anchorX
            else -> 0.0f
        }
        val origDy = when (handleId) {
            0, 1, 2 -> -halfH - anchorY
            4, 5, 6 -> halfH - anchorY
            else -> 0.0f
        }

        val minSize = MIN_SIZE_CLAMP_DP * density

        var newDx = curLx - anchorX
        if (origDx > 0.0f) {
            newDx = max(minSize, newDx)
        } else if (origDx < 0.0f) {
            newDx = kotlin.math.min(-minSize, newDx)
        }

        var newDy = curLy - anchorY
        if (origDy > 0.0f) {
            newDy = max(minSize, newDy)
        } else if (origDy < 0.0f) {
            newDy = kotlin.math.min(-minSize, newDy)
        }

        val scaleX = if (kotlin.math.abs(origDx) < 1e-3f) 1.0f else newDx / origDx
        val scaleY = if (kotlin.math.abs(origDy) < 1e-3f) 1.0f else newDy / origDy

        val (worldAnchorX, worldAnchorY) = localToWorld(anchorX / viewport.scale, anchorY / viewport.scale, startState.worldCenter, rad)

        return originalPoints.map { p ->
            val (plx, ply) = worldToLocal(p.x, p.y, startState.worldCenter, rad)
            val plxRel = plx - (anchorX / viewport.scale)
            val plyRel = ply - (anchorY / viewport.scale)

            val nlx = (anchorX / viewport.scale) + plxRel * scaleX
            val nly = (anchorY / viewport.scale) + plyRel * scaleY

            val (wx, wy) = localToWorld(nlx, nly, startState.worldCenter, rad)
            p.copy(
                x = if (wx.isNaN() || wx.isInfinite()) p.x else wx,
                y = if (wy.isNaN() || wy.isInfinite()) p.y else wy
            )
        }
    }

    /**
     * Converts world point to local box unrotated coordinate relative to box center.
     */
    fun worldToLocal(wx: Float, wy: Float, center: Pair<Float, Float>, rad: Float): Pair<Float, Float> {
        val dx = wx - center.first
        val dy = wy - center.second
        val lx = dx * cos(-rad) - dy * sin(-rad)
        val ly = dx * sin(-rad) + dy * cos(-rad)
        return Pair(lx, ly)
    }

    /**
     * Converts local box unrotated coordinate relative to box center to world point.
     */
    fun localToWorld(lx: Float, ly: Float, center: Pair<Float, Float>, rad: Float): Pair<Float, Float> {
        val wx = center.first + lx * cos(rad) - ly * sin(rad)
        val wy = center.second + lx * sin(rad) + ly * cos(rad)
        return Pair(wx, wy)
    }

    /**
     * Transforms an ImageElement during handle resize, stretch, move, or rotation.
     * When isOnlyImages is true:
     * - Corner handles (0, 2, 4, 6) scale proportionally (aspect ratio locked, opposite corner fixed).
     * - Side-midpoint handles (1, 3, 5, 7) stretch along one axis only (opposite side fixed).
     * When false (mixed selection):
     * - Retains free corner stretch.
     */
    fun transformImageElement(
        originalImage: ImageElement,
        handleId: Int,
        startState: TransformStartState,
        currentScreenX: Float,
        currentScreenY: Float,
        viewport: ViewportState,
        density: Float = 1.0f,
        isOnlyImages: Boolean = true
    ): ImageElement {
        if (handleId == -1) {
            // Whole body move
            val worldDx = (currentScreenX - startState.startScreenX) / viewport.scale
            val worldDy = (currentScreenY - startState.startScreenY) / viewport.scale
            return clampImageToCanvas(originalImage.translated(worldDx, worldDy))
        }

        val (scx, scy) = ViewportTransform.worldToScreen(startState.worldCenter.first, startState.worldCenter.second, viewport)

        if (handleId == 8) {
            // Rotation around selection centroid
            val startAngle = atan2(startState.startScreenY - scy, startState.startScreenX - scx)
            val currentAngle = atan2(currentScreenY - scy, currentScreenX - scx)
            val deltaAngle = currentAngle - startAngle
            val deltaDeg = Math.toDegrees(deltaAngle.toDouble()).toFloat()

            val cosD = cos(deltaAngle)
            val sinD = sin(deltaAngle)
            val (cx, cy) = startState.worldCenter
            val origCenterX = originalImage.x + originalImage.width / 2f
            val origCenterY = originalImage.y + originalImage.height / 2f
            val rx = origCenterX - cx
            val ry = origCenterY - cy
            val newCenterX = cx + rx * cosD - ry * sinD
            val newCenterY = cy + rx * sinD + ry * cosD

            val newX = newCenterX - originalImage.width / 2f
            val newY = newCenterY - originalImage.height / 2f
            val initRot = startState.originalImageRotations[originalImage.id] ?: originalImage.rotationDegrees
            val newRot = (initRot + deltaDeg) % 360f
            val finalRot = if (newRot < 0f) newRot + 360f else newRot

            return clampImageToCanvas(
                originalImage.copy(
                    x = newX,
                    y = newY,
                    rotationDegrees = finalRot,
                    bounds = CanvasRect(newX, newY, newX + originalImage.width, newY + originalImage.height)
                )
            )
        }

        // Resize handles (0..7)
        val sW = startState.worldWidth * viewport.scale
        val sH = startState.worldHeight * viewport.scale
        val rad = Math.toRadians(startState.rotationDegrees.toDouble()).toFloat()
        val cosNeg = cos(-rad)
        val sinNeg = sin(-rad)

        // Convert current touch to local box frame
        val curDx = currentScreenX - scx
        val curDy = currentScreenY - scy
        val curLx = curDx * cosNeg - curDy * sinNeg
        val curLy = curDx * sinNeg + curDy * cosNeg

        val halfW = sW / 2.0f
        val halfH = sH / 2.0f

        // Determine anchor in local box frame (screen units)
        val (anchorX, anchorY) = when (handleId) {
            0 -> Pair(halfW, halfH)   // NW dragged -> SE anchor fixed
            1 -> Pair(0.0f, halfH)    // N dragged -> S anchor fixed
            2 -> Pair(-halfW, halfH)  // NE dragged -> SW anchor fixed
            3 -> Pair(-halfW, 0.0f)   // E dragged -> W anchor fixed
            4 -> Pair(-halfW, -halfH) // SE dragged -> NW anchor fixed
            5 -> Pair(0.0f, -halfH)   // S dragged -> N anchor fixed
            6 -> Pair(halfW, -halfH)  // SW dragged -> NE anchor fixed
            7 -> Pair(halfW, 0.0f)    // W dragged -> E anchor fixed
            else -> Pair(0.0f, 0.0f)
        }

        val origDx = when (handleId) {
            0, 6, 7 -> -halfW - anchorX
            2, 3, 4 -> halfW - anchorX
            else -> 0.0f
        }
        val origDy = when (handleId) {
            0, 1, 2 -> -halfH - anchorY
            4, 5, 6 -> halfH - anchorY
            else -> 0.0f
        }

        val minSize = MIN_SIZE_CLAMP_DP * density
        val isCorner = handleId == 0 || handleId == 2 || handleId == 4 || handleId == 6
        val isSide = handleId == 1 || handleId == 3 || handleId == 5 || handleId == 7

        val scaleX: Float
        val scaleY: Float

        if (isOnlyImages && isCorner) {
            // Proportional scaling preserving aspect ratio, opposite corner fixed
            val vX = curLx - anchorX
            val vY = curLy - anchorY
            val dot = vX * origDx + vY * origDy
            val origLenSq = origDx * origDx + origDy * origDy
            val rawScale = if (origLenSq > 1e-4f) dot / origLenSq else 1.0f

            val minScaleX = if (kotlin.math.abs(origDx) > 1e-4f) minSize / kotlin.math.abs(origDx) else 0.05f
            val minScaleY = if (kotlin.math.abs(origDy) > 1e-4f) minSize / kotlin.math.abs(origDy) else 0.05f
            val minScale = max(minScaleX, minScaleY)

            val finalScale = max(rawScale, minScale)
            scaleX = finalScale
            scaleY = finalScale
        } else if (isOnlyImages && isSide) {
            // Single-axis stretch, opposite side fixed
            if (handleId == 3 || handleId == 7) {
                // Horizontal stretch (E / W)
                var newDx = curLx - anchorX
                if (origDx > 0.0f) {
                    newDx = max(minSize, newDx)
                } else if (origDx < 0.0f) {
                    newDx = kotlin.math.min(-minSize, newDx)
                }
                scaleX = if (kotlin.math.abs(origDx) < 1e-3f) 1.0f else newDx / origDx
                scaleY = 1.0f // Height locked
            } else {
                // Vertical stretch (N / S)
                var newDy = curLy - anchorY
                if (origDy > 0.0f) {
                    newDy = max(minSize, newDy)
                } else if (origDy < 0.0f) {
                    newDy = kotlin.math.min(-minSize, newDy)
                }
                scaleX = 1.0f // Width locked
                scaleY = if (kotlin.math.abs(origDy) < 1e-3f) 1.0f else newDy / origDy
            }
        } else {
            // Free stretch (mixed / stroke behavior)
            var newDx = curLx - anchorX
            if (origDx > 0.0f) {
                newDx = max(minSize, newDx)
            } else if (origDx < 0.0f) {
                newDx = kotlin.math.min(-minSize, newDx)
            }

            var newDy = curLy - anchorY
            if (origDy > 0.0f) {
                newDy = max(minSize, newDy)
            } else if (origDy < 0.0f) {
                newDy = kotlin.math.min(-minSize, newDy)
            }

            scaleX = if (kotlin.math.abs(origDx) < 1e-3f) 1.0f else newDx / origDx
            scaleY = if (kotlin.math.abs(origDy) < 1e-3f) 1.0f else newDy / origDy
        }

        // Apply scaleX and scaleY relative to anchor point
        val anchorWorldX = anchorX / viewport.scale
        val anchorWorldY = anchorY / viewport.scale

        val origImgCenterX = originalImage.x + originalImage.width / 2f
        val origImgCenterY = originalImage.y + originalImage.height / 2f
        val (imgLx, imgLy) = worldToLocal(origImgCenterX, origImgCenterY, startState.worldCenter, rad)

        val plxRel = imgLx - anchorWorldX
        val plyRel = imgLy - anchorWorldY

        val nlx = anchorWorldX + plxRel * scaleX
        val nly = anchorWorldY + plyRel * scaleY

        val (newCenterX, newCenterY) = localToWorld(nlx, nly, startState.worldCenter, rad)
        val newWidth = originalImage.width * scaleX
        val newHeight = originalImage.height * scaleY

        val newX = newCenterX - newWidth / 2f
        val newY = newCenterY - newHeight / 2f

        return clampImageToCanvas(
            originalImage.copy(
                x = newX,
                y = newY,
                width = newWidth,
                height = newHeight,
                bounds = CanvasRect(newX, newY, newX + newWidth, newY + newHeight)
            )
        )
    }

    private fun clampImageToCanvas(img: ImageElement): ImageElement {
        val extent = 20000.0f
        val clampedW = img.width.coerceIn(10f, extent)
        val clampedH = img.height.coerceIn(10f, extent)
        val clampedX = img.x.coerceIn(-extent, extent - clampedW)
        val clampedY = img.y.coerceIn(-extent, extent - clampedH)
        return img.copy(
            x = clampedX,
            y = clampedY,
            width = clampedW,
            height = clampedH,
            bounds = CanvasRect(clampedX, clampedY, clampedX + clampedW, clampedY + clampedH)
        )
    }
}

fun CanvasRect.translated(dx: Float, dy: Float): CanvasRect {
    return CanvasRect(left + dx, top + dy, right + dx, bottom + dy)
}
