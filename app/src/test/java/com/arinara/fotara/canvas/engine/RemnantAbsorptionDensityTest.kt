// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemnantAbsorptionDensityTest {

    @Test
    fun testRemnantAbsorptionThreshold_ScalesWithDisplayDensity() {
        // Construct a stroke with a short tail of 6 pixels in world coordinates:
        // Main segment: (0, 0) to (50, 0) [length 50]
        // Short remnant: (50, 0) to (56, 0) [length 6]
        // Cut with a lasso selecting (0, 0) to (50, 0)
        val stroke = StrokeElement(
            id = "remnant_test_stroke",
            layerId = "layer_1",
            points = listOf(
                StrokePoint(0f, 0f),
                StrokePoint(25f, 0f),
                StrokePoint(50f, 0f),
                StrokePoint(53f, 0f),
                StrokePoint(56f, 0f)
            ),
            color = 0xFFFFFFFF,
            width = 2f,
            toolType = StrokeToolType.PEN,
            bounds = CanvasRect(0f, 0f, 56f, 0f)
        )

        val lassoPolygon = listOf(
            Pair(-5f, -5f),
            Pair(51f, -5f),
            Pair(51f, 5f),
            Pair(-5f, 5f)
        )

        // 1. With density = 1.0f and zoom = 1.0f:
        // minRemainderScreen = 4.0 * 1.0 = 4.0px
        // The remaining tail of length 6px is > 4.0px, so it is KEPT as a remnant!
        val splitResultDensity1 = CanvasSelectionEngine.sliceStrokeWithLasso(
            stroke = stroke,
            lassoPolygon = lassoPolygon,
            viewportScale = 1.0f,
            density = 1.0f
        )
        assertNotNull(splitResultDensity1)
        assertTrue(splitResultDensity1 is SelectedElementReference.PartialStroke)
        val (remnantsDensity1, _) = CanvasSelectionEngine.materializePartialSplit(
            original = stroke,
            ref = splitResultDensity1 as SelectedElementReference.PartialStroke,
            viewportScale = 1.0f,
            density = 1.0f
        )
        assertEquals("Under density 1.0f (threshold 4px), 6px tail must be kept", 1, remnantsDensity1.size)

        // 2. With non-1.0 density (density = 2.5f, e.g. xxxhdpi device) and zoom = 1.0f:
        // minRemainderScreen = 4.0 * 2.5 = 10.0px
        // minRemainderWorld = 10.0 / 1.0 = 10.0px
        // The remaining tail of length 6px is < 10.0px, so it is ABSORBED into selection!
        val splitResultDensity25 = CanvasSelectionEngine.sliceStrokeWithLasso(
            stroke = stroke,
            lassoPolygon = lassoPolygon,
            viewportScale = 1.0f,
            density = 2.5f
        )
        assertNotNull(splitResultDensity25)
        // Since the 6px remainder is <= 10.0px, it was absorbed directly into the selection, making the whole stroke selected!
        assertTrue("Under density 2.5f (threshold 10px), 6px tail must be absorbed into selection", splitResultDensity25 is SelectedElementReference.Whole)

        // 3. With density = 2.0f and zoom = 2.0f:
        // minRemainderScreen = 4.0 * 2.0 = 8.0px
        // minRemainderWorld = 8.0 / 2.0 = 4.0px in world space
        // 6px world tail > 4.0px world threshold -> KEPT
        val splitResultZoomed = CanvasSelectionEngine.sliceStrokeWithLasso(
            stroke = stroke,
            lassoPolygon = lassoPolygon,
            viewportScale = 2.0f,
            density = 2.0f
        )
        assertNotNull(splitResultZoomed)
        assertTrue(splitResultZoomed is SelectedElementReference.PartialStroke)
        val (remnantsZoomed, _) = CanvasSelectionEngine.materializePartialSplit(
            original = stroke,
            ref = splitResultZoomed as SelectedElementReference.PartialStroke,
            viewportScale = 2.0f,
            density = 2.0f
        )
        assertEquals("Under density 2.0f with zoom 2.0x (threshold 4px world), 6px tail must be kept", 1, remnantsZoomed.size)
    }
}
