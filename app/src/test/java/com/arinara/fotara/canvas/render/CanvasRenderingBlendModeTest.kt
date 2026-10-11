// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasRenderingBlendModeTest {

    @Test
    fun testComputeStrokeAlpha_HighlighterAndPenOpacityCalculations() {
        // Shared constant HIGHLIGHTER_BASE_ALPHA = 90
        assertEquals(90, CanvasRenderer.HIGHLIGHTER_BASE_ALPHA)

        // Pen: base alpha 255 multiplied by layer opacity
        assertEquals(255, CanvasRenderer.computeStrokeAlpha(StrokeToolType.PEN, 1.0f))
        assertEquals(127, CanvasRenderer.computeStrokeAlpha(StrokeToolType.PEN, 0.5f))
        assertEquals(0, CanvasRenderer.computeStrokeAlpha(StrokeToolType.PEN, 0.0f))

        // Highlighter: base alpha 90 multiplied by layer opacity
        assertEquals(90, CanvasRenderer.computeStrokeAlpha(StrokeToolType.HIGHLIGHTER, 1.0f))
        assertEquals(45, CanvasRenderer.computeStrokeAlpha(StrokeToolType.HIGHLIGHTER, 0.5f))
        assertEquals(18, CanvasRenderer.computeStrokeAlpha(StrokeToolType.HIGHLIGHTER, 0.2f))
        assertEquals(0, CanvasRenderer.computeStrokeAlpha(StrokeToolType.HIGHLIGHTER, 0.0f))

        // Clamping to [0, 255]
        assertEquals(255, CanvasRenderer.computeStrokeAlpha(StrokeToolType.PEN, 1.5f))
        assertEquals(0, CanvasRenderer.computeStrokeAlpha(StrokeToolType.PEN, -0.5f))
    }

    @Test
    fun testShouldIsolateLayer_DecisionPredicate() {
        val normalStroke1 = StrokeElement(
            id = "s1",
            layerId = "l1",
            points = listOf(StrokePoint(0f, 0f)),
            color = 0xFF000000,
            width = 4.0f,
            toolType = StrokeToolType.PEN,
            blendMode = StrokeBlendMode.NORMAL,
            bounds = CanvasRect(0f, 0f, 10f, 10f)
        )
        val normalStroke2 = StrokeElement(
            id = "s2",
            layerId = "l1",
            points = listOf(StrokePoint(10f, 10f)),
            color = 0xFFF4D03F,
            width = 20.0f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.NORMAL,
            bounds = CanvasRect(10f, 10f, 20f, 20f)
        )

        // All NORMAL elements -> no isolation needed (zero extra offscreen cost)
        assertFalse(CanvasRenderer.shouldIsolateLayer(listOf(normalStroke1, normalStroke2)))
        assertFalse(CanvasRenderer.shouldIsolateLayer(emptyList()))

        // Non-NORMAL strokes -> isolation required
        for (mode in listOf(StrokeBlendMode.MULTIPLY, StrokeBlendMode.DARKEN, StrokeBlendMode.SCREEN)) {
            val blendedStroke = normalStroke2.copy(id = "blended_${mode.name}", blendMode = mode)
            assertTrue("Layer with $mode must be isolated", CanvasRenderer.shouldIsolateLayer(listOf(normalStroke1, blendedStroke)))
        }

        // Live stroke with non-NORMAL blend mode triggers layer isolation
        assertTrue(CanvasRenderer.shouldIsolateLayer(listOf(normalStroke1), hasBlendedInProgress = true))
        assertFalse(CanvasRenderer.shouldIsolateLayer(listOf(normalStroke1), hasBlendedInProgress = false))
    }
}
