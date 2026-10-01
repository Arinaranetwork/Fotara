// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasDocumentOperations
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.StrokeElement

/**
 * Command interface for the Canvas undo/redo system.
 * Every user action or continuous gesture maps to a single invertible command.
 */
interface CanvasCommand {
    val description: String
    fun apply(document: CanvasDocument): CanvasDocument
    fun undo(document: CanvasDocument): CanvasDocument
}

/**
 * Adds one or more elements to the document.
 */
class AddElementsCommand(
    private val elementsToAdd: List<CanvasElement>,
    override val description: String = "Add Elements"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return document.copy(
            elements = document.elements + elementsToAdd,
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        val idsToRemove = elementsToAdd.map { it.id }.toSet()
        return document.copy(
            elements = document.elements.filter { it.id !in idsToRemove },
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Deletes one or more elements from the document.
 */
class RemoveElementsCommand(
    private val elementsToRemove: List<CanvasElement>,
    override val description: String = "Delete Elements"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        val idsToRemove = elementsToRemove.map { it.id }.toSet()
        return document.copy(
            elements = document.elements.filter { it.id !in idsToRemove },
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return document.copy(
            elements = document.elements + elementsToRemove,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Transforms elements (translation, scaling, rotation).
 */
class TransformElementsCommand(
    private val before: List<CanvasElement>,
    private val after: List<CanvasElement>,
    override val description: String = "Transform Elements"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        val afterMap = after.associateBy { it.id }
        val updated = document.elements.map { el -> afterMap[el.id] ?: el }
        return document.copy(elements = updated, updatedAt = System.currentTimeMillis())
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        val beforeMap = before.associateBy { it.id }
        val updated = document.elements.map { el -> beforeMap[el.id] ?: el }
        return document.copy(elements = updated, updatedAt = System.currentTimeMillis())
    }
}

/**
 * Updates properties of a layer (visibility, lock, opacity, rename).
 */
class ChangeLayerPropsCommand(
    private val layerId: String,
    private val oldLayer: CanvasLayer,
    private val newLayer: CanvasLayer,
    override val description: String = "Change Layer Properties"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        val updated = document.layers.map { if (it.id == layerId) newLayer else it }
        return document.copy(layers = updated, updatedAt = System.currentTimeMillis())
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        val updated = document.layers.map { if (it.id == layerId) oldLayer else it }
        return document.copy(layers = updated, updatedAt = System.currentTimeMillis())
    }
}

/**
 * Reorders document layers.
 */
class ReorderLayersCommand(
    private val oldOrderIds: List<String>,
    private val newOrderIds: List<String>,
    override val description: String = "Reorder Layers"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return CanvasDocumentOperations.reorderLayers(document, newOrderIds)
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return CanvasDocumentOperations.reorderLayers(document, oldOrderIds)
    }
}

/**
 * Creates a new layer.
 */
class AddLayerCommand(
    private val layer: CanvasLayer,
    override val description: String = "Add Layer"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return document.copy(
            layers = document.layers + layer,
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return document.copy(
            layers = document.layers.filter { it.id != layer.id },
            elements = document.elements.filter { it.layerId != layer.id },
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Deletes a layer and its member elements.
 */
class RemoveLayerCommand(
    private val layer: CanvasLayer,
    private val memberElements: List<CanvasElement>,
    override val description: String = "Delete Layer"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return document.copy(
            layers = document.layers.filter { it.id != layer.id },
            elements = document.elements.filter { it.layerId != layer.id },
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return document.copy(
            layers = (document.layers + layer).sortedBy { it.order },
            elements = document.elements + memberElements,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Moves a set of elements from one layer to another.
 */
class MoveElementsToLayerCommand(
    private val elementIds: Set<String>,
    private val fromLayerId: String,
    private val toLayerId: String,
    override val description: String = "Move Elements to Layer"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return CanvasDocumentOperations.moveElementsToLayer(document, elementIds, toLayerId)
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return CanvasDocumentOperations.moveElementsToLayer(document, elementIds, fromLayerId)
    }
}

/**
 * Replaces original strokes with split strokes resulting from an area erase gesture.
 */
class AreaEraseCommand(
    private val originalStrokes: List<StrokeElement>,
    private val replacementStrokes: List<StrokeElement>,
    override val description: String = "Area Erase"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        val originalIds = originalStrokes.map { it.id }.toSet()
        val remaining = document.elements.filter { it.id !in originalIds }
        return document.copy(
            elements = remaining + replacementStrokes,
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        val replacementIds = replacementStrokes.map { it.id }.toSet()
        val remaining = document.elements.filter { it.id !in replacementIds }
        return document.copy(
            elements = remaining + originalStrokes,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Changes document background style.
 */
class SetBackgroundStyleCommand(
    private val oldStyle: CanvasBackgroundStyle,
    private val newStyle: CanvasBackgroundStyle,
    override val description: String = "Change Background Style"
) : CanvasCommand {

    override fun apply(document: CanvasDocument): CanvasDocument {
        return document.copy(
            backgroundStyle = newStyle,
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun undo(document: CanvasDocument): CanvasDocument {
        return document.copy(
            backgroundStyle = oldStyle,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Manages the undo/redo history stacks with capacity capping and transactional safety.
 */
class CanvasHistoryManager(
    private val maxHistorySize: Int = 100
) {

    private val undoStack = ArrayDeque<CanvasCommand>()
    private val redoStack = ArrayDeque<CanvasCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoSize: Int get() = undoStack.size
    val redoSize: Int get() = redoStack.size

    /**
     * Executes a command, applying it to [currentDoc].
     * Pushes to undo stack and clears redo stack.
     * Guaranteed transactional safety: if applying fails, returns [currentDoc] untouched.
     */
    fun execute(command: CanvasCommand, currentDoc: CanvasDocument): CanvasDocument {
        return try {
            val updated = command.apply(currentDoc)
            undoStack.addLast(command)
            if (undoStack.size > maxHistorySize) {
                undoStack.removeFirst()
            }
            redoStack.clear()
            updated
        } catch (_: Exception) {
            currentDoc
        }
    }

    /**
     * Undoes the last command.
     */
    fun undo(currentDoc: CanvasDocument): CanvasDocument? {
        if (undoStack.isEmpty()) return null
        val command = undoStack.removeLast()
        return try {
            val updated = command.undo(currentDoc)
            redoStack.addLast(command)
            updated
        } catch (_: Exception) {
            undoStack.addLast(command)
            null
        }
    }

    /**
     * Redoes the last undone command.
     */
    fun redo(currentDoc: CanvasDocument): CanvasDocument? {
        if (redoStack.isEmpty()) return null
        val command = redoStack.removeLast()
        return try {
            val updated = command.apply(currentDoc)
            undoStack.addLast(command)
            updated
        } catch (_: Exception) {
            redoStack.addLast(command)
            null
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
