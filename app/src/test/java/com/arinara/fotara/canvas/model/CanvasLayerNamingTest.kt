// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasLayerNamingTest {

    @Test
    fun testEmptyLayerList_ReturnsLayer1() {
        val result = CanvasLayerNaming.generateNextDefaultLayerName(emptyList())
        assertEquals("Layer 1", result)
    }

    @Test
    fun testSequentialLayers_ReturnsNextIncrement() {
        val layers = listOf("Layer 1", "Layer 2", "Layer 3")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(layers)
        assertEquals("Layer 4", result)
    }

    @Test
    fun testGapsInNumbering_FillsLowestGap() {
        // Gap at 2
        val layers = listOf("Layer 1", "Layer 3")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(layers)
        assertEquals("Layer 2", result)
    }

    @Test
    fun testMissingLayer1_ReturnsLayer1() {
        // Gaps starting at 1
        val layers = listOf("Layer 2", "Layer 3", "Layer 5")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(layers)
        assertEquals("Layer 1", result)
    }

    @Test
    fun testAfterDelete_ReusesLowestAvailableNumber() {
        // User had Layer 1 and Layer 2, deleted Layer 1
        val remainingLayers = listOf("Layer 2")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(remainingLayers)
        assertEquals("Layer 1", result)
    }

    @Test
    fun testDuplicateLayerNames_HandledCorrectly() {
        val layers = listOf("Layer 1", "Layer 1", "Layer 2")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(layers)
        assertEquals("Layer 3", result)
    }

    @Test
    fun testArbitraryCustomLayerNames_IgnoredWhenFindingNumbers() {
        val layers = listOf("Background", "Sketch", "Lineart", "Layer 2")
        val result = CanvasLayerNaming.generateNextDefaultLayerName(layers)
        assertEquals("Layer 1", result)
    }
}
