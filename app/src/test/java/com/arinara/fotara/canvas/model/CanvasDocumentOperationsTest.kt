// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.model

import com.arinara.fotara.canvas.engine.CanvasRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasDocumentOperationsTest {

    private fun createDummyStroke(id: String, layerId: String): StrokeElement {
        return StrokeElement(
            id = id,
            layerId = layerId,
            points = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)),
            color = 0xFF000000,
            width = 2.0f,
            bounds = CanvasRect(0f, 0f, 10f, 10f)
        )
    }

    @Test
    fun testAddLayer() {
        val doc = CanvasDocument()
        assertEquals(1, doc.layers.size)

        val updated = CanvasDocumentOperations.addLayer(doc, "Sketches")
        assertEquals(2, updated.layers.size)
        assertEquals("Sketches", updated.layers[1].name)
        assertEquals(1, updated.layers[1].order)
    }

    @Test
    fun testRemoveLayer_DeletesContainedElementsAndReturnsForUndo() {
        val layer1 = CanvasLayer(id = "l1", name = "Layer 1", order = 0)
        val layer2 = CanvasLayer(id = "l2", name = "Layer 2", order = 1)
        val s1 = createDummyStroke("s1", "l1")
        val s2 = createDummyStroke("s2", "l2")
        val s3 = createDummyStroke("s3", "l2")

        val doc = CanvasDocument(
            layers = listOf(layer1, layer2),
            elements = listOf(s1, s2, s3)
        )

        // Remove layer 2
        val (updatedDoc, deletedElements) = CanvasDocumentOperations.removeLayer(doc, "l2")

        assertEquals(1, updatedDoc.layers.size)
        assertEquals("l1", updatedDoc.layers.first().id)

        // Elements belonging to l2 must be removed from document
        assertEquals(1, updatedDoc.elements.size)
        assertEquals("s1", updatedDoc.elements.first().id)

        // Deleted elements must contain s2 and s3 for undo restoration
        assertEquals(2, deletedElements.size)
        assertTrue(deletedElements.any { it.id == "s2" })
        assertTrue(deletedElements.any { it.id == "s3" })
    }

    @Test
    fun testRemoveOnlyRemainingLayer_IsRejected() {
        val doc = CanvasDocument(layers = listOf(CanvasLayer(id = "l1", name = "Only Layer")))
        val (updatedDoc, deleted) = CanvasDocumentOperations.removeLayer(doc, "l1")

        // Must refuse to delete the only layer
        assertEquals(1, updatedDoc.layers.size)
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun testReorderLayers() {
        val l1 = CanvasLayer(id = "l1", name = "L1", order = 0)
        val l2 = CanvasLayer(id = "l2", name = "L2", order = 1)
        val l3 = CanvasLayer(id = "l3", name = "L3", order = 2)

        val doc = CanvasDocument(layers = listOf(l1, l2, l3))
        val reordered = CanvasDocumentOperations.reorderLayers(doc, listOf("l3", "l1", "l2"))

        assertEquals("l3", reordered.layers[0].id)
        assertEquals("l1", reordered.layers[1].id)
        assertEquals("l2", reordered.layers[2].id)
        assertEquals(0, reordered.layers[0].order)
        assertEquals(1, reordered.layers[1].order)
        assertEquals(2, reordered.layers[2].order)
    }

    @Test
    fun testRenameAndToggleProps() {
        val l1 = CanvasLayer(id = "l1", name = "Draft", isVisible = true, isLocked = false, opacity = 1.0f)
        var doc = CanvasDocument(layers = listOf(l1))

        doc = CanvasDocumentOperations.renameLayer(doc, "l1", "Final Lineart")
        assertEquals("Final Lineart", doc.layers.first().name)

        doc = CanvasDocumentOperations.setLayerVisibility(doc, "l1", false)
        assertFalse(doc.layers.first().isVisible)

        doc = CanvasDocumentOperations.setLayerLocked(doc, "l1", true)
        assertTrue(doc.layers.first().isLocked)

        doc = CanvasDocumentOperations.setLayerOpacity(doc, "l1", 0.45f)
        assertEquals(0.45f, doc.layers.first().opacity, 0.001f)
    }

    @Test
    fun testMoveElementsToLayer() {
        val l1 = CanvasLayer(id = "l1", name = "L1")
        val l2 = CanvasLayer(id = "l2", name = "L2")
        val s1 = createDummyStroke("s1", "l1")
        val s2 = createDummyStroke("s2", "l1")

        val doc = CanvasDocument(layers = listOf(l1, l2), elements = listOf(s1, s2))
        val movedDoc = CanvasDocumentOperations.moveElementsToLayer(doc, setOf("s1"), "l2")

        assertEquals("l2", movedDoc.elements.find { it.id == "s1" }?.layerId)
        assertEquals("l1", movedDoc.elements.find { it.id == "s2" }?.layerId)
    }
}
