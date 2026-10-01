// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasHistoryTest {

    private fun createSampleStroke(id: String): StrokeElement {
        return StrokeElement(
            id = id,
            layerId = "layer_default",
            points = listOf(StrokePoint(0f, 0f), StrokePoint(50f, 50f)),
            color = 0xFFFFFFFF,
            width = 4.0f,
            bounds = CanvasRect(0f, 0f, 50f, 50f)
        )
    }

    @Test
    fun testAddAndRemoveElements_UndoRedoRoundTrip() {
        val history = CanvasHistoryManager(maxHistorySize = 10)
        var doc = CanvasDocument()
        val stroke = createSampleStroke("s1")

        // Execute Add
        doc = history.execute(AddElementsCommand(listOf(stroke)), doc)
        assertEquals(1, doc.elements.size)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        // Undo Add
        doc = history.undo(doc)!!
        assertEquals(0, doc.elements.size)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        // Redo Add
        doc = history.redo(doc)!!
        assertEquals(1, doc.elements.size)
        assertEquals("s1", doc.elements.first().id)
    }

    @Test
    fun testTransformElements_UndoRedo() {
        val history = CanvasHistoryManager()
        val s1 = createSampleStroke("s1")
        val s1Moved = s1.translated(100f, 100f)

        var doc = CanvasDocument(elements = listOf(s1))

        // Execute transform
        doc = history.execute(TransformElementsCommand(before = listOf(s1), after = listOf(s1Moved)), doc)
        assertEquals(100f, (doc.elements.first() as StrokeElement).points.first().x, 0.01f)

        // Undo transform
        doc = history.undo(doc)!!
        assertEquals(0f, (doc.elements.first() as StrokeElement).points.first().x, 0.01f)

        // Redo transform
        doc = history.redo(doc)!!
        assertEquals(100f, (doc.elements.first() as StrokeElement).points.first().x, 0.01f)
    }

    @Test
    fun testChangeLayerProps_UndoRedo() {
        val history = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Original", opacity = 1.0f)
        val modifiedLayer = layer.copy(name = "Renamed", opacity = 0.5f)

        var doc = CanvasDocument(layers = listOf(layer))

        doc = history.execute(ChangeLayerPropsCommand("layer_1", layer, modifiedLayer), doc)
        assertEquals("Renamed", doc.layers.first().name)
        assertEquals(0.5f, doc.layers.first().opacity, 0.01f)

        // Undo
        doc = history.undo(doc)!!
        assertEquals("Original", doc.layers.first().name)
        assertEquals(1.0f, doc.layers.first().opacity, 0.01f)

        // Redo
        doc = history.redo(doc)!!
        assertEquals("Renamed", doc.layers.first().name)
    }

    @Test
    fun testNewCommand_ClearsRedoStack() {
        val history = CanvasHistoryManager()
        var doc = CanvasDocument()

        doc = history.execute(AddElementsCommand(listOf(createSampleStroke("s1"))), doc)
        doc = history.execute(AddElementsCommand(listOf(createSampleStroke("s2"))), doc)

        // Undo once -> redo is available
        doc = history.undo(doc)!!
        assertEquals(1, doc.elements.size)
        assertTrue(history.canRedo)

        // Execute new third command -> redo stack must be cleared
        doc = history.execute(AddElementsCommand(listOf(createSampleStroke("s3"))), doc)
        assertEquals(2, doc.elements.size)
        assertFalse(history.canRedo)
    }

    @Test
    fun testHistoryCapping_DropsOldestCommands() {
        val history = CanvasHistoryManager(maxHistorySize = 5)
        var doc = CanvasDocument()

        // Push 10 commands into a history capped at 5
        for (i in 1..10) {
            doc = history.execute(AddElementsCommand(listOf(createSampleStroke("s$i"))), doc)
        }

        assertEquals(10, doc.elements.size)
        assertEquals(5, history.undoSize)

        // Undo 5 times
        for (i in 1..5) {
            doc = history.undo(doc)!!
        }

        assertEquals(5, doc.elements.size)
        assertFalse(history.canUndo) // Cannot undo older than 5 commands
    }
}
