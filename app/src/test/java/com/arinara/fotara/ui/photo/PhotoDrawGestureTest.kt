// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.photo

import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhotoDrawGestureTest {

    private fun screenToPhoto(
        touchX: Float,
        touchY: Float,
        containerW: Float,
        containerH: Float,
        fitLeft: Float,
        fitTop: Float,
        fitScale: Float,
        userScale: Float,
        panOffsetX: Float,
        panOffsetY: Float
    ): Pair<Float, Float> {
        val viewCenterX = containerW / 2f
        val viewCenterY = containerH / 2f
        val contentX = (touchX - viewCenterX - panOffsetX) / userScale + viewCenterX
        val contentY = (touchY - viewCenterY - panOffsetY) / userScale + viewCenterY
        val photoX = (contentX - fitLeft) / fitScale
        val photoY = (contentY - fitTop) / fitScale
        return photoX to photoY
    }

    private fun photoToScreen(
        photoX: Float,
        photoY: Float,
        containerW: Float,
        containerH: Float,
        fitLeft: Float,
        fitTop: Float,
        fitScale: Float,
        userScale: Float,
        panOffsetX: Float,
        panOffsetY: Float
    ): Pair<Float, Float> {
        val viewCenterX = containerW / 2f
        val viewCenterY = containerH / 2f
        val contentX = photoX * fitScale + fitLeft
        val contentY = photoY * fitScale + fitTop
        val touchX = (contentX - viewCenterX) * userScale + viewCenterX + panOffsetX
        val touchY = (contentY - viewCenterY) * userScale + viewCenterY + panOffsetY
        return touchX to touchY
    }

    @Test
    fun coordinateMappingRoundTrip_isExact() {
        val containerW = 1080f
        val containerH = 2160f
        val photoW = 3000f
        val photoH = 4000f

        val fitScale = minOf(containerW / photoW, containerH / photoH) // 1080/3000 = 0.36
        val fitLeft = (containerW - photoW * fitScale) / 2f
        val fitTop = (containerH - photoH * fitScale) / 2f

        val userScale = 2.5f
        val panOffsetX = 120f
        val panOffsetY = -80f

        val originalPhotoX = 1500f
        val originalPhotoY = 2000f

        val (touchX, touchY) = photoToScreen(
            originalPhotoX, originalPhotoY,
            containerW, containerH, fitLeft, fitTop, fitScale, userScale, panOffsetX, panOffsetY
        )

        val (recoveredPhotoX, recoveredPhotoY) = screenToPhoto(
            touchX, touchY,
            containerW, containerH, fitLeft, fitTop, fitScale, userScale, panOffsetX, panOffsetY
        )

        assertTrue(abs(originalPhotoX - recoveredPhotoX) < 0.01f)
        assertTrue(abs(originalPhotoY - recoveredPhotoY) < 0.01f)
    }

    @Test
    fun strokeWidthInPhotoPx_scalesInverselyWithFitScale() {
        val sliderDp = 4f
        val density = 3f

        // Case A: 1000px wide photo fits in 1000px container -> fitScale = 1.0
        val fitScaleA = 1.0f
        val widthA = (sliderDp * density) / fitScaleA
        assertEquals(12.0f, widthA, 0.001f)

        // Case B: 4000px wide photo fits in 1000px container -> fitScale = 0.25
        val fitScaleB = 0.25f
        val widthB = (sliderDp * density) / fitScaleB
        assertEquals(48.0f, widthB, 0.001f)

        // Display width on screen is width * fitScale = 48 * 0.25 = 12px in both cases!
        assertEquals(widthA * fitScaleA, widthB * fitScaleB, 0.001f)
    }

    @Test
    fun undoRedoStack_capsAt100AndMaintainsInvariants() {
        val undoStack = ArrayDeque<List<StrokeElement>>()
        val redoStack = ArrayDeque<List<StrokeElement>>()

        fun createStroke(id: Int): StrokeElement {
            val pts = listOf(StrokePoint(id.toFloat(), id.toFloat(), 1f))
            return StrokeElement(
                layerId = "photo_layer",
                points = pts,
                color = 0xFFFFFFFF,
                width = 4f,
                bounds = CanvasRect(id.toFloat(), id.toFloat(), id + 4f, id + 4f)
            )
        }

        // Push 110 states
        for (i in 1..110) {
            undoStack.addLast(listOf(createStroke(i)))
            if (undoStack.size > 100) {
                undoStack.removeFirst()
            }
        }

        assertEquals(100, undoStack.size)
        // First entry in undo stack should be state 11 (1..10 were dropped)
        assertEquals(11f, undoStack.first()[0].points[0].x, 0.01f)
        // Last entry should be state 110
        assertEquals(110f, undoStack.last()[0].points[0].x, 0.01f)

        // Undo 3 steps
        for (step in 1..3) {
            val popped = undoStack.removeLast()
            redoStack.addLast(popped)
        }

        assertEquals(97, undoStack.size)
        assertEquals(3, redoStack.size)

        // Redo 1 step
        val redone = redoStack.removeLast()
        undoStack.addLast(redone)

        assertEquals(98, undoStack.size)
        assertEquals(2, redoStack.size)

        // A new user action clears redoStack
        redoStack.clear()
        assertTrue(redoStack.isEmpty())
    }

    @Test
    fun eraserCapsule_erasesCrossingStrokes() {
        val crossingStroke = StrokeElement(
            layerId = "photo_layer",
            points = listOf(
                StrokePoint(50f, 100f, 1f),
                StrokePoint(150f, 100f, 1f)
            ),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(50f, 98f, 150f, 102f)
        )

        val nonCrossingStroke = StrokeElement(
            layerId = "photo_layer",
            points = listOf(
                StrokePoint(50f, 300f, 1f),
                StrokePoint(150f, 300f, 1f)
            ),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(50f, 298f, 150f, 302f)
        )

        // Eraser sweeps vertically through x=100 from y=50 to y=150
        val splitCrossing = StrokeProcessor.eraseStrokeWithCapsule(
            stroke = crossingStroke,
            ax = 100f,
            ay = 50f,
            bx = 100f,
            by = 150f,
            eraserRadius = 10f,
            minRemainder = 3f
        )

        // The crossing stroke is affected (either split into 2 smaller segments or shortened)
        assertTrue(splitCrossing.size != 1 || splitCrossing[0].points.size < crossingStroke.points.size)

        // Eraser on non-crossing stroke does nothing
        val splitNonCrossing = StrokeProcessor.eraseStrokeWithCapsule(
            stroke = nonCrossingStroke,
            ax = 100f,
            ay = 50f,
            bx = 100f,
            by = 150f,
            eraserRadius = 10f,
            minRemainder = 3f
        )

        assertEquals(1, splitNonCrossing.size)
        assertEquals(nonCrossingStroke.points.size, splitNonCrossing[0].points.size)
    }
}
